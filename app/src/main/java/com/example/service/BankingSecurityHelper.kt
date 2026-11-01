package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BankingSecurityHelper
 *
 * Provides comprehensive isolation, whitelisting and protection for banking,
 * fintech, payment, and investment applications.
 *
 * Ensures Lock In's Accessibility Service never inspects, queries, or interferes
 * with sensitive banking windows, preventing security SDKs (Kaspersky, AppGuardian,
 * ThreatMetrix, Shield) from flagging Lock In as a suspicious overlay/sideloaded RAT.
 */
object BankingSecurityHelper {

    private const val TAG = "BankingSecurityHelper"
    private const val PREFS_NAME = "lock_in_banking_security_prefs"
    private const val KEY_PAUSE_UNTIL = "banking_pause_until_epoch"

    @Volatile
    private var pauseUntilEpoch: Long = 0L

    private val _isPausedFlow = MutableStateFlow(false)
    val isPausedFlow: StateFlow<Boolean> = _isPausedFlow.asStateFlow()

    /**
     * Comprehensive registry of Brazilian and major international banking / fintech packages.
     */
    val allKnownBankingPackages: Set<String> get() = KNOWN_BANKING_PACKAGES

    private val KNOWN_BANKING_PACKAGES = setOf(
        // Caixa Econômica Federal (All official apps)
        "br.com.gabba.caixa",
        "br.com.gabba.Caixa",
        "br.gov.caixa.tem",
        "br.gov.caixa.atendimentocidadao",
        "br.gov.caixa.fgts.trabalhador",
        "br.gov.caixa.cartoes",
        "br.gov.caixa.habitacao",
        "br.gov.caixa.ib",
        "br.gov.caixa.loterias",
        "br.gov.caixa.previdencia",
        "br.gov.caixa.seguros",
        "br.gov.caixa.beneficios",
        "br.com.caixa.dpm",
        "br.com.caixaseguros.seguroauto",
        // Nubank
        "com.nu.production",
        "com.nubank",
        // Itaú
        "com.itau",
        "com.itau.personnalite",
        "com.itau.empresas",
        "com.itau.investimentos",
        "com.itau.cartoes",
        "com.itau.iti",
        // Bradesco
        "com.bradesco",
        "com.bradesco.cartoes",
        "com.bradesco.prime",
        "com.bradesco.empresas",
        "br.com.bradesco.next",
        "br.com.bradesco.digiconta",
        // Banco do Brasil
        "br.com.bb.android",
        "br.com.bb.investimentos",
        "br.com.bb.cartoes",
        // Santander
        "com.santander.app",
        "com.santander.app.way",
        "com.santander.empresas",
        // Inter
        "br.com.intermedium",
        "br.com.bancointer.interpag",
        // C6 Bank
        "com.c6bank.app",
        "com.c6bank.empresas",
        // Mercado Pago
        "com.mercadopago.wallet",
        // PicPay
        "com.picpay",
        // PagBank / PagSeguro
        "br.com.uol.ps.myaccount",
        "br.com.uol.ps",
        // Neon
        "br.com.neon",
        // BTG Pactual
        "com.btg.pactual.banking",
        "com.btg.pactual.investimentos",
        // XP Investimentos / Rico / Clear
        "br.com.xp.investimentos",
        "br.com.xp.carteira",
        "br.com.rico",
        "br.com.clear",
        // Banco Pan
        "br.com.bancopan.pan",
        // Cooperativas
        "br.com.sicoobnet",
        "br.com.sicredi.mobile",
        "br.com.banrisul.mobile",
        "br.com.banrisul.office",
        "br.com.safra.mobile",
        "br.com.safrapay",
        // BMG, Daycoval, Will, Agibank
        "br.com.bmg.mobile",
        "br.com.daycoval.mobile",
        "com.willbank.app",
        "br.com.agibank",
        "br.com.original.bank",
        // Adquirentes e Pagamentos
        "co.stone.ton",
        "br.com.stone",
        "io.cloudwalk.infinitepay",
        "com.infinitepay.app",
        "br.com.cielo.mobile",
        "br.com.rede.app",
        // Cripto & Global Fintechs
        "com.binance.dev",
        "com.coinbase.android",
        "com.revolut.revolut",
        "com.wise",
        "com.nomad",
        "com.avenue",
        "br.com.mercadobitcoin.app",
        "br.gov.meugovbr"
    )

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        pauseUntilEpoch = prefs.getLong(KEY_PAUSE_UNTIL, 0L)
        updatePauseState()
    }

    private fun updatePauseState(): Boolean {
        val now = System.currentTimeMillis()
        val paused = now < pauseUntilEpoch
        _isPausedFlow.value = paused
        return paused
    }

    /**
     * Checks if a given package is a banking or financial app that must be
     * completely exempted from any accessibility inspection or overlay.
     */
    fun isBankingOrFinancialApp(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.lowercase()

        // 1. Direct match in known banking registry
        if (KNOWN_BANKING_PACKAGES.contains(lower)) {
            return true
        }

        // 2. Strict prefix match for all government & Caixa apps
        if (lower.startsWith("br.gov.caixa.") || lower.startsWith("br.com.caixa.")) {
            return true
        }

        // 3. Heuristic package keywords commonly used by banking and financial SDKs
        if (lower.contains("caixa") ||
            lower.contains("gabba") ||
            lower.contains(".bank") ||
            lower.contains(".banco") ||
            lower.contains("bancodobrasil") ||
            lower.contains("bradesco") ||
            lower.contains("santander") ||
            lower.contains("nubank") ||
            lower.contains("itau") ||
            lower.contains("mercadopago") ||
            lower.contains("picpay") ||
            lower.contains("pagbank") ||
            lower.contains("pagseguro") ||
            lower.contains("intermedium") ||
            lower.contains("bancointer") ||
            lower.contains("c6bank") ||
            lower.contains("sicoob") ||
            lower.contains("sicredi") ||
            lower.contains("banrisul") ||
            lower.contains("safra") ||
            lower.contains("bancopan") ||
            lower.contains("daycoval") ||
            lower.contains("agibank") ||
            lower.contains("willbank") ||
            lower.contains("fintech") ||
            lower.contains("carteira") ||
            lower.contains(".invest") ||
            lower.contains(".wallet") ||
            lower.contains("crypto")
        ) {
            return true
        }

        return false
    }

    /**
     * Returns true if banking protection pause is currently active.
     */
    fun isBankingProtectionPaused(): Boolean {
        return updatePauseState()
    }

    /**
     * Temporarily pauses accessibility/monitoring to allow seamless, uninterrupted banking access.
     */
    fun pauseForBankingAccess(context: Context, durationMinutes: Int = 10) {
        val until = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        pauseUntilEpoch = until
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_PAUSE_UNTIL, until).apply()
        _isPausedFlow.value = true

        // 1. Isolate Accessibility Service so bank anti-fraud scanner sees zero inspection capability
        LockInAccessibilityService.instance?.setBankingIsolationMode(true)

        // 2. Hide any active Floating Island overlay so bank detects no overlay window
        FloatingIslandService.hideIsland(context)

        // 3. Temporarily stop VPN if running to avoid bank VPN detection
        try {
            if (LockInVpnManager.isVpnRunning.value) {
                LockInVpnManager.stopVpn(context)
            }
        } catch (_: Exception) {}

        Log.i(TAG, "Banking pause activated for $durationMinutes minutes (until $until)")
    }

    /**
     * Resumes protection immediately.
     */
    fun resumeProtectionImmediately(context: Context) {
        pauseUntilEpoch = 0L
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_PAUSE_UNTIL, 0L).apply()
        _isPausedFlow.value = false

        // 1. Restore standard Accessibility Service monitoring
        LockInAccessibilityService.instance?.setBankingIsolationMode(false)

        // 2. Restore VPN if it was previously enabled
        try {
            if (LockInVpnManager.isVpnEnabled.value && !LockInVpnManager.isVpnRunning.value) {
                LockInVpnManager.startVpn(context)
            }
        } catch (_: Exception) {}

        Log.i(TAG, "Banking pause cleared. Full protection active.")
    }

    /**
     * Checks if Caixa Econômica Federal (main app or Caixa Tem) is installed on device.
     */
    fun isCaixaInstalled(context: Context): Boolean {
        val pm = context.packageManager
        val caixaPackages = listOf(
            "br.com.gabba.Caixa",
            "br.com.gabba.caixa",
            "br.gov.caixa.tem",
            "br.gov.caixa.atendimentocidadao",
            "br.gov.caixa.fgts.trabalhador"
        )
        for (pkg in caixaPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Checks if USB Debugging / Developer Options are enabled, which causes Caixa
     * to immediately block user logins on Android.
     */
    fun isUsbDebuggingEnabled(context: Context): Boolean {
        return try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns friendly name if known, or null.
     */
    fun getFriendlyBankName(packageName: String?): String? {
        if (packageName == null) return null
        val lower = packageName.lowercase()
        return when {
            lower.contains("caixa") || lower.contains("gabba") -> "Caixa Econômica Federal"
            lower.contains("nu.production") || lower.contains("nubank") -> "Nubank"
            lower.contains("itau") -> "Banco Itaú"
            lower.contains("bradesco") -> "Banco Bradesco"
            lower.contains("bb.android") || lower.contains("bancodobrasil") -> "Banco do Brasil"
            lower.contains("santander") -> "Santander"
            lower.contains("intermedium") || lower.contains("bancointer") -> "Banco Inter"
            lower.contains("c6bank") -> "C6 Bank"
            lower.contains("mercadopago") -> "Mercado Pago"
            lower.contains("picpay") -> "PicPay"
            lower.contains("pagseguro") || lower.contains("uol.ps") || lower.contains("pagbank") -> "PagBank"
            lower.contains("neon") -> "Neon"
            lower.contains("btg") -> "BTG Pactual"
            lower.contains("xp.investimentos") || lower.contains("rico") || lower.contains("clear") -> "XP / Rico"
            lower.contains("sicoob") -> "Sicoob"
            lower.contains("sicredi") -> "Sicredi"
            lower.contains("banrisul") -> "Banrisul"
            lower.contains("safra") -> "Safra"
            lower.contains("bancopan") -> "Banco Pan"
            lower.contains("daycoval") -> "Banco Daycoval"
            lower.contains("agibank") -> "Agibank"
            lower.contains("willbank") -> "Will Bank"
            else -> "Aplicativo Bancário"
        }
    }
}
