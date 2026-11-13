package com.example.service

import com.example.LockInApplication
import com.example.data.local.RestrictedAppEntity
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Certifies that Kwai receives EXACTLY the same treatment as TikTok everywhere the app
 * special-cases short-video platforms: video-mode detection, app↔website mapping,
 * installation blacklist (aliases, typos and known packages) and website suggestions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TikTokKwaiParityTest {

    private val tiktokPackages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.tiktok.android")
    private val kwaiPackages = listOf("com.kwai.video", "com.kuaishou.nebula", "com.kwai.beta")

    @Suppress("UNCHECKED_CAST")
    private fun setBlacklistEntries(entries: List<String>) {
        val field = PlayStoreBlacklistManager::class.java.getDeclaredField("_blacklistedApps").apply { isAccessible = true }
        (field.get(PlayStoreBlacklistManager) as MutableStateFlow<List<String>>).value = entries
    }

    @Test
    fun videoCategoryDetectionMatchesTikTokExactly() {
        for (tiktokPkg in tiktokPackages) {
            assertTrue("TikTok should have short videos: $tiktokPkg", AppVideoCategoryHelper.hasShortVideos(tiktokPkg))
            assertTrue("TikTok should be short-only: $tiktokPkg", AppVideoCategoryHelper.hasShortVideosOnly(tiktokPkg))
        }
        kwaiPackages.forEach { pkg ->
            val mirror = tiktokPackages.first()
            assertEquals("hasShortVideos differs for $pkg", AppVideoCategoryHelper.hasShortVideos(mirror), AppVideoCategoryHelper.hasShortVideos(pkg))
            assertEquals("hasShortVideosOnly differs for $pkg", AppVideoCategoryHelper.hasShortVideosOnly(mirror), AppVideoCategoryHelper.hasShortVideosOnly(pkg))
            assertEquals("hasVideos differs for $pkg", AppVideoCategoryHelper.hasVideos(mirror), AppVideoCategoryHelper.hasVideos(pkg))
            assertTrue("Kwai must be detected as a short-video platform: $pkg", AppVideoCategoryHelper.hasShortVideos(pkg))
            assertTrue("Kwai must be short-only like TikTok: $pkg", AppVideoCategoryHelper.hasShortVideosOnly(pkg))
        }
        // Domain-based detection parity (Kwai web = kwai.com, TikTok web = tiktok.com)
        assertTrue(AppVideoCategoryHelper.hasShortVideosForDomain("tiktok.com"))
        assertTrue(AppVideoCategoryHelper.hasShortVideosForDomain("kwai.com"))
        assertTrue(AppVideoCategoryHelper.hasShortVideosForDomain("www.kwai.com"))
        assertTrue(AppVideoCategoryHelper.hasVideosForDomain("kwai.com"))
    }

    @Test
    fun websiteMappingMatchesTikTok() {
        val mapper = com.example.util.AppWebsiteMapper
        // Kwai gets the same app↔website mirror TikTok has (domain inherits app limit)
        assertEquals(listOf("kwai.com"), mapper.getWebsitesForApp("com.kwai.video", "Kwai"))
        assertEquals(listOf("kwai.com"), mapper.getWebsitesForApp("com.kuaishou.nebula", "Kwai"))

        // Reverse lookup: visiting kwai.com maps back to the Kwai app entity (same limit inheritance)
        val kwaiEntity = RestrictedAppEntity(packageName = "com.kwai.video", appName = "Kwai")
        val tiktokEntity = RestrictedAppEntity(packageName = "com.zhiliaoapp.musically", appName = "TikTok")
        assertEquals(kwaiEntity, mapper.findAppForDomain(listOf(kwaiEntity), "kwai.com"))
        assertEquals(kwaiEntity, mapper.findAppForDomain(listOf(kwaiEntity), "https://www.kwai.com/video"))
        assertEquals(tiktokEntity, mapper.findAppForDomain(listOf(tiktokEntity), "tiktok.com"))
        assertNull("kwai.com must not map to a TikTok-only entity", mapper.findAppForDomain(listOf(tiktokEntity), "kwai.com"))
    }

    @Test
    fun blacklistTreatsKwaiLikeTikTok() {
        val app = LockInApplication.instance
        try {
            setBlacklistEntries(emptyList())
            PlayStoreBlacklistManager.addApp(app, "Kwai")

            // Same text-matching behaviors TikTok relies on
            assertNotNull("Kwai must be blacklisted by name", PlayStoreBlacklistManager.isAppBlacklisted("Kwai"))
            assertNotNull("Store title match", PlayStoreBlacklistManager.isAppBlacklisted("Kwai - Vídeos Curtos Engraçados"))
            assertNotNull("Fuzzy alias match (quai)", PlayStoreBlacklistManager.isAppBlacklisted("quai"))
            assertNotNull("Canonical package label match", PlayStoreBlacklistManager.isAppBlacklisted("Kwai Lite"))

            // Known packages of the canonical entry
            assertNotNull("com.kwai.video must match the Kwai entry", PlayStoreBlacklistManager.isPackageBlacklisted("com.kwai.video"))
            assertNotNull("com.kuaishou.nebula must match the Kwai entry", PlayStoreBlacklistManager.isPackageBlacklisted("com.kuaishou.nebula"))

            // No cross-matching with other apps / no false positives
            assertNull("systemui is never blacklisted", PlayStoreBlacklistManager.isPackageBlacklisted("com.android.systemui"))
            setBlacklistEntries(listOf("Kwai"))
            assertNull("Kwai entry must not catch Instagram", PlayStoreBlacklistManager.isPackageBlacklisted("com.instagram.android"))
            assertNull("Kwai entry must not catch YouTube", PlayStoreBlacklistManager.isPackageBlacklisted("com.google.android.youtube"))

            // TikTok keeps its own independent mappings
            setBlacklistEntries(listOf("TikTok"))
            assertNotNull("TikTok package still matches", PlayStoreBlacklistManager.isPackageBlacklisted("com.zhiliaoapp.musically"))
            assertNotNull(PlayStoreBlacklistManager.isAppBlacklisted("tik tok"))
        } finally {
            setBlacklistEntries(emptyList())
        }
    }

    @Test
    fun websiteSuggestionsIncludeKwaiLikeTikTok() {
        assertTrue(WebsiteBlacklistManager.POPULAR_SUGGESTIONS.contains("tiktok.com"))
        assertTrue("kwai.com must be offered like tiktok.com", WebsiteBlacklistManager.POPULAR_SUGGESTIONS.contains("kwai.com"))
    }
}
