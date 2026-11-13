package com.example.service

import com.example.LockInApplication
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Certifica a regra: app na blacklist de instalação => site oficial bloqueado
 * em TODAS as camadas (lista de sites, checagem de URL e sinkhole DNS).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstallBlocklistWebsiteBlockTest {

    private val app: LockInApplication get() = LockInApplication.instance

    @After
    fun resetState() {
        setFlow(PlayStoreBlacklistManager, "_blacklistedApps", emptyList<String>())
        setFlow(PlayStoreBlacklistManager, "_isBlacklistEnabled", true)
        setFlow(WebsiteBlacklistManager, "_blacklistedWebsites", emptyList<String>())
        setFlow(WebsiteBlacklistManager, "_isBlacklistEnabled", true)
        app.getSharedPreferences("lock_in_playstore_blacklist", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("lock_in_website_blacklist", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setFlow(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        (field.get(target) as MutableStateFlow<T>).value = value
    }

    @Test
    fun addingAppToInstallBlacklistAlsoBlocksItsWebsiteEverywhere() {
        assertTrue(PlayStoreBlacklistManager.addApp(app, "Reddit"))

        val sites = WebsiteBlacklistManager.blacklistedWebsites.value
        assertTrue("reddit.com deve entrar na lista de sites", sites.contains("reddit.com"))

        assertNotNull(
            "site deve bloquear pela lista de sites",
            WebsiteBlacklistManager.isWebsiteBlacklisted("https://www.reddit.com/r/pics")
        )
        assertNotNull(
            "site deve bloquear pela blacklist de instalação",
            PlayStoreBlacklistManager.isWebsiteBlacklistedByInstallationBlacklist("reddit.com")
        )
    }

    @Test
    fun aliasesResolveToOfficialDomains() {
        val cases = listOf(
            "TikTok" to listOf("tiktok.com"),
            "Kwai" to listOf("kwai.com"),
            "Instagram" to listOf("instagram.com"),
            "Insta" to listOf("instagram.com"),
            "Twitter" to listOf("x.com", "twitter.com"),
            "X" to listOf("x.com", "twitter.com"),
            "Netflix" to listOf("netflix.com"),
            "ZapZap" to listOf("zapzap.com")
        )
        for ((entry, expectedDomains) in cases) {
            resetState()
            assertTrue(PlayStoreBlacklistManager.addApp(app, entry))
            val sites = WebsiteBlacklistManager.blacklistedWebsites.value
            for (domain in expectedDomains) {
                assertTrue("'$entry' deve bloquear $domain (lista: $sites)", sites.contains(domain))
            }
        }
    }

    @Test
    fun installBlacklistUrlCheckCoversUrlsSubdomainsAndShorteners() {
        PlayStoreBlacklistManager.addApp(app, "Reddit")

        val blocked = listOf(
            "reddit.com",
            "https://reddit.com/",
            "www.reddit.com",
            "m.reddit.com",
            "old.reddit.com",
            "redd.it",
            "https://redd.it/s/abc123",
            "REDDIT.COM"
        )
        for (url in blocked) {
            assertNotNull("URL deve ser bloqueada: $url", PlayStoreBlacklistManager.isWebsiteBlacklistedByInstallationBlacklist(url))
        }

        val allowed = listOf("google.com", "youtube.com", "github.com", "wikipedia.org")
        for (url in allowed) {
            assertNull("URL não deve ser bloqueada: $url", PlayStoreBlacklistManager.isWebsiteBlacklistedByInstallationBlacklist(url))
        }
    }

    @Test
    fun dnsLayerBlocksInstallBlacklistedDomainsForAnyApp() {
        PlayStoreBlacklistManager.addApp(app, "Reddit")

        assertTrue("DNS deve bloquear reddit.com", LockInVpnService.isDomainBlockedForNetwork("reddit.com"))
        assertTrue("DNS deve bloquear subdomínio", LockInVpnService.isDomainBlockedForNetwork("old.reddit.com"))
        assertTrue("DNS deve bloquear encurtador", LockInVpnService.isDomainBlockedForNetwork("redd.it"))
        assertFalse("DNS não deve bloquear domínio livre", LockInVpnService.isDomainBlockedForNetwork("google.com"))

        // Camada de apps esgotados (limite zerado) continua funcionando pelo DNS
        LockInVpnManager.registerDepletedApp(app, "com.instagram.android")
        assertTrue(LockInVpnService.isDomainBlockedForNetwork("old.instagram.com"))
        LockInVpnManager.clearDepletedApp(app, "com.instagram.android")
        assertFalse(LockInVpnService.isDomainBlockedForNetwork("old.instagram.com"))
    }

    @Test
    fun syncIsIdempotentAndRunsOnEveryWrite() {
        assertTrue(PlayStoreBlacklistManager.addApp(app, "Reddit"))
        assertFalse("segunda adição não duplica", PlayStoreBlacklistManager.addApp(app, "Reddit"))
        assertEquals(1, WebsiteBlacklistManager.blacklistedWebsites.value.count { it == "reddit.com" })

        // setBlacklist cobre escrita de restore de backup
        PlayStoreBlacklistManager.setBlacklist(app, listOf("Netflix"), true)
        assertTrue(WebsiteBlacklistManager.blacklistedWebsites.value.contains("reddit.com"))
        assertTrue(WebsiteBlacklistManager.blacklistedWebsites.value.contains("netflix.com"))
    }

    @Test
    fun syncForcesWebsiteBlockEnabled() {
        WebsiteBlacklistManager.setBlacklistEnabled(app, false)
        assertFalse(WebsiteBlacklistManager.isBlacklistEnabled.value)

        PlayStoreBlacklistManager.addApp(app, "Reddit")

        assertTrue("sincronização reativa a lista de sites", WebsiteBlacklistManager.isBlacklistEnabled.value)
        assertNotNull(WebsiteBlacklistManager.isWebsiteBlacklisted("reddit.com"))
    }

    @Test
    fun disabledInstallBlacklistDoesNotBlockSites() {
        PlayStoreBlacklistManager.setBlacklistEnabled(app, false)
        PlayStoreBlacklistManager.addApp(app, "Hulu")

        assertFalse(WebsiteBlacklistManager.blacklistedWebsites.value.contains("hulu.com"))
    }

    @Test
    fun existingEntriesAreSyncedOnStartup() {
        // Simula blacklists já existentes antes desta versão (catch-up no boot)
        setFlow(PlayStoreBlacklistManager, "_blacklistedApps", listOf("Reddit", "Kwai"))
        setFlow(WebsiteBlacklistManager, "_blacklistedWebsites", emptyList<String>())

        PlayStoreBlacklistManager.syncBlockedAppWebsites(app)

        val sites = WebsiteBlacklistManager.blacklistedWebsites.value
        assertTrue(sites.contains("reddit.com"))
        assertTrue(sites.contains("kwai.com"))
    }
}
