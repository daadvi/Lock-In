package com.example.service

import com.example.LockInApplication
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Certifica o fechamento do bypass por DNS-over-HTTPS: com alguma regra de bloqueio
 * ativa, os domínios dos provedores de DoH também são sinkholados — o navegador cai
 * no DNS do sistema, que passa pelo túnel. Sem nenhuma regra ativa, o DoH do usuário
 * permanece intacto (nada é bloqueado sem motivo).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DohBypassBlockTest {

    private val app: LockInApplication get() = LockInApplication.instance

    @After
    fun resetState() {
        setFlow(PlayStoreBlacklistManager, "_blacklistedApps", emptyList<String>())
        setFlow(WebsiteBlacklistManager, "_blacklistedWebsites", emptyList<String>())
        LockInVpnManager.clearAllDepletedApps(app)
        app.getSharedPreferences("lock_in_website_blacklist", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("lock_in_playstore_blacklist", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setFlow(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        (field.get(target) as MutableStateFlow<T>).value = value
    }

    @Test
    fun dohProviderIsNotBlockedWhenNoRuleIsActive() {
        assertFalse(
            "sem nenhuma regra ativa o DoH do usuário fica intacto",
            LockInVpnService.isDomainBlockedForNetwork("dns.google")
        )
    }

    @Test
    fun dohProvidersAreBlockedWhenASiteRuleIsActive() {
        assertTrue(WebsiteBlacklistManager.addWebsite(app, "reddit.com"))

        val dohDomains = listOf(
            "dns.google", "dns.google.dns", "chrome.cloudflare-dns.com",
            "mozilla.cloudflare-dns.com", "cloudflare-dns.com", "doh.opendns.com",
            "doh.cleanbrowsing.org", "doh.dns.apple.com", "dns.quad9.net",
            "dns.nextdns.io", "dns.adguard.com", "doh.mullvad.net", "dns.switch.ch",
            "dns.controld.com", "doh.libredns.gr", "dns.alidns.com", "doh.pub"
        )
        for (domain in dohDomains) {
            assertTrue("$domain deve ser sinkholado", LockInVpnService.isDomainBlockedForNetwork(domain))
        }
    }

    @Test
    fun dohProvidersAreBlockedWhenAnAppRuleIsActive() {
        assertTrue(PlayStoreBlacklistManager.addApp(app, "Instagram"))
        assertTrue(LockInVpnService.isDomainBlockedForNetwork("dns.google"))
    }

    @Test
    fun dohProvidersAreBlockedWhenAnAppIsDepleted() {
        LockInVpnManager.registerDepletedApp(app, "com.instagram.android")
        assertTrue(LockInVpnService.isDomainBlockedForNetwork("dns.google"))
    }

    @Test
    fun dohBlockingDoesNotHitUnrelatedDomains() {
        assertTrue(WebsiteBlacklistManager.addWebsite(app, "reddit.com"))

        for (domain in listOf(
            "google.com", "youtube.com", "github.com", "wikipedia.org",
            "dns.google.company", "mydns.google", "notquad9.net"
        )) {
            assertFalse("$domain não deve ser bloqueado", LockInVpnService.isDomainBlockedForNetwork(domain))
        }
    }
}