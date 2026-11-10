package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.LockInApplication
import com.example.R
import com.example.data.model.FriendAppUsage
import com.example.data.model.InstallApprovalRequest
import com.example.data.model.LeaveApprovalRequest
import com.example.data.model.PartyAlert
import com.example.data.model.PartyMember
import com.example.data.model.PartyState
import com.example.data.model.RequestStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

object FriendsPartyManager {

    private const val TAG = "FriendsPartyManager"
    private const val PREFS_NAME = "lock_in_friends_party_prefs"
    private const val KEY_MY_USER_ID = "my_user_id"
    private const val KEY_PARTY_STATE_JSON = "party_state_json"
    private const val KEY_SHARE_DETAILED_SCREEN_TIME = "share_detailed_screen_time"
    private const val NOTIFICATION_CHANNEL_ID = "lock_in_friends_party_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "Lock In • Friends Panel"

    fun isSharingDetailedScreenTime(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHARE_DETAILED_SCREEN_TIME, true)
    }

    @Synchronized
    fun setSharingDetailedScreenTime(context: Context, share: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHARE_DETAILED_SCREEN_TIME, share).apply()

        val myId = getMyUserId()
        val current = _partyState.value ?: return
        val updatedMembers = current.members.map { member ->
            if (member.userId == myId) {
                member.copy(shareDetailedScreenTime = share)
            } else {
                member
            }
        }
        val updated = current.copy(members = updatedMembers)
        _partyState.value = updated
        savePartyToPrefs(context, updated)

        val myMember = updatedMembers.firstOrNull { it.userId == myId }
        if (myMember != null) {
            broadcastMemberSync(current.partyCode, myMember)
        }
    }

    private const val ACTION_SYNC_MEMBER = "ACTION_SYNC_MEMBER"
    private const val ACTION_REQUEST_INSTALL = "ACTION_REQUEST_INSTALL"
    private const val ACTION_RESOLVE_INSTALL = "ACTION_RESOLVE_INSTALL"
    private const val ACTION_REQUEST_LEAVE = "ACTION_REQUEST_LEAVE"
    private const val ACTION_RESOLVE_LEAVE = "ACTION_RESOLVE_LEAVE"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionGeneration = AtomicLong()
    private const val MAX_MEMBERS = 20
    private const val MAX_REQUESTS = 100
    private var syncJob: Job? = null

    private val _partyState = MutableStateFlow<PartyState?>(null)
    val partyState: StateFlow<PartyState?> = _partyState.asStateFlow()

    private val _isJoined = MutableStateFlow(false)
    val isJoined: StateFlow<Boolean> = _isJoined.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _syncStatus = MutableStateFlow("🟢 Conectado")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Map of packageName to expiry epoch timestamp (millis)
    private val temporarilyAllowedPackages = ConcurrentHashMap<String, Long>()
    private val handledEventIds = ConcurrentHashMap.newKeySet<String>()
    @Volatile
    private var lastPollCursor: String? = null

    fun revokeTemporaryAllowance(packageName: String) {
        temporarilyAllowedPackages.remove(packageName)
    }

    internal fun getTemporarilyAllowedPackagesMap(): ConcurrentHashMap<String, Long> = temporarilyAllowedPackages

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var myId = prefs.getString(KEY_MY_USER_ID, null)
        if (myId.isNullOrBlank()) {
            myId = "user_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_MY_USER_ID, myId).apply()
        }
        _currentUserId.value = myId

        createNotificationChannel(context)
        loadPartyFromPrefs(context)
        refreshMyStats(context)

        if (_partyState.value != null) {
            startCloudSyncLoop(context.applicationContext)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de solicitações de amigos e Friends Panel"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun getMyUserId(): String {
        return _currentUserId.value.ifBlank { "user_local" }
    }

    fun getMyDisplayName(context: Context): String {
        return try {
            val app = context.applicationContext as? LockInApplication ?: LockInApplication.instance
            val name = app.userPreferences.userName.value
            if (name.isNotBlank()) name else "Você"
        } catch (_: Exception) {
            "Você"
        }
    }

    /**
     * Starts continuous cloud synchronization loop using OkHttp and ntfy.sh relay.
     */
    fun startCloudSyncLoop(context: Context) {
        syncJob?.cancel()
        val party = _partyState.value ?: return
        if (!PartyMessageSecurity.isValidCode(party.partyCode)) {
            _syncStatus.value = "Grupo antigo: recrie com convite seguro"
            return
        }
        val generation = sessionGeneration.get()
        syncJob = scope.launch {
            while (isActive) {
                try {
                    val current = _partyState.value
                    if (current == null) {
                        delay(2500L)
                        continue
                    }

                    if (generation != sessionGeneration.get()) return@launch
                    _syncStatus.value = "🟢 Conectado"
                    val batch = FriendsPartyCloudClient.pollEvents(current.partyCode, lastPollCursor)
                    synchronized(this@FriendsPartyManager) {
                        if (generation != sessionGeneration.get() || _partyState.value?.partyCode != current.partyCode) return@launch
                        lastPollCursor = batch.nextCursor
                    }
                    batch.events.forEach { event ->
                        acceptSessionEvent(context, event, current.partyCode, generation)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _syncStatus.value = "🟡 Reconectando..."
                    Log.w(TAG, "Sync loop error: ${e.message}")
                }
                delay(3000L)
            }
        }
    }

    /**
     * Gathers real device stats (screen time, top apps, adherence) and updates current user member record.
     */
    fun refreshMyStats(context: Context) {
        scope.launch {
            try {
                val current = _partyState.value ?: return@launch
                val generation = sessionGeneration.get()
                val myId = getMyUserId()
                val myName = getMyDisplayName(context)

                val limitMinutes = ScreenTimeManager.limitMinutesFlow.value.coerceAtLeast(1)
                val remainingSeconds = ScreenTimeManager.remainingSecondsFlow.value
                val usedSeconds = ((limitMinutes * 60L) - remainingSeconds).coerceAtLeast(0L)

                // Calculate respect / adherence percentage
                val respectPct = if (usedSeconds <= limitMinutes * 60L) {
                    val ratio = 100 - ((usedSeconds.toFloat() / (limitMinutes * 60f)) * 30f).toInt()
                    ratio.coerceIn(70, 100)
                } else {
                    val excess = usedSeconds - (limitMinutes * 60L)
                    val drop = ((excess.toFloat() / 3600f) * 20f).toInt()
                    (70 - drop).coerceIn(10, 69)
                }

                val streak = try {
                    StreakManager.getStreak(context)
                } catch (_: Exception) {
                    1
                }

                // Query top apps from Room or DeviceUsageStatsHelper
                val topAppsList = mutableListOf<FriendAppUsage>()
                try {
                    val app = context.applicationContext as? LockInApplication ?: LockInApplication.instance
                    val restricted = app.repository.getAllAppsList()
                    restricted.take(4).forEach { entity ->
                        val usedAppSec = ((entity.individualLimitMinutes * 60L) - entity.individualRemainingSeconds).coerceAtLeast(0L)
                        topAppsList.add(
                            FriendAppUsage(
                                packageName = entity.packageName,
                                appName = entity.appName,
                                usageSeconds = usedAppSec
                            )
                        )
                    }
                } catch (_: Exception) {}


                val myMemberUpdated = PartyMember(
                    userId = myId,
                    displayName = myName,
                    isHost = current.hostUserId == myId,
                    screenTimeSecondsToday = usedSeconds,
                    screenTimeLimitMinutes = limitMinutes,
                    topApps = topAppsList,
                    respectPercentage = respectPct,
                    streakDays = streak,
                    lastActiveEpoch = System.currentTimeMillis(),
                    isOnline = true,
                    shareDetailedScreenTime = isSharingDetailedScreenTime(context)
                )

                synchronized(this@FriendsPartyManager) {
                val latest = _partyState.value ?: return@launch
                if (latest.partyCode != current.partyCode || generation != sessionGeneration.get()) return@launch
                val updatedMembers = latest.members.filter { it.userId != myId } + myMemberUpdated
                val currentPendingInstalls = _partyState.value?.pendingInstallRequests ?: latest.pendingInstallRequests
                val currentPendingLeaves = _partyState.value?.pendingLeaveRequests ?: latest.pendingLeaveRequests
                val updatedState = latest.copy(
                    members = updatedMembers,
                    pendingInstallRequests = currentPendingInstalls,
                    pendingLeaveRequests = currentPendingLeaves
                )
                _partyState.value = updatedState
                savePartyToPrefs(context, updatedState)

                // Publish my stats to cloud room so friends see live updates
                broadcastMemberSync(current.partyCode, myMemberUpdated)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error refreshing my stats: ${e.message}")
            }
        }
    }

    /**
     * Creates a party using an unguessable invitation secret.
     */
    @Synchronized
    fun createParty(context: Context, partyName: String? = null): String {
        val partyCode = PartyMessageSecurity.createCode()
        resetSessionTracking()
        val myId = getMyUserId()
        val myName = getMyDisplayName(context)

        val hostMember = PartyMember(
            userId = myId,
            displayName = myName,
            isHost = true,
            screenTimeSecondsToday = 0L,
            screenTimeLimitMinutes = ScreenTimeManager.limitMinutesFlow.value.coerceAtLeast(60),
            topApps = emptyList(),
            respectPercentage = 100,
            respectMinutesToday = 0,
            streakDays = StreakManager.getStreak(context).coerceAtLeast(1),
            isOnline = true,
            shareDetailedScreenTime = isSharingDetailedScreenTime(context)
        )

        val newState = PartyState(
            partyCode = partyCode,
            partyName = partyName?.take(80)?.ifBlank { "Lock In Party" } ?: "Lock In Party",
            hostUserId = myId,
            members = listOf(hostMember),
            pendingInstallRequests = emptyList(),
            pendingLeaveRequests = emptyList(),
            alerts = emptyList(),
            createdEpoch = System.currentTimeMillis()
        )

        _partyState.value = newState
        _isJoined.value = true
        savePartyToPrefs(context, newState)
        startCloudSyncLoop(context.applicationContext)
        refreshMyStats(context)

        // Publish creation to cloud room
        broadcastMemberSync(partyCode, hostMember)

        return partyCode
    }

    fun sendCheer(context: Context, targetMemberName: String) {
        val myName = getMyDisplayName(context)
        val alert = PartyAlert(
            memberName = myName,
            alertType = "CHEER",
            message = "mandou força para $targetMemberName manter o foco! 💪"
        )
        addAlert(context, alert)
    }

    fun sendNudge(context: Context, targetMemberName: String) {
        val myName = getMyDisplayName(context)
        val alert = PartyAlert(
            memberName = myName,
            alertType = "NUDGE",
            message = "cobrou foco de $targetMemberName! ⏰ Hora de largar o celular!"
        )
        addAlert(context, alert)
    }

    @Synchronized
    fun addAlert(context: Context, alert: PartyAlert) {
        val current = _partyState.value ?: return
        val updatedAlerts = listOf(alert) + current.alerts.take(20)
        val updatedState = current.copy(alerts = updatedAlerts)
        _partyState.value = updatedState
        savePartyToPrefs(context, updatedState)
    }

    /**
     * Joins an existing party using a code and connects via cloud room.
     */
    @Synchronized
    fun joinParty(context: Context, code: String): Boolean {
        val cleanCode = normalizeCode(code)
        if (!PartyMessageSecurity.isValidCode(cleanCode)) return false
        resetSessionTracking()

        val myId = getMyUserId()
        val myName = getMyDisplayName(context)

        val limitMinutes = ScreenTimeManager.limitMinutesFlow.value.coerceAtLeast(60)
        val remainingSeconds = ScreenTimeManager.remainingSecondsFlow.value
        val usedSeconds = ((limitMinutes * 60L) - remainingSeconds).coerceAtLeast(0L)

        val myMember = PartyMember(
            userId = myId,
            displayName = myName,
            isHost = false,
            screenTimeSecondsToday = usedSeconds,
            screenTimeLimitMinutes = limitMinutes,
            topApps = emptyList(),
            respectPercentage = 100,
            streakDays = StreakManager.getStreak(context).coerceAtLeast(1),
            isOnline = true,
            shareDetailedScreenTime = isSharingDetailedScreenTime(context)
        )

        val newState = PartyState(
            partyCode = cleanCode,
            partyName = "Lock In Party",
            hostUserId = "",
            members = listOf(myMember),
            pendingInstallRequests = emptyList(),
            pendingLeaveRequests = emptyList(),
            createdEpoch = System.currentTimeMillis()
        )

        _partyState.value = newState
        _isJoined.value = true
        lastPollCursor = null
        savePartyToPrefs(context, newState)
        startCloudSyncLoop(context.applicationContext)
        refreshMyStats(context)

        // Polling and discovery use the same guarded event path. No detached room results.
        broadcastMemberSync(cleanCode, myMember)
        return true
    }

    fun normalizeCode(input: String): String = PartyMessageSecurity.normalizeCode(input)

    @Synchronized
    private fun acceptSessionEvent(context: Context, event: CloudPartyEvent, code: String, generation: Long) {
        if (sessionGeneration.get() != generation || _partyState.value?.partyCode != code) return
        if (!handledEventIds.add(event.eventId)) return
        if (handledEventIds.size > 2048) {
            handledEventIds.clear()
            handledEventIds.add(event.eventId)
        }
        processIncomingCloudEvent(context, event)
    }

    private fun resetSessionTracking() {
        sessionGeneration.incrementAndGet()
        syncJob?.cancel()
        handledEventIds.clear()
        temporarilyAllowedPackages.clear()
        lastPollCursor = null
    }

    /**
     * Checks if a package is temporarily allowed to be installed/opened (e.g. approved by friend).
     */
    fun isPackageTemporarilyAllowed(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val expiry = temporarilyAllowedPackages[packageName] ?: return false
        val now = System.currentTimeMillis()
        return if (now < expiry) {
            true
        } else {
            temporarilyAllowedPackages.remove(packageName)
            false
        }
    }

    /**
     * Temporarily allows a package to be installed or opened for [durationMinutes].
     */
    fun allowTemporaryInstall(packageName: String, durationMinutes: Int = 10) {
        val expiry = System.currentTimeMillis() + (durationMinutes.coerceIn(1, 10) * 60_000L)
        temporarilyAllowedPackages[packageName] = expiry
    }

    /**
     * Checks if there is any pending installation request in the active party.
     * The user CANNOT leave the party while this is true.
     */
    fun hasPendingInstallRequest(): Boolean {
        val current = _partyState.value ?: return false
        return current.pendingInstallRequests.any { it.status == RequestStatus.PENDING }
    }

    /**
     * Checks if there is any pending leave request in the active party.
     */
    fun hasPendingLeaveRequest(): Boolean {
        val current = _partyState.value ?: return false
        return current.pendingLeaveRequests.any { it.status == RequestStatus.PENDING }
    }

    /**
     * When a blacklisted app installation is attempted, creates an approval request.
     * Broadcasts real-time notification to other party members via Cloud Relay.
     */
    @Synchronized
    fun requestInstallApproval(
        context: Context,
        targetPackage: String,
        targetAppName: String
    ): InstallApprovalRequest? {
        val current = _partyState.value ?: return null
        if (!PartyMessageSecurity.isValidCode(current.partyCode) || targetPackage.isBlank() || targetPackage.length > 255) return null
        if (current.pendingInstallRequests.count { it.status == RequestStatus.PENDING } >= MAX_REQUESTS) return null
        val myId = getMyUserId()
        val myName = getMyDisplayName(context)

        // Check if an identical pending request already exists
        val existing = current.pendingInstallRequests.find {
            it.requesterId == myId && it.targetPackage == targetPackage && it.status == RequestStatus.PENDING
        }
        if (existing != null) {
            return existing
        }

        val request = InstallApprovalRequest(
            requestId = "req_inst_" + UUID.randomUUID().toString(),
            requesterId = myId,
            requesterName = myName,
            targetPackage = targetPackage,
            targetAppName = targetAppName,
            timestamp = System.currentTimeMillis(),
            status = RequestStatus.PENDING
        )

        val updatedList = trimInstallHistory(current.pendingInstallRequests) + request
        val updatedState = current.copy(pendingInstallRequests = updatedList)
        _partyState.value = updatedState
        savePartyToPrefs(context, updatedState)

        // Broadcast to cloud room
        scope.launch {
            val payload = serializeInstallRequest(request)
            FriendsPartyCloudClient.publishEvent(
                partyCode = current.partyCode,
                action = ACTION_REQUEST_INSTALL,
                senderId = myId,
                payload = payload,
                notificationTitle = "⚠️ Pedido de Instalação: $targetAppName",
                notificationBody = "$myName quer instalar $targetAppName! Abra o Friends Panel para aprovar ou recusar."
            )
        }

        return request
    }

    /**
     * Responds to an installation request (Approve or Reject).
     * If approved, grants a 10-minute install window and broadcasts response to party.
     */
    @Synchronized
    fun respondToInstallRequest(
        context: Context,
        requestId: String,
        approved: Boolean
    ): Boolean {
        val current = _partyState.value ?: return false
        val myId = getMyUserId()
        val myName = getMyDisplayName(context)

        val request = current.pendingInstallRequests.firstOrNull { it.requestId == requestId } ?: return false
        if (request.status != RequestStatus.PENDING || request.requesterId == myId || current.members.none { it.userId == myId }) return false
        val matchedPackage = request.targetPackage
        val targetAppName = request.targetAppName
        val updatedRequests = current.pendingInstallRequests.map { req ->
            if (req.requestId == requestId) req.copy(
                status = if (approved) RequestStatus.APPROVED else RequestStatus.REJECTED,
                resolvedById = myId, resolvedByName = myName, resolvedEpoch = System.currentTimeMillis()
            ) else req
        }
        val updatedState = current.copy(pendingInstallRequests = updatedRequests)
        _partyState.value = updatedState
        savePartyToPrefs(context, updatedState)
        // Only the requester's device receives a temporary allowance from the peer event.

        // Broadcast resolution to cloud room
        scope.launch {
            val payload = JSONObject().apply {
                put("requestId", requestId)
                put("status", if (approved) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name)
                put("resolvedById", myId)
                put("resolvedByName", myName)
                put("targetPackage", matchedPackage)
                put("targetAppName", targetAppName)
            }
            FriendsPartyCloudClient.publishEvent(
                partyCode = current.partyCode,
                action = ACTION_RESOLVE_INSTALL,
                senderId = myId,
                payload = payload,
                notificationTitle = if (approved) "✅ Instalação Permitida!" else "❌ Instalação Recusada",
                notificationBody = if (approved) "$myName permitiu a instalação de $targetAppName (10m)." else "$myName recusou a instalação de $targetAppName."
            )
        }

        return true
    }

    /**
     * Requests to leave / unpair from the party.
     * Enforces strict rules:
     * 1. Cannot leave if an install request is currently pending!
     * 2. If multiple users are in the party, unpairing REQUIRES approval from another member.
     *
     * @return Pair<Boolean, String> where first is success/allowed, second is informational message.
     */
    @Synchronized
    fun requestLeaveParty(context: Context): Pair<Boolean, String> {
        val current = _partyState.value ?: return Pair(true, "Você não está em nenhuma party.")

        if (!PartyMessageSecurity.isValidCode(current.partyCode)) {
            forceLeaveParty(context)
            return true to "Grupo antigo removido. Crie um grupo com convite seguro."
        }
        // RULE 1: Cannot leave while an install request is pending!
        if (hasPendingInstallRequest()) {
            return Pair(
                false,
                "Ação Bloqueada: Não é permitido sair do grupo enquanto houver uma solicitação de instalação pendente. Aguarde a aprovação ou recusa dos seus amigos!"
            )
        }

        // Check if user is the sole member
        if (current.members.size <= 1) {
            forceLeaveParty(context)
            return Pair(true, "Você despareou e saiu do grupo com sucesso.")
        }

        val myId = getMyUserId()
        val myName = getMyDisplayName(context)

        // Check if there is already a pending leave request
        val existing = current.pendingLeaveRequests.find {
            it.requesterId == myId && it.status == RequestStatus.PENDING
        }
        if (existing != null) {
            return Pair(
                false,
                "Já existe uma solicitação de saída pendente. Aguarde um dos outros membros aprovar seu despareamento."
            )
        }

        if (current.pendingLeaveRequests.count { it.status == RequestStatus.PENDING } >= MAX_REQUESTS) return false to "Limite de solicitações atingido."
        // RULE 2: Unpairing requires approval of another member!
        val leaveReq = LeaveApprovalRequest(
            requestId = "req_leave_" + UUID.randomUUID().toString(),
            requesterId = myId,
            requesterName = myName,
            timestamp = System.currentTimeMillis(),
            status = RequestStatus.PENDING
        )

        val updatedState = current.copy(
            pendingLeaveRequests = trimLeaveHistory(current.pendingLeaveRequests) + leaveReq
        )
        _partyState.value = updatedState
        savePartyToPrefs(context, updatedState)

        // Broadcast to cloud room
        scope.launch {
            val payload = serializeLeaveRequest(leaveReq)
            FriendsPartyCloudClient.publishEvent(
                partyCode = current.partyCode,
                action = ACTION_REQUEST_LEAVE,
                senderId = myId,
                payload = payload,
                notificationTitle = "🚪 Pedido de Despareamento",
                notificationBody = "$myName solicitou sair do grupo. Abra o Friends Panel para aprovar ou recusar a saída."
            )
        }

        return Pair(
            false,
            "Solicitação de saída enviada! Para evitar despareamentos por impulso, um dos outros membros do grupo precisa aprovar sua saída."
        )
    }

    /**
     * Responds to a member's request to leave the party.
     */
    @Synchronized
    fun respondToLeaveRequest(
        context: Context,
        requestId: String,
        approved: Boolean
    ): Boolean {
        val current = _partyState.value ?: return false
        val myId = getMyUserId()
        val myName = getMyDisplayName(context)

        val request = current.pendingLeaveRequests.firstOrNull { it.requestId == requestId } ?: return false
        if (request.status != RequestStatus.PENDING || request.requesterId == myId || current.members.none { it.userId == myId }) return false
        val leavingMemberId = request.requesterId
        val leavingMemberName = request.requesterName
        val updatedRequests = current.pendingLeaveRequests.map { req ->
            if (req.requestId == requestId) req.copy(
                status = if (approved) RequestStatus.APPROVED else RequestStatus.REJECTED,
                resolvedById = myId, resolvedByName = myName, resolvedEpoch = System.currentTimeMillis()
            ) else req
        }
        val updated = current.copy(
            members = if (approved) current.members.filter { it.userId != leavingMemberId } else current.members,
            pendingLeaveRequests = updatedRequests
        )
        _partyState.value = updated
        savePartyToPrefs(context, updated)

        // Broadcast leave resolution to cloud room
        scope.launch {
            val payload = JSONObject().apply {
                put("requestId", requestId)
                put("leavingMemberId", leavingMemberId)
                put("leavingMemberName", leavingMemberName)
                put("status", if (approved) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name)
                put("resolvedById", myId)
                put("resolvedByName", myName)
            }
            FriendsPartyCloudClient.publishEvent(
                partyCode = current.partyCode,
                action = ACTION_RESOLVE_LEAVE,
                senderId = myId,
                payload = payload,
                notificationTitle = if (approved) "🚪 Despareamento Aprovado" else "❌ Despareamento Negado",
                notificationBody = if (approved) "$leavingMemberName foi autorizado a sair do grupo." else "$myName recusou a saída de $leavingMemberName."
            )
        }

        return true
    }

    /**
     * Broadcasts current member statistics to cloud room.
     */
    private fun broadcastMemberSync(partyCode: String, member: PartyMember) {
        scope.launch {
            if (_partyState.value?.partyCode != partyCode) return@launch
            val payload = serializeMemberForNetwork(member)
            FriendsPartyCloudClient.publishEvent(
                partyCode = partyCode,
                action = ACTION_SYNC_MEMBER,
                senderId = member.userId,
                payload = payload
            )
        }
    }

    /**
     * Ingests an incoming cloud event from a connected peer.
     */
    @Synchronized
    internal fun processIncomingCloudEvent(context: Context, event: CloudPartyEvent) {
        try {
            processValidCloudEvent(context, event)
        } catch (_: org.json.JSONException) {
            // A malformed peer message must not discard the rest of the batch or stop sync.
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun processValidCloudEvent(context: Context, event: CloudPartyEvent) {
        val myId = getMyUserId()
        if (event.senderId.isBlank() || event.senderId.length > 128 || event.senderId == myId) return
        val current = _partyState.value ?: return
        if (!PartyMessageSecurity.isValidCode(current.partyCode)) return
        if (event.action == ACTION_SYNC_MEMBER) {
            val member = deserializePartyMember(event.payload)
            if (member.userId != event.senderId || member.userId == myId) return
            if (current.members.none { it.userId == member.userId } && current.members.size >= MAX_MEMBERS) return
            val updated = current.copy(members = current.members.filter { it.userId != member.userId } + member)
            _partyState.value = updated
            savePartyToPrefs(context, updated)
            return
        }
        val sender = current.members.firstOrNull { it.userId == event.senderId } ?: return
        when (event.action) {
            ACTION_REQUEST_INSTALL -> {
                val req = deserializeInstallRequest(event.payload)
                if (req.requestId.isBlank() || req.requestId.length > 128 || req.requesterId != sender.userId ||
                    event.payload.optString("status") != RequestStatus.PENDING.name || req.status != RequestStatus.PENDING || req.targetPackage.isBlank() || req.targetPackage.length > 255 ||
                    current.pendingInstallRequests.count { it.status == RequestStatus.PENDING } >= MAX_REQUESTS || current.pendingInstallRequests.any { it.requestId == req.requestId }) return
                val updated = current.copy(pendingInstallRequests = trimInstallHistory(current.pendingInstallRequests) + req.copy(requesterName = sender.displayName))
                _partyState.value = updated
                savePartyToPrefs(context, updated)
                showNotification(context, 8801, "Pedido de instalação", "${sender.displayName} pediu autorização para ${req.targetAppName}.")
            }
            ACTION_RESOLVE_INSTALL -> {
                val req = current.pendingInstallRequests.firstOrNull { it.requestId == event.payload.optString("requestId") } ?: return
                val status = resolutionStatus(event.payload) ?: return
                if (req.status != RequestStatus.PENDING || req.requesterId == sender.userId) return
                val updated = current.copy(pendingInstallRequests = current.pendingInstallRequests.map {
                    if (it.requestId == req.requestId) it.copy(status = status, resolvedById = sender.userId,
                        resolvedByName = sender.displayName, resolvedEpoch = System.currentTimeMillis()) else it
                })
                _partyState.value = updated
                savePartyToPrefs(context, updated)
                if (req.requesterId == myId) {
                    if (status == RequestStatus.APPROVED) allowTemporaryInstall(req.targetPackage)
                    showNotification(context, 8802, if (status == RequestStatus.APPROVED) "Instalação permitida" else "Instalação recusada",
                        "${sender.displayName} respondeu ao pedido para ${req.targetAppName}.")
                }
            }
            ACTION_REQUEST_LEAVE -> {
                val req = deserializeLeaveRequest(event.payload)
                if (req.requestId.isBlank() || req.requestId.length > 128 || req.requesterId != sender.userId ||
                    event.payload.optString("status") != RequestStatus.PENDING.name || req.status != RequestStatus.PENDING || current.pendingLeaveRequests.count { it.status == RequestStatus.PENDING } >= MAX_REQUESTS ||
                    current.pendingLeaveRequests.any { it.requestId == req.requestId }) return
                val updated = current.copy(pendingLeaveRequests = trimLeaveHistory(current.pendingLeaveRequests) + req.copy(requesterName = sender.displayName))
                _partyState.value = updated
                savePartyToPrefs(context, updated)
                showNotification(context, 8804, "Pedido de despareamento", "${sender.displayName} pediu para sair do grupo.")
            }
            ACTION_RESOLVE_LEAVE -> {
                val req = current.pendingLeaveRequests.firstOrNull { it.requestId == event.payload.optString("requestId") } ?: return
                val status = resolutionStatus(event.payload) ?: return
                if (req.status != RequestStatus.PENDING || req.requesterId == sender.userId) return
                if (status == RequestStatus.APPROVED && req.requesterId == myId) {
                    forceLeaveParty(context)
                    showNotification(context, 8805, "Despareamento concluído", "Seu pedido de saída foi aprovado.")
                } else {
                    val updated = current.copy(
                        members = if (status == RequestStatus.APPROVED) current.members.filter { it.userId != req.requesterId } else current.members,
                        pendingLeaveRequests = current.pendingLeaveRequests.map {
                            if (it.requestId == req.requestId) it.copy(status = status, resolvedById = sender.userId,
                                resolvedByName = sender.displayName, resolvedEpoch = System.currentTimeMillis()) else it
                        }
                    )
                    _partyState.value = updated
                    savePartyToPrefs(context, updated)
                }
            }
        }
    }

    private fun trimInstallHistory(requests: List<InstallApprovalRequest>): List<InstallApprovalRequest> {
        val pending = requests.filter { it.status == RequestStatus.PENDING }
        return pending + requests.filter { it.status != RequestStatus.PENDING }.takeLast((MAX_REQUESTS - pending.size - 1).coerceAtLeast(0))
    }

    private fun trimLeaveHistory(requests: List<LeaveApprovalRequest>): List<LeaveApprovalRequest> {
        val pending = requests.filter { it.status == RequestStatus.PENDING }
        return pending + requests.filter { it.status != RequestStatus.PENDING }.takeLast((MAX_REQUESTS - pending.size - 1).coerceAtLeast(0))
    }

    private fun resolutionStatus(payload: JSONObject): RequestStatus? = when (payload.optString("status")) {
        RequestStatus.APPROVED.name -> RequestStatus.APPROVED
        RequestStatus.REJECTED.name -> RequestStatus.REJECTED
        else -> null
    }

    internal fun serializeMemberForNetwork(member: PartyMember): JSONObject = serializePartyMember(
        if (member.shareDetailedScreenTime) member else member.copy(topApps = emptyList(), screenTimeSecondsToday = 0L)
    )

    /**
     * Force leave / reset for testing or clean slate.
     */
    @Synchronized
    fun forceLeaveParty(context: Context) {
        resetSessionTracking()
        syncJob = null
        _partyState.value = null
        _isJoined.value = false
        handledEventIds.clear()
        temporarilyAllowedPackages.clear()
        lastPollCursor = null
        clearPartyPrefs(context)
    }

    @Synchronized
    fun addMember(context: Context, member: PartyMember) {
        val current = _partyState.value ?: return
        if (current.members.none { it.userId == member.userId } && current.members.size >= MAX_MEMBERS) return
        val updated = current.copy(members = current.members.filter { it.userId != member.userId } + member)
        _partyState.value = updated
        savePartyToPrefs(context, updated)
    }

    fun addSimulatedFriend(
        context: Context,
        name: String,
        screenTimeSec: Long = 5400L,
        limitMin: Int = 180,
        respectPct: Int = 94,
        streakDays: Int = 6,
        topApps: List<FriendAppUsage> = listOf(
            FriendAppUsage("com.whatsapp", "WhatsApp", 2400L),
            FriendAppUsage("com.google.android.youtube", "YouTube", 1800L)
        )
    ) {
        val current = _partyState.value ?: return
        val newFriend = PartyMember(
            userId = "friend_" + UUID.randomUUID().toString().take(6),
            displayName = name,
            isHost = false,
            screenTimeSecondsToday = screenTimeSec,
            screenTimeLimitMinutes = limitMin,
            topApps = topApps,
            respectPercentage = respectPct,
            streakDays = streakDays,
            violationsToday = 0,
            isOnline = true
        )
        addMember(context, newFriend)
    }

    private fun showNotification(context: Context, notificationId: Int, title: String, body: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_SCREEN", "FRIENDS_PANEL")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to display notification: ${e.message}")
        }
    }

    // ==========================================
    // JSON SERIALIZATION
    // ==========================================

    private fun serializePartyMember(m: PartyMember): JSONObject {
        val mObj = JSONObject()
        mObj.put("userId", m.userId)
        mObj.put("displayName", m.displayName.take(80))
        mObj.put("isHost", m.isHost)
        mObj.put("screenTimeSecondsToday", m.screenTimeSecondsToday)
        mObj.put("screenTimeLimitMinutes", m.screenTimeLimitMinutes)
        mObj.put("respectPercentage", m.respectPercentage)
        mObj.put("respectMinutesToday", m.respectMinutesToday)
        mObj.put("streakDays", m.streakDays)
        mObj.put("violationsToday", m.violationsToday)
        mObj.put("lastActiveEpoch", m.lastActiveEpoch)
        mObj.put("isOnline", m.isOnline)
        mObj.put("shareDetailedScreenTime", m.shareDetailedScreenTime)

        val appsArray = JSONArray()
        m.topApps.take(4).forEach { app ->
            val appObj = JSONObject()
            appObj.put("packageName", app.packageName.take(255))
            appObj.put("appName", app.appName.take(80))
            appObj.put("usageSeconds", app.usageSeconds)
            appsArray.put(appObj)
        }
        mObj.put("topApps", appsArray)
        return mObj
    }

    private fun deserializePartyMember(mObj: JSONObject): PartyMember {
        val apps = mutableListOf<FriendAppUsage>()
        val appsArray = mObj.optJSONArray("topApps")
        if (appsArray != null) {
            for (j in 0 until minOf(appsArray.length(), 20)) {
                val appObj = appsArray.getJSONObject(j)
                apps.add(
                    FriendAppUsage(
                        packageName = appObj.optString("packageName"),
                        appName = appObj.optString("appName"),
                        usageSeconds = appObj.optLong("usageSeconds")
                    )
                )
            }
        }
        return PartyMember(
            userId = mObj.optString("userId"),
            displayName = mObj.optString("displayName", "Amigo").take(80),
            isHost = mObj.optBoolean("isHost"),
            screenTimeSecondsToday = mObj.optLong("screenTimeSecondsToday"),
            screenTimeLimitMinutes = mObj.optInt("screenTimeLimitMinutes", 180),
            topApps = apps,
            respectPercentage = mObj.optInt("respectPercentage", 100).coerceIn(0, 100),
            respectMinutesToday = mObj.optInt("respectMinutesToday", 180),
            streakDays = mObj.optInt("streakDays", 0),
            violationsToday = mObj.optInt("violationsToday", 0),
            lastActiveEpoch = mObj.optLong("lastActiveEpoch"),
            isOnline = mObj.optBoolean("isOnline", true),
            shareDetailedScreenTime = mObj.optBoolean("shareDetailedScreenTime", true)
        )
    }

    private fun serializeInstallRequest(req: InstallApprovalRequest): JSONObject {
        val rObj = JSONObject()
        rObj.put("requestId", req.requestId)
        rObj.put("requesterId", req.requesterId)
        rObj.put("requesterName", req.requesterName.take(80))
        rObj.put("targetPackage", req.targetPackage)
        rObj.put("targetAppName", req.targetAppName.take(80))
        rObj.put("timestamp", req.timestamp)
        rObj.put("status", req.status.name)
        req.resolvedById?.let { rObj.put("resolvedById", it) }
        req.resolvedByName?.let { rObj.put("resolvedByName", it) }
        req.resolvedEpoch?.let { rObj.put("resolvedEpoch", it) }
        return rObj
    }

    private fun deserializeInstallRequest(rObj: JSONObject): InstallApprovalRequest {
        return InstallApprovalRequest(
            requestId = rObj.optString("requestId"),
            requesterId = rObj.optString("requesterId"),
            requesterName = rObj.optString("requesterName"),
            targetPackage = rObj.optString("targetPackage"),
            targetAppName = rObj.optString("targetAppName"),
            timestamp = rObj.optLong("timestamp"),
            status = try {
                RequestStatus.valueOf(rObj.optString("status"))
            } catch (_: Exception) {
                RequestStatus.PENDING
            },
            resolvedById = rObj.optString("resolvedById").ifBlank { null },
            resolvedByName = rObj.optString("resolvedByName").ifBlank { null },
            resolvedEpoch = if (rObj.has("resolvedEpoch")) rObj.optLong("resolvedEpoch") else null
        )
    }

    private fun serializeLeaveRequest(req: LeaveApprovalRequest): JSONObject {
        val lObj = JSONObject()
        lObj.put("requestId", req.requestId)
        lObj.put("requesterId", req.requesterId)
        lObj.put("requesterName", req.requesterName.take(80))
        lObj.put("timestamp", req.timestamp)
        lObj.put("status", req.status.name)
        req.resolvedById?.let { lObj.put("resolvedById", it) }
        req.resolvedByName?.let { lObj.put("resolvedByName", it) }
        req.resolvedEpoch?.let { lObj.put("resolvedEpoch", it) }
        return lObj
    }

    private fun deserializeLeaveRequest(lObj: JSONObject): LeaveApprovalRequest {
        return LeaveApprovalRequest(
            requestId = lObj.optString("requestId"),
            requesterId = lObj.optString("requesterId"),
            requesterName = lObj.optString("requesterName"),
            timestamp = lObj.optLong("timestamp"),
            status = try {
                RequestStatus.valueOf(lObj.optString("status"))
            } catch (_: Exception) {
                RequestStatus.PENDING
            },
            resolvedById = lObj.optString("resolvedById").ifBlank { null },
            resolvedByName = lObj.optString("resolvedByName").ifBlank { null },
            resolvedEpoch = if (lObj.has("resolvedEpoch")) lObj.optLong("resolvedEpoch") else null
        )
    }

    private fun savePartyToPrefs(context: Context, state: PartyState) {
        try {
            val root = JSONObject()
            root.put("partyCode", state.partyCode)
            root.put("partyName", state.partyName)
            root.put("hostUserId", state.hostUserId)
            root.put("createdEpoch", state.createdEpoch)

            val membersArray = JSONArray()
            state.members.forEach { m -> membersArray.put(serializePartyMember(m)) }
            root.put("members", membersArray)

            val installArray = JSONArray()
            state.pendingInstallRequests.forEach { req -> installArray.put(serializeInstallRequest(req)) }
            root.put("pendingInstallRequests", installArray)

            val leaveArray = JSONArray()
            state.pendingLeaveRequests.forEach { req -> leaveArray.put(serializeLeaveRequest(req)) }
            root.put("pendingLeaveRequests", leaveArray)

            val alertsArray = JSONArray()
            state.alerts.forEach { a ->
                val aObj = JSONObject()
                aObj.put("id", a.id)
                aObj.put("memberName", a.memberName)
                aObj.put("appName", a.appName)
                aObj.put("alertType", a.alertType)
                aObj.put("message", a.message)
                aObj.put("timestamp", a.timestamp)
                alertsArray.put(aObj)
            }
            root.put("alerts", alertsArray)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PARTY_STATE_JSON, root.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving party to prefs: ${e.message}")
        }
    }

    private fun loadPartyFromPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_PARTY_STATE_JSON, null)
            if (!jsonStr.isNullOrBlank()) {
                val root = JSONObject(jsonStr)
                val partyCode = root.optString("partyCode", "LOCK")
                val partyName = root.optString("partyName", "Lock In Party")
                val hostUserId = root.optString("hostUserId", "")
                val createdEpoch = root.optLong("createdEpoch", System.currentTimeMillis())

                val members = mutableListOf<PartyMember>()
                val membersArray = root.optJSONArray("members")
                if (membersArray != null) {
                    for (i in 0 until minOf(membersArray.length(), MAX_MEMBERS)) {
                        members.add(deserializePartyMember(membersArray.getJSONObject(i)))
                    }
                }

                val installRequests = mutableListOf<InstallApprovalRequest>()
                val installArray = root.optJSONArray("pendingInstallRequests")
                if (installArray != null) {
                    for (i in 0 until minOf(installArray.length(), MAX_REQUESTS)) {
                        installRequests.add(deserializeInstallRequest(installArray.getJSONObject(i)))
                    }
                }

                val leaveRequests = mutableListOf<LeaveApprovalRequest>()
                val leaveArray = root.optJSONArray("pendingLeaveRequests")
                if (leaveArray != null) {
                    for (i in 0 until minOf(leaveArray.length(), MAX_REQUESTS)) {
                        leaveRequests.add(deserializeLeaveRequest(leaveArray.getJSONObject(i)))
                    }
                }

                val alerts = mutableListOf<PartyAlert>()
                val alertsArray = root.optJSONArray("alerts")
                if (alertsArray != null) {
                    for (i in 0 until minOf(alertsArray.length(), 20)) {
                        val aObj = alertsArray.getJSONObject(i)
                        alerts.add(
                            PartyAlert(
                                id = aObj.optString("id"),
                                memberName = aObj.optString("memberName"),
                                appName = aObj.optString("appName"),
                                alertType = aObj.optString("alertType", "OVER_LIMIT"),
                                message = aObj.optString("message"),
                                timestamp = aObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                _partyState.value = PartyState(
                    partyCode = partyCode,
                    partyName = partyName,
                    hostUserId = hostUserId,
                    members = members,
                    pendingInstallRequests = installRequests,
                    pendingLeaveRequests = leaveRequests,
                    alerts = alerts,
                    createdEpoch = createdEpoch
                )
                _isJoined.value = true
            } else {
                _partyState.value = null
                _isJoined.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading party from prefs: ${e.message}")
            _partyState.value = null
            _isJoined.value = false
        }
    }

    private fun clearPartyPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_PARTY_STATE_JSON).apply()
        } catch (_: Exception) {}
    }
}
