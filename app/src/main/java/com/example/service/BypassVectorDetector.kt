package com.example.service

/**
 * Detecta apps que sao alternativas para contornar o bloqueio do Lock In
 * (navegadores, clientes de VPN/DNS proprio, clonadores de app).
 *
 * A decisao e puramente textual para poder ser testada sem Android.
 */
object BypassVectorDetector {

    private val KEYWORDS = listOf(
        "vpn", "proxy", "dns", "clash", "v2ray", "shadowsocks", "wireguard",
        "openvpn", "psiphon", "tunnel", "tor", "clone", "parallel", "dual",
        "multiapp", "appcloner", "shelter", "island",
        // Clientes de DNS/VPN conhecidos que nao trazem a palavra no nome do pacote.
        "onedot", "cloudflare", "nextdns", "adguard", "intra", "warp"
    )

    /** Nomes de pacote do sistema que contem palavras-chave mas nao sao vetores de burla. */
    private val ALLOWED = listOf(
        "com.android.providers", "android", "com.google.android.gms"
    )

    internal fun isBypassVector(packageName: String?, isBrowser: Boolean = false): Boolean {
        val pkg = packageName?.trim()?.lowercase() ?: return false
        if (pkg.isEmpty()) return false
        if (ALLOWED.any { pkg == it || pkg.startsWith("$it.") }) return false
        if (isBrowser) return true
        return KEYWORDS.any { pkg.contains(it) }
    }
}
