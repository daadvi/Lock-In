package com.example

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.service.LockInAccessibilityService
import com.example.util.AndroidPermissionAccess
import com.example.util.AppPermission
import com.example.util.PermissionStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionAccessTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun accessibilityRequiresThisAppsExactEnabledService() {
        val resolver = context.contentResolver
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "other.package/com.example.service.LockInAccessibilityService")
        assertFalse(AndroidPermissionAccess.isAccessibilityEnabled(context))
        val component = ComponentName(context, LockInAccessibilityService::class.java)
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "other.package/.Reader:${component.flattenToShortString()}")
        assertTrue(AndroidPermissionAccess.isAccessibilityEnabled(context))
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        assertFalse(AndroidPermissionAccess.isAccessibilityEnabled(context))
    }

    @Test fun deniedNotificationPermissionIsNeverDisplayedAsGranted() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(PermissionStatus.PENDING, AndroidPermissionAccess(context).read().statuses[AppPermission.NOTIFICATIONS])
    }

    @Test fun unsupportedDirectSettingsFallBackWithoutReportingPermissionAsGranted() {
        val attempts = mutableListOf<Intent>()
        val wrapped = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                attempts.add(intent)
                if (attempts.size == 1) throw ActivityNotFoundException()
            }
        }
        val access = AndroidPermissionAccess(wrapped)
        assertTrue(access.open(AppPermission.ACCESSIBILITY))
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, attempts.last().action)
        assertEquals(ComponentName(context, LockInAccessibilityService::class.java).flattenToString(),
            attempts.first().getStringExtra(Intent.EXTRA_COMPONENT_NAME))
        assertTrue(attempts.last().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertFalse(access.read().isGranted(AppPermission.ACCESSIBILITY))
    }

    @Test fun navigationFailureIsReportedAndAppDetailsTargetsLockIn() {
        val attempts = mutableListOf<Intent>()
        val wrapped = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                attempts.add(intent)
                throw SecurityException("Unavailable settings activity")
            }
        }
        assertFalse(AndroidPermissionAccess(wrapped).open(AppPermission.OVERLAY))
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, attempts.last().action)
        assertEquals("package:${context.packageName}", attempts.last().dataString)
        attempts.clear()
        assertFalse(AndroidPermissionAccess(wrapped).openAppDetails())
        assertEquals("package:${context.packageName}", attempts.first().dataString)
    }

    @Test fun batterySetupUsesTheSystemOptimizationList() {
        var opened: Intent? = null
        val wrapped = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { opened = intent }
        }
        assertTrue(AndroidPermissionAccess(wrapped).open(AppPermission.BATTERY))
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, opened?.action)
    }
}
