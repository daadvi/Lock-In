package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferencesRepository
import com.example.service.AppVideoCategoryHelper
import com.example.service.DeviceAdminManager
import com.example.service.EmergencyUnlockManager
import com.example.service.LockInVpnManager
import com.example.service.PlayStoreBlacklistManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lock in", appName)
  }

  @Test
  fun `user preferences registration and trust password`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = UserPreferencesRepository(context)
    repo.setRegistration("Foco Total", "minhasenhaforte123")

    assertTrue(repo.isRegistered.value)
    assertTrue(repo.verifyTrustPassword("minhasenhaforte123"))
    assertFalse(repo.verifyTrustPassword("senhaincorreta"))
    assertTrue(repo.getTrustPassword().startsWith("v1:salt:"))
    assertEquals(20, repo.sharedLimitMinutes.value)
  }

  @Test
  fun `video category helper detects short videos only apps`() {
    // Instagram, TikTok, Kwai have only short-form feeds
    assertTrue(AppVideoCategoryHelper.hasShortVideosOnly("com.instagram.android"))
    assertTrue(AppVideoCategoryHelper.hasShortVideosOnly("com.zhiliaoapp.musically"))
    assertTrue(AppVideoCategoryHelper.hasShortVideosOnly("com.kwai.video"))

    assertFalse(AppVideoCategoryHelper.hasBothShortAndLongVideos("com.instagram.android"))
    assertFalse(AppVideoCategoryHelper.hasLongVideosOnly("com.instagram.android"))
  }

  @Test
  fun `video category helper detects both short and long video apps`() {
    // YouTube and Facebook have both
    assertTrue(AppVideoCategoryHelper.hasBothShortAndLongVideos("com.google.android.youtube"))
    assertTrue(AppVideoCategoryHelper.hasBothShortAndLongVideos("com.facebook.katana"))

    assertFalse(AppVideoCategoryHelper.hasShortVideosOnly("com.google.android.youtube"))
    assertFalse(AppVideoCategoryHelper.hasLongVideosOnly("com.google.android.youtube"))
  }

  @Test
  fun `video category helper detects long videos only apps`() {
    // Netflix, Disney+, Twitch, Prime Video
    assertTrue(AppVideoCategoryHelper.hasLongVideosOnly("com.netflix.mediaclient"))
    assertTrue(AppVideoCategoryHelper.hasLongVideosOnly("com.disney.disneyplus"))
    assertTrue(AppVideoCategoryHelper.hasLongVideosOnly("tv.twitch.android.app"))

    assertFalse(AppVideoCategoryHelper.hasShortVideos("com.netflix.mediaclient"))
    assertFalse(AppVideoCategoryHelper.hasShortVideosOnly("com.netflix.mediaclient"))
    assertFalse(AppVideoCategoryHelper.hasBothShortAndLongVideos("com.netflix.mediaclient"))
  }

  @Test
  fun `video category helper excludes utility apps`() {
    assertFalse(AppVideoCategoryHelper.hasVideos("com.google.android.calculator"))
    assertFalse(AppVideoCategoryHelper.hasVideos("com.android.settings"))
    assertFalse(AppVideoCategoryHelper.hasVideos("com.google.android.deskclock"))
  }

  @Test
  fun `play store blacklist matches prohibited apps accurately without false positives`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    PlayStoreBlacklistManager.initialize(context)
    PlayStoreBlacklistManager.setBlacklist(context, listOf("Instagram", "TikTok", "Free Fire"), enabled = true)

    // Exact matches & aliases
    assertNotNull(PlayStoreBlacklistManager.isAppBlacklisted("Instagram"))
    assertNotNull(PlayStoreBlacklistManager.isAppBlacklisted("Instagram da Meta"))
    assertNotNull(PlayStoreBlacklistManager.isAppBlacklisted("TikTok"))
    assertNotNull(PlayStoreBlacklistManager.isAppBlacklisted("Free Fire"))
    assertNotNull(PlayStoreBlacklistManager.isPackageBlacklisted("com.instagram.android"))
    assertNotNull(PlayStoreBlacklistManager.isPackageBlacklisted("com.zhiliaoapp.musically"))

    // Crucial: NO false positives on system apps, launcher, settings or Lock in
    assertNull(PlayStoreBlacklistManager.isAppBlacklisted("Lock in"))
    assertNull(PlayStoreBlacklistManager.isAppBlacklisted("Configurações"))
    assertNull(PlayStoreBlacklistManager.isAppBlacklisted("Início"))
    assertNull(PlayStoreBlacklistManager.isAppBlacklisted("Android"))
    assertNull(PlayStoreBlacklistManager.isPackageBlacklisted("com.android.systemui"))
    assertNull(PlayStoreBlacklistManager.isPackageBlacklisted("com.google.android.apps.nexuslauncher"))
  }

  @Test
  fun `device admin uninstall authorization state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    DeviceAdminManager.setUninstallAuthorized(context, false)
    assertFalse(DeviceAdminManager.isUninstallAuthorized(context))

    DeviceAdminManager.setUninstallAuthorized(context, true)
    assertTrue(DeviceAdminManager.isUninstallAuthorized(context))
  }

  @Test
  fun `play store blacklist installed package resolver`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    PlayStoreBlacklistManager.initialize(context)
    PlayStoreBlacklistManager.setBlacklist(context, listOf("TikTok", "Instagram"), enabled = true)

    val keys = PlayStoreBlacklistManager.getCanonicalKeysFor("TikTok")
    assertTrue(keys.contains("tiktok"))

    val igKeys = PlayStoreBlacklistManager.getCanonicalKeysFor("Instagram")
    assertTrue(igKeys.contains("instagram"))
  }

  @Test
  fun `high friction security challenge phrases valid`() {
    val unblockPhrases = listOf(
      "Eu quero instalar esse app e sei o que vou fazer com ele, mesmo sendo errado",
      "Quero instalar esse aplicativo, mesmo me desrespeitando",
      "Eu estou ciente do que vou fazer agora e aceito as consequências de instalar esse app"
    )
    assertTrue(unblockPhrases.contains("Eu quero instalar esse app e sei o que vou fazer com ele, mesmo sendo errado"))
    assertTrue(unblockPhrases.contains("Quero instalar esse aplicativo, mesmo me desrespeitando"))
    assertTrue(unblockPhrases.contains("Eu estou ciente do que vou fazer agora e aceito as consequências de instalar esse app"))

    val uninstallPhrases = listOf(
      "Eu quero desinstalar o Lock In e sei o que vou perder com isso, mesmo sendo errado",
      "Quero desinstalar o aplicativo, mesmo me desrespeitando",
      "Eu estou ciente do que vou fazer agora e aceito as consequências de desinstalar o Lock In"
    )
    assertTrue(uninstallPhrases.contains("Eu quero desinstalar o Lock In e sei o que vou perder com isso, mesmo sendo errado"))

    val adminPhrases = listOf(
      "Eu quero desativar o administrador e sei o perigo de perder minha proteção, mesmo sendo errado",
      "Quero retirar o Lock In dos administradores, mesmo me desrespeitando",
      "Eu estou ciente do que vou fazer agora e aceito as consequências de desativar o administrador"
    )
    assertTrue(adminPhrases.contains("Eu quero desativar o administrador e sei o perigo de perder minha proteção, mesmo sendo errado"))
  }

  @Test
  fun `installation of non-blacklisted apps is allowed freely while blacklisted apps are caught`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    PlayStoreBlacklistManager.initialize(context)
    PlayStoreBlacklistManager.setBlacklist(context, listOf("Instagram", "TikTok"), enabled = true)

    // 1. Regular non-blacklisted installer window texts must NOT trigger blacklist
    val normalInstallerTexts = listOf(
      "Instalador de Pacotes",
      "Calculadora Simples",
      "Deseja instalar este app?",
      "Instalar",
      "Cancelar"
    )
    assertNull(PlayStoreBlacklistManager.findBlacklistMatchInTexts(normalInstallerTexts))

    // 2. Blacklisted app installer window texts MUST trigger blacklist
    val blacklistedInstallerTexts = listOf(
      "Instalador de Pacotes",
      "Instagram",
      "Deseja instalar este app?",
      "Instalar",
      "Cancelar"
    )
    val match = PlayStoreBlacklistManager.findBlacklistMatchInTexts(blacklistedInstallerTexts)
    assertNotNull(match)
    assertEquals("Instagram", match)

    // 3. Opening non-blacklisted app is NOT blocked
    assertNull(PlayStoreBlacklistManager.isAppOrPackageBlacklisted(context, "com.google.android.calculator"))

    // 4. Opening blacklisted app IS blocked
    val appBlockedMatch = PlayStoreBlacklistManager.isAppOrPackageBlacklisted(context, "com.instagram.android")
    assertNotNull(appBlockedMatch)
    assertEquals("Instagram", appBlockedMatch)
  }

  @Test
  fun `emergency unlock manager enforces max 2 monthly limit and prevents overflow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    EmergencyUnlockManager.checkAndResetMonthlyUsage(context)

    // Initially can use
    val initialRemaining = EmergencyUnlockManager.getRemainingUnlocksThisMonth(context)
    assertTrue(initialRemaining in 0..2)

    // Simulate record unlock used
    EmergencyUnlockManager.recordUnlockUsed(context)
    val usedAfterFirst = EmergencyUnlockManager.getUsedCountThisMonth(context)
    assertTrue(usedAfterFirst in 1..2)

    // Simulate 2nd usage
    EmergencyUnlockManager.recordUnlockUsed(context)
    val usedAfterSecond = EmergencyUnlockManager.getUsedCountThisMonth(context)
    assertEquals(2, usedAfterSecond)
    assertFalse(EmergencyUnlockManager.canUseEmergencyUnlock(context))

    // Attempt 3rd usage (overflow attempt) - MUST be safely rejected and clamped at 2
    EmergencyUnlockManager.recordUnlockUsed(context)
    val usedAfterOverflow = EmergencyUnlockManager.getUsedCountThisMonth(context)
    assertEquals(2, usedAfterOverflow)
    assertEquals(0, EmergencyUnlockManager.getRemainingUnlocksThisMonth(context))
  }

  @Test
  fun `vpn manager records blocked query counts and tracks state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    LockInVpnManager.initialize(context)

    val countBefore = LockInVpnManager.blockedDnsCount.value
    LockInVpnManager.recordBlockedQuery(context, "tiktok.com")
    val countAfter = LockInVpnManager.blockedDnsCount.value
    assertEquals(countBefore + 1, countAfter)

    LockInVpnManager.registerDepletedApp(context, "com.instagram.android")
    assertTrue(LockInVpnManager.depletedPackages.value.contains("com.instagram.android"))

    LockInVpnManager.clearDepletedApp(context, "com.instagram.android")
    assertFalse(LockInVpnManager.depletedPackages.value.contains("com.instagram.android"))
  }
}
