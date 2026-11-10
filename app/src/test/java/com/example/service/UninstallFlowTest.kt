package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.LockInApplication
import com.example.UninstallProtectionActivity
import com.example.BlockedActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UninstallFlowTest {
    private val app get() = LockInApplication.instance
    private val services = mutableListOf<LockInAccessibilityService>()
    @org.junit.After fun cleanup() { services.forEach { it.onDestroy() } }
    private fun overlayViews(service: LockInAccessibilityService): List<android.view.View> =
        org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowWindowManagerImpl>(service.getSystemService(android.view.WindowManager::class.java)).views
    private fun detect(target: String, pkg: String = "com.android.packageinstaller", action: String = "Desinstalar"): Pair<Boolean, LockInAccessibilityService> {
        val service = Robolectric.buildService(LockInAccessibilityService::class.java).create().get().also { services.add(it) }
        val root = AccessibilityNodeInfo.obtain().apply { packageName = pkg }
        for (text in listOf(target, action)) {
            shadowOf(root).addChild(AccessibilityNodeInfo.obtain().apply { this.text = text })
        }
        AccessibilityNodeInfo::class.java.getDeclaredMethod("setSealed", Boolean::class.javaPrimitiveType)
            .invoke(root, true)
        shadowOf(service).setRootInActiveWindow(root)
        val budget = service.javaClass.getDeclaredField("scanBudget").apply { isAccessible = true }
        budget.set(service, NodeScanBudget(120, Long.MAX_VALUE, { 0 }).apply { reset() })
        val method = service.javaClass.getDeclaredMethod("checkUninstallAttempt", String::class.java, Long::class.javaPrimitiveType)
            .apply { isAccessible = true }
        return (method.invoke(service, pkg, System.currentTimeMillis()) as Boolean) to service
    }
    @Test fun uninstallDialogLaunchesChallengeWithoutGoingHome() {
        val (detected, service) = detect("Lock In")
        assertTrue(detected)
        assertFalse(shadowOf(service).globalActionsPerformed.contains(AccessibilityService.GLOBAL_ACTION_HOME))
        val window = overlayViews(service).single()
        assertEquals(android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            (window.layoutParams as android.view.WindowManager.LayoutParams).type)
        assertNull(shadowOf(app).nextStartedActivity) // No background activity launch needed to show the challenge.
        service.onDestroy()
        assertTrue(overlayViews(service).isEmpty())
    }
    @Test fun anotherAppsUninstallDoesNotLaunchChallenge() {
        val (detected, service) = detect("WhatsApp")
        assertFalse(detected)
        assertTrue(overlayViews(service).isEmpty())
        assertNull(shadowOf(app).nextStartedActivity)
    }
    @Test fun launcherOwnedConfirmationShowsTheChallenge() {
        val (detected, service) = detect("Lock In", "com.miui.home", "Desinstalar Lock In?")
        assertTrue(detected)
        assertEquals(1, overlayViews(service).size)
    }
    @Test fun launcherIconsAndGenericUninstallShortcutDoNotTriggerChallenge() {
        val (detected, service) = detect("Lock In", "com.miui.home")
        assertFalse(detected)
        assertTrue(overlayViews(service).isEmpty())
    }
    @Test fun earlierRemovalOfAnotherAppDoesNotDisableLockInProtection() {
        LockInAccessibilityService.allowTemporaryUninstall("com.other.app")
        assertTrue(detect("Lock In").first)
    }
    @Test fun justOpeningAppDetailsDoesNotStartAnUninstallChallenge() {
        assertFalse(detect("Lock In", "com.android.settings").first)
    }
    @Test fun clickingUninstallInAppDetailsBypassesGenericDebounce() {
        val (initial, service) = detect("Lock In", "com.android.settings")
        assertFalse(initial)
        service.javaClass.getDeclaredField("lastContentChangeProcessedEpoch").apply { isAccessible = true }
            .setLong(service, System.currentTimeMillis())
        val event = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED).apply {
            packageName = "com.android.settings"
            contentDescription = "Desinstalar"
        }
        service.onAccessibilityEvent(event)
        event.recycle()
        assertEquals(1, overlayViews(service).size)
    }
    @Test fun authorizationExpiresAndOldPermanentAuthorizationIsIgnored() {
        val prefs = app.getSharedPreferences("lock_in_device_admin_prefs", 0)
        prefs.edit().putBoolean("key_uninstall_authorized", true).apply()
        assertFalse(DeviceAdminManager.isUninstallAuthorized(app))
        DeviceAdminManager.setUninstallAuthorized(app, true)
        assertTrue(DeviceAdminManager.isUninstallAuthorized(app))
        prefs.edit().putLong("authorized_until", System.currentTimeMillis() - 1).apply()
        assertFalse(DeviceAdminManager.isUninstallAuthorized(app))
        DeviceAdminManager.setUninstallAuthorized(app, false)
        assertFalse(DeviceAdminManager.isUninstallAuthorized(app))
    }
    @Test fun systemRemovalTargetsOnlyLockInAndReturnsCancellationResult() {
        val intent = DeviceAdminManager.systemUninstallIntent(app)
        assertEquals(Intent.ACTION_UNINSTALL_PACKAGE, intent.action)
        assertEquals("package:${app.packageName}", intent.dataString)
        assertTrue(intent.getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false))
    }
    @Test fun depletedAppLaunchesExistingBlockScreenImmediately() {
        DeviceAdminManager.setUninstallAuthorized(app, false)
        AppBlockEnforcer.triggerBlockScreen(app, "test.depleted", force = true)
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(BlockedActivity::class.java.name, intent.component?.className)
        assertTrue(intent.getBooleanExtra(BlockedActivity.EXTRA_IS_DEPLETED_LIMIT, false))
        assertEquals("test.depleted", intent.getStringExtra(BlockedActivity.EXTRA_PACKAGE_NAME))
    }
}
