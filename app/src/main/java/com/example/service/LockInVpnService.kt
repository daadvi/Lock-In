package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * LockInVpnService
 *
 * Lightweight, 100% on-device virtual network shield (DNS Sinkhole & Firewall).
 *
 * 1. Bypasses 100% of banking and financial apps via [VpnService.Builder.addDisallowedApplication]
 *    so banking security SDKs never detect VPN interference.
 * 2. Intercepts DNS queries locally to sinkhole distraction domains and blacklisted sites.
 * 3. Provides network barrier for exhausted apps without sending any user data off the device.
 */
class LockInVpnService : VpnService() {

    companion object {
        private const val TAG = "LockInVpnService"
        const val ACTION_START = "com.example.service.LOCK_IN_VPN_START"
        const val ACTION_STOP = "com.example.service.LOCK_IN_VPN_STOP"
        private const val NOTIFICATION_ID = 9042
        private const val CHANNEL_ID = "lock_in_vpn_channel"
        private const val VPN_DNS_SERVER = "10.10.0.2"
        private const val UPSTREAM_DNS_PRIMARY = "1.1.1.1"
        private const val UPSTREAM_DNS_SECONDARY = "8.8.8.8"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private var startJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lock In - Escudo de Rede Local",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Firewall e Bloqueador de Domínios local ativo (100% no aparelho)"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Lock In • Escudo de Rede Ativo")
            .setContentText("Firewall local protegendo seu foco. Bancos isentos.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startVpn() {
        if (vpnInterface != null || startJob?.isActive == true) return
        // Android's foreground-service deadline must be met before slow PackageManager calls.
        startForeground(NOTIFICATION_ID, buildNotification())
        startJob = scope.launch(Dispatchers.Main.immediate) {
            var candidate: ParcelFileDescriptor? = null
            try {
                withContext(Dispatchers.IO) {
                    val builder = Builder()
                        .setSession("Lock In Escudo")
                        .setMtu(1500)
                        .addAddress("10.10.0.1", 24)
                        .addDnsServer(VPN_DNS_SERVER)
                        .addRoute(VPN_DNS_SERVER, 32)
                    for (info in packageManager.getInstalledPackages(0)) {
                        if (BankingSecurityHelper.isBankingOrFinancialApp(info.packageName)) {
                            try { builder.addDisallowedApplication(info.packageName) }
                            catch (_: android.content.pm.PackageManager.NameNotFoundException) {}
                        }
                    }
                    builder.addDisallowedApplication(packageName)
                    candidate = builder.establish()
                }
                val established = candidate ?: error("VPN permission is unavailable")
                vpnInterface = established
                LockInVpnManager.setVpnRunning(true)
                startDnsPacketLoop(established)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unable to establish VPN", e)
                stopVpn()
                stopSelf()
            } finally {
                // Includes cancellation while PackageManager/establish was still executing.
                if (candidate !== vpnInterface) candidate?.close()
            }
        }
    }

    private fun startDnsPacketLoop(pfd: ParcelFileDescriptor) {
        vpnJob?.cancel()
        vpnJob = scope.launch {
            try {
                coroutineScope {
                    val input = FileInputStream(pfd.fileDescriptor)
                    val output = FileOutputStream(pfd.fileDescriptor)
                    // Backpressure bounds both queued packets and upstream sockets.
                    val requests = Channel<ByteArray>(64)
                    repeat(4) {
                        launch {
                            for (packet in requests) {
                                try {
                                    val ihl = (packet[0].toInt() and 0x0F) * 4
                                    val buffer = ByteBuffer.wrap(packet)
                                    handleDnsQuery(packet, ihl, ihl + 8,
                                        (buffer.getShort(ihl + 4).toInt() and 0xFFFF) - 8,
                                        buffer.getShort(ihl).toInt() and 0xFFFF, 53, output)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.w(TAG, "DNS request failed", e)
                                }
                            }
                        }
                    }
                    val packet = ByteArray(32767)
                    val pollDescriptors = arrayOf(android.system.StructPollfd().apply {
                        fd = pfd.fileDescriptor
                        events = android.system.OsConstants.POLLIN.toShort()
                    })
                    try {
                        while (isActive) {
                            val length = try { input.read(packet) } catch (e: java.io.IOException) {
                                // A nonblocking TUN can report EAGAIN when idle.
                                val errno = e.cause as? android.system.ErrnoException
                                if (errno?.errno == android.system.OsConstants.EAGAIN) 0 else throw e
                            }
                            if (length < 0) break
                            if (length == 0) {
                                // Sleep in the kernel until traffic arrives; no 100 Hz idle coroutine polling.
                                android.system.Os.poll(pollDescriptors, 1000)
                                continue
                            }
                            if (!DnsPacket.isDnsQuery(packet, length)) continue
                            requests.send(packet.copyOf(length))
                        }
                    } finally {
                        requests.close()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "VPN packet loop stopped", e)
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    if (vpnInterface === pfd) {
                        stopVpn()
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun handleDnsQuery(
        packet: ByteArray,
        ihl: Int,
        dnsOffset: Int,
        dnsLen: Int,
        srcPort: Int,
        dstPort: Int,
        output: FileOutputStream
    ) {
        val queryDomain = parseDnsQuestionDomain(packet, dnsOffset, dnsLen)
        val isBlocked = if (queryDomain != null) {
            val blockedByWebsite = WebsiteBlacklistManager.isWebsiteBlacklisted(queryDomain)
            if (blockedByWebsite != null) {
                LockInVpnManager.recordBlockedQuery(this, queryDomain)
                true
            } else {
                // Check if belongs to depleted app domains
                isDomainBelongingToDepletedApp(queryDomain)
            }
        } else false

        if (isBlocked) {
            // Sinkhole: reply with 0.0.0.0
            val responsePacket = buildDnsBlockResponse(packet, ihl, dnsOffset, dnsLen, srcPort, dstPort)
            if (responsePacket != null) {
                synchronized(output) {
                    output.write(responsePacket)
                    output.flush()
                }
            }
        } else if (queryDomain != null) {
            // Forward to upstream DNS (1.1.1.1) using dedicated ephemeral socket to guarantee thread safety
            try {
                DatagramSocket().use { querySocket ->
                    check(protect(querySocket)) { "Cannot protect upstream socket" }
                    querySocket.connect(InetAddress.getByName(UPSTREAM_DNS_PRIMARY), 53)
                    querySocket.soTimeout = 1500
                    val dnsQueryBytes = packet.copyOfRange(dnsOffset, dnsOffset + dnsLen)
                    val outPacket = DatagramPacket(
                        dnsQueryBytes,
                        dnsQueryBytes.size,
                        InetAddress.getByName(UPSTREAM_DNS_PRIMARY),
                        53
                    )
                    querySocket.send(outPacket)

                    val inBuffer = ByteArray(2048)
                    val inPacket = DatagramPacket(inBuffer, inBuffer.size)
                    querySocket.receive(inPacket)

                    val responsePayload = inPacket.data.copyOf(inPacket.length)
                    val finalIpPacket = wrapDnsResponseInIp(packet, ihl, srcPort, dstPort, responsePayload)
                    synchronized(output) {
                        output.write(finalIpPacket)
                        output.flush()
                    }
                }
            } catch (_: Exception) {
                // Fallback: try secondary upstream if primary timed out
                try {
                    DatagramSocket().use { fallbackSocket ->
                        check(protect(fallbackSocket)) { "Cannot protect upstream socket" }
                        fallbackSocket.connect(InetAddress.getByName(UPSTREAM_DNS_SECONDARY), 53)
                        fallbackSocket.soTimeout = 1500
                        val dnsQueryBytes = packet.copyOfRange(dnsOffset, dnsOffset + dnsLen)
                        val outPacket = DatagramPacket(
                            dnsQueryBytes,
                            dnsQueryBytes.size,
                            InetAddress.getByName(UPSTREAM_DNS_SECONDARY),
                            53
                        )
                        fallbackSocket.send(outPacket)

                        val inBuffer = ByteArray(2048)
                        val inPacket = DatagramPacket(inBuffer, inBuffer.size)
                        fallbackSocket.receive(inPacket)

                        val responsePayload = inPacket.data.copyOf(inPacket.length)
                        val finalIpPacket = wrapDnsResponseInIp(packet, ihl, srcPort, dstPort, responsePayload)
                        synchronized(output) {
                            output.write(finalIpPacket)
                            output.flush()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun isDomainBelongingToDepletedApp(domain: String): Boolean {
        val depleted = LockInVpnManager.depletedPackages.value
        if (depleted.isEmpty()) return false

        val lower = domain.lowercase()
        for (pkg in depleted) {
            when {
                pkg.contains("instagram") && (lower.contains("instagram") || lower.contains("cdninstagram")) -> return true
                pkg.contains("tiktok") && (lower.contains("tiktok") || lower.contains("byteoversea") || lower.contains("ibytedtos")) -> return true
                pkg.contains("youtube") && (lower.contains("youtube") || lower.contains("googlevideo") || lower.contains("ytimg")) -> return true
                pkg.contains("twitter") && (lower.contains("twitter") || lower.contains("twimg") || lower.contains("x.com")) -> return true
                pkg.contains("pinterest") && (lower.contains("pinterest") || lower.contains("pinimg")) -> return true
                pkg.contains("facebook") && (lower.contains("facebook") || lower.contains("fbcdn")) -> return true
                pkg.contains("reddit") && (lower.contains("reddit") || lower.contains("redditmedia")) -> return true
            }
        }
        return false
    }

    private fun parseDnsQuestionDomain(packet: ByteArray, dnsOffset: Int, dnsLen: Int): String? =
        DnsPacket.question(packet, dnsOffset, dnsLen)?.domain

    private fun buildDnsBlockResponse(
        packet: ByteArray,
        ihl: Int,
        dnsOffset: Int,
        dnsLen: Int,
        srcPort: Int,
        dstPort: Int
    ): ByteArray? {
        try {
            if (dnsLen < 12) return null
            val txId0 = packet[dnsOffset]
            val txId1 = packet[dnsOffset + 1]

            val question = DnsPacket.question(packet, dnsOffset, dnsLen) ?: return null
            val questionBytes = packet.copyOfRange(dnsOffset + 12, question.end)
            // NXDOMAIN is valid for A, AAAA and other query types; never return an A answer to AAAA.
            val dnsResp = ByteBuffer.allocate(12 + questionBytes.size)
            dnsResp.put(txId0)
            dnsResp.put(txId1)
            dnsResp.putShort((0x8083 or ((packet[dnsOffset + 2].toInt() and 1) shl 8)).toShort())
            dnsResp.putShort(1)
            dnsResp.putShort(0)
            dnsResp.putShort(0)
            dnsResp.putShort(0)
            dnsResp.put(questionBytes)

            val dnsPayload = dnsResp.array()
            return wrapDnsResponseInIp(packet, ihl, srcPort, dstPort, dnsPayload)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating DNS block response", e)
            return null
        }
    }

    private fun wrapDnsResponseInIp(
        originalPacket: ByteArray,
        ihl: Int,
        origSrcPort: Int,
        origDstPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val totalIpLen = ihl + 8 + dnsPayload.size
        val respPacket = ByteArray(totalIpLen)
        val buf = ByteBuffer.wrap(respPacket)

        // 1. IP Header: Copy version, ihl, tos
        respPacket[0] = originalPacket[0]
        respPacket[1] = originalPacket[1]
        buf.putShort(2, totalIpLen.toShort())
        buf.putShort(4, 0) // Identification
        buf.putShort(6, 0x4000.toShort()) // Flags (Don't Fragment)
        respPacket[8] = 64 // TTL
        respPacket[9] = 17 // Protocol UDP

        // Swap Source IP and Destination IP
        System.arraycopy(originalPacket, 16, respPacket, 12, 4) // Src = Orig Dst
        System.arraycopy(originalPacket, 12, respPacket, 16, 4) // Dst = Orig Src

        // Compute IP Header Checksum
        buf.putShort(10, 0)
        val ipChecksum = computeIpChecksum(respPacket, 0, ihl)
        buf.putShort(10, ipChecksum.toShort())

        // 2. UDP Header
        buf.position(ihl)
        buf.putShort(origDstPort.toShort()) // Source Port (53)
        buf.putShort(origSrcPort.toShort()) // Destination Port (client)
        buf.putShort((8 + dnsPayload.size).toShort()) // Length
        buf.putShort(0) // UDP Checksum optional in IPv4

        // 3. DNS Payload
        buf.put(dnsPayload)

        return respPacket
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            val high = data[i].toInt() and 0xFF
            val low = data[i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun stopVpn() {
        startJob?.cancel()
        startJob = null
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        LockInVpnManager.setVpnRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "Lock In VPN interface stopped.")
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}
