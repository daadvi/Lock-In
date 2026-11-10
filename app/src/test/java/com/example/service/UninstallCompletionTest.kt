package com.example.service

import android.app.Activity
import android.content.Intent
import android.os.Looper
import com.example.LockInApplication
import com.example.UninstallProtectionActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UninstallCompletionTest {
    private val app get() = LockInApplication.instance
    private fun verifiedIntent() = Intent(app, UninstallProtectionActivity::class.java)
        .putExtra(UninstallProtectionActivity.EXTRA_CHALLENGE_MODE, UninstallProtectionActivity.MODE_UNINSTALL_APP)
        .putExtra(UninstallProtectionActivity.EXTRA_VERIFIED_OVERLAY, true)

    @Test fun verifiedOverlayOpensSystemRemovalAndCancellationRevokesAuthorization() {
        DeviceAdminManager.setUninstallAuthorized(app, true)
        Robolectric.buildActivity(UninstallProtectionActivity::class.java, verifiedIntent()).use { controller ->
            controller.setup().visible()
            shadowOf(Looper.getMainLooper()).idle()
            val activity = controller.get()
            val request = shadowOf(activity).nextStartedActivityForResult
            assertNotNull(request)
            assertEquals(Intent.ACTION_UNINSTALL_PACKAGE, request.intent.action)
            assertEquals("package:${app.packageName}", request.intent.dataString)
            assertTrue(DeviceAdminManager.isUninstallAuthorized(app))
            shadowOf(activity).receiveResult(request.intent, Activity.RESULT_CANCELED, Intent())
            assertFalse(DeviceAdminManager.isUninstallAuthorized(app))
            assertTrue(activity.isFinishing)
        }
    }
    @Test fun intentFlagWithoutCompletedChallengeCannotStartSystemRemoval() {
        DeviceAdminManager.setUninstallAuthorized(app, false)
        Robolectric.buildActivity(UninstallProtectionActivity::class.java, verifiedIntent()).use { controller ->
            controller.setup().visible()
            shadowOf(Looper.getMainLooper()).idle()
            assertNull(shadowOf(controller.get()).nextStartedActivityForResult)
            assertFalse(DeviceAdminManager.isUninstallAuthorized(app))
        }
    }
}
