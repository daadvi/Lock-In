package com.example

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.service.FocusTimeManager
import com.example.ui.screens.FocusTimeScreen
import com.example.ui.screens.FriendsPanelScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.LockInTheme
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowUsageStatsManager.UsageStatsBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class, com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
class ThemeScreensUiTest(private val theme: String) {
    @get:Rule val compose = createComposeRule()
    private val context get() = LockInApplication.instance

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = listOf(arrayOf("LIGHT"), arrayOf("DARK"))
    }

    @Before fun installSampleAppsAndUsage() {
        val pm = shadowOf(context.packageManager)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        listOf("com.whatsapp" to "WhatsApp", "com.google.android.apps.maps" to "Google Maps",
            "com.google.android.apps.messaging" to "Mensagens SMS").forEach { (pkg, label) ->
            val info = ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = label }
            pm.installPackage(PackageInfo().apply { packageName = pkg; applicationInfo = info })
            pm.addResolveInfoForIntent(launcher, ResolveInfo().apply {
                activityInfo = ActivityInfo().apply { packageName = pkg; name = "$pkg.MainActivity"; applicationInfo = info }
            })
        }
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        shadowOf(ops).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_ALLOWED)
        val now = System.currentTimeMillis()
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        shadowOf(usage).addUsageStats(UsageStatsManager.INTERVAL_DAILY, UsageStatsBuilder.newBuilder()
            .setPackageName("com.whatsapp").setFirstTimeStamp(now - 3_600_000)
            .setLastTimeStamp(now).setTotalTimeInForeground(180_000).build())
        FocusTimeManager.setEmergencyAllowedPackages(context, listOf("com.whatsapp"))
    }

    private fun screenshot(name: String, node: SemanticsNodeInteraction = compose.onRoot()) {
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        node.captureRoboImage("build/outputs/theme/${theme.lowercase()}-$name.png",
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
    }

    /** Checks rendered pixels, including nested controls, rather than matching color constants. */
    private fun assertNoLightPanel(tag: String) {
        if (theme != "DARK") return
        // Roborazzi draws with Robolectric's native renderer; PixelCopy-based captureToImage
        // waits for a device window redraw that is not available in this JVM environment.
        val file = File("build/outputs/theme/panels/$tag.png")
        file.delete()
        compose.onNodeWithTag(tag).captureRoboImage(file.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
        val pixels = checkNotNull(ImageIO.read(file))
        var light = 0
        var samples = 0
        for (y in 0 until pixels.height step 2) for (x in 0 until pixels.width step 2) {
            val pixel = pixels.getRGB(x, y)
            if ((pixel shr 16 and 255) > 191 && (pixel shr 8 and 255) > 191 && (pixel and 255) > 191) light++
            samples++
        }
        val lightFraction = light.toFloat() / samples
        assertTrue("$tag has a light panel: ${(lightFraction * 100).toInt()}% bright pixels", lightFraction < 0.16f)
    }

    @Test fun focusTimeCardsPresetsAndTimePickerUseSelectedTheme() {
        compose.setContent { LockInTheme(themeMode = theme) { FocusTimeScreen(onBack = {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("15m").fetchSemanticsNodes().isNotEmpty() }
        screenshot("focus-time")
        assertNoLightPanel("focus_warning_card")
        compose.onNodeWithTag("focus_content").performScrollToIndex(2)
        screenshot("focus-apps")
        assertNoLightPanel("focus_allowed_apps_card")
        compose.onNodeWithText("Liberado durante o foco").assertExists()
        compose.onNodeWithTag("focus_content").performScrollToIndex(0)
        compose.onNodeWithText("Personalizar").performClick()
        val picker = compose.onNodeWithTag("cmf_time_picker_dialog")
        picker.assertExists()
        screenshot("focus-time-picker", picker)
        assertNoLightPanel("cmf_time_picker_dialog")
    }

    @Test fun statisticsChartGaugeAndSummaryUseSelectedTheme() {
        compose.setContent { LockInTheme(themeMode = theme) { StatisticsScreen(emptyList(), emptyList(), onBack = {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("HORÁRIO", substring = true).fetchSemanticsNodes().isNotEmpty() }
        screenshot("statistics")
        assertNoLightPanel("stats_chart_card")
        compose.onNodeWithTag("stats_content").performScrollToNode(hasTestTag("stats_adherence_gauge"))
        screenshot("statistics-gauge")
        assertNoLightPanel("stats_adherence_gauge")
        compose.onNodeWithTag("stats_content").performScrollToNode(hasText("WhatsApp"))
        screenshot("statistics-apps")
        assertNoLightPanel("stats_screen_time_tile")
        assertNoLightPanel("stats_saved_time_tile")
        compose.onNodeWithText("WhatsApp").assertExists()
    }

    @Test fun friendsJoinButtonUsesThemeAndSwitchingPreservesTypedCode() {
        var selectedTheme by mutableStateOf(theme)
        compose.setContent { LockInTheme(themeMode = selectedTheme) { FriendsPanelScreen(onBack = {}) } }
        val field = compose.onNodeWithTag("friends_code_input_field")
        field.performTextInput("LCK-1234")
        screenshot("friends")
        assertNoLightPanel("friends_join_party_button")
        compose.runOnIdle { selectedTheme = if (theme == "DARK") "LIGHT" else "DARK" }
        compose.waitForIdle()
        field.assertTextContains("LCK-1234")
    }
}
