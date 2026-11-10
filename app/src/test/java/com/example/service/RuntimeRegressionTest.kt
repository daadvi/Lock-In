package com.example.service

import android.app.Application
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RuntimeRegressionTest {
    @Test fun accessibilityScanStopsBeforeDistantTextInWideEmptyTree() {
        val service = LockInAccessibilityService()
        val budgetField = service.javaClass.getDeclaredField("scanBudget").apply { isAccessible = true }
        val budget = NodeScanBudget(maxLookups = 120, maxMillis = Long.MAX_VALUE, clock = { 0 })
        budgetField.set(service, budget)
        val root = AccessibilityNodeInfo.obtain()
        repeat(1000) { index ->
            val child = AccessibilityNodeInfo.obtain()
            if (index == 0) child.text = "Primeiro"
            if (index == 999) child.text = "Fora do orçamento"
            shadowOf(root).addChild(child)
        }
        val collect = service.javaClass.getDeclaredMethod("collectWindowTexts", AccessibilityNodeInfo::class.java, MutableList::class.java, Int::class.javaPrimitiveType).apply { isAccessible = true }
        val texts = mutableListOf<String>()
        budget.reset()
        collect.invoke(service, root, texts, 0)
        assertEquals(listOf("primeiro"), texts)
        assertFalse(budget.tryAcquire())
    }

    @Test fun stoppedOverlayCancelsDemoAndAllChildJobs() {
        val service = FloatingIslandService()
        val scopeField = service.javaClass.getDeclaredField("scope").apply { isAccessible = true }
        val scope = scopeField.get(service) as kotlinx.coroutines.CoroutineScope
        service.onDestroy()
        assertFalse(scope.coroutineContext[kotlinx.coroutines.Job]!!.isActive)
    }

    @Test fun screenTimerCanBeDisabledAndEnabledAgain() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        ScreenTimeManager.init(context)
        ScreenTimeManager.setLimitMinutes(context, 5)
        val field = ScreenTimeManager.javaClass.getDeclaredField("tickerJob").apply { isAccessible = true }
        val first = field.get(ScreenTimeManager) as kotlinx.coroutines.Job
        ScreenTimeManager.setEnabled(context, false)
        assertFalse(first.isActive)
        ScreenTimeManager.setEnabled(context, true)
        assertTrue((field.get(ScreenTimeManager) as kotlinx.coroutines.Job).isActive)
        ScreenTimeManager.stopMonitoring()
    }

    @Test fun installedLookupDoesNotReturnAnotherBlacklistedApplication() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val manager = PlayStoreBlacklistManager
        manager.setBlacklist(context, listOf("Alpha Tool", "Beta Tool"), true)
        manager.javaClass.getDeclaredField("installedLabels").apply { isAccessible = true }
            .set(manager, mapOf("test.alpha" to "Alpha Tool"))
        manager.javaClass.getDeclaredField("installedLabelsUpdatedAt").apply { isAccessible = true }
            .setLong(manager, android.os.SystemClock.elapsedRealtime())
        assertNull(manager.findInstalledPackageForBlacklist(context, "Beta Tool"))
        assertEquals("test.alpha", manager.findInstalledPackageForBlacklist(context, "Alpha Tool"))
    }

    @Test fun largeDrawableIsRasterizedDirectlyAtRequestedIconSize() {
        var actualWidth = 0
        val drawable = object : android.graphics.drawable.Drawable() {
            override fun getIntrinsicWidth() = 10000
            override fun getIntrinsicHeight() = 10000
            override fun draw(canvas: android.graphics.Canvas) { actualWidth = canvas.width }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(filter: android.graphics.ColorFilter?) {}
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
        val helper = com.example.ui.components.AppIconHelper
        val method = helper.javaClass.getDeclaredMethod("drawableToBitmap", android.graphics.drawable.Drawable::class.java, Int::class.javaPrimitiveType).apply { isAccessible = true }
        val bitmap = method.invoke(helper, drawable, 64) as android.graphics.Bitmap
        assertEquals(64, actualWidth)
        assertEquals(64, bitmap.width)
        assertEquals(64, bitmap.height)
    }

    @Test fun repeatedHideRequestsDoNotStartBackgroundServices() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val application = shadowOf(context)
        repeat(10_000) { FloatingIslandService.hideIsland(context) }
        assertNull(application.nextStartedService)
    }
}
