package com.example

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LockInTheme
import com.example.ui.tutorial.*
import com.github.takahirom.roborazzi.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class SpotlightTutorialTest {
    @get:Rule val compose = createComposeRule()
    @Test fun overlayKeepsTargetClearBlocksRealActionsAndCloses() {
        var clicks = 0
        var controller: TutorialController? = null
        compose.setContent { LockInTheme(themeMode="LIGHT") {
            TutorialHost {
                val current = LocalTutorialController.current
                SideEffect { controller=current }
                Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
                    Button(onClick={clicks++},modifier=Modifier.align(Alignment.TopCenter).padding(top=70.dp)
                        .tutorialTarget("overview").testTag("real_action")) { Text("Área importante") }
                    TutorialEntry(Modifier.align(Alignment.BottomCenter).padding(bottom=24.dp))
                }
            }
        } }
        compose.onNodeWithTag("tutorial_open").performClick()
        compose.waitUntil(5000) { controller?.anchors?.containsKey("overview") == true }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitUntil(5000) { compose.onAllNodesWithTag("robot_art").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tutorial_scrim").performTouchInput { click(Offset(center.x,100.dp.toPx())) }
        assertEquals(0,clicks)
        compose.onNodeWithTag("tutorial_guide").captureRoboImage("build/outputs/spotlight/guide.png",
            roborazziOptions=RoborazziOptions(taskType=RoborazziTaskType.Record))
        val before = compose.onNodeWithTag("tutorial_robot").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("tutorial_next").performClick()
        compose.mainClock.advanceTimeBy(1200)
        val after = compose.onNodeWithTag("tutorial_robot").fetchSemanticsNode().boundsInRoot
        assertTrue("Robot should move to the top corner", after.top < before.top)
        compose.onNodeWithTag("tutorial_hide").performClick()
        compose.onNodeWithTag("real_action").performClick()
        assertEquals(1,clicks)
    }
    @Test fun nineExpressionsRenderWithReducedMotion() {
        Settings.Global.putFloat(LockInApplication.instance.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,0f)
        compose.setContent { LockInTheme(themeMode="DARK") {
            Surface { Column {
                RobotExpression.entries.chunked(3).forEach { row -> Row {
                    row.forEach { expression -> Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                        TutorialRobot(Modifier.size(115.dp),expression,animate=false)
                        Text(expression.label,style=MaterialTheme.typography.labelSmall)
                    } }
                } }
            } }
        } }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("robot_art",useUnmergedTree=true).fetchSemanticsNodes().size == 9 }
        RobotExpression.entries.forEach { compose.onNodeWithContentDescription("Robô guia: ${it.label}").assertExists() }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/outputs/spotlight/expressions.png",
            roborazziOptions=RoborazziOptions(taskType=RoborazziTaskType.Record))
    }
    @Test fun settingsTourScrollsToPlayStoreWithoutActivatingIt() {
        var controller: TutorialController? = null
        var changes = 0
        compose.setContent { LockInTheme(themeMode="DARK") {
            TutorialHost {
                val current = LocalTutorialController.current
                SideEffect { controller = current }
                com.example.ui.screens.SettingsScreen("DARK",true,true,true,false,false,
                    currentPassword={""},onSaveThemeMode={changes++},onSaveHapticFeedback={changes++},
                    onSaveStreakReminders={changes++},onSavePreLimitAlerts={changes++},onSaveMindfulDelay={changes++},
                    onSaveStrictMode={changes++},onUpdatePassword={changes++},onResetToDefaults={changes++},
                    onNavigateToStyling={},onNavigateToResetSettings={},onNavigateToPermissions={},onBack={})
            }
        } }
        compose.runOnIdle { controller!!.start(24) }
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        compose.waitUntil(5000) {
            controller?.anchors?.get("play_store")?.bounds?.let { it.height>1 && it.center.y in 100f..700f } == true
        }
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
        compose.onNodeWithTag("tutorial_title").assertTextEquals("Bloqueio de apps da Play Store")
        compose.onNodeWithTag("tutorial_guide").captureRoboImage("build/outputs/spotlight/settings-play-store.png",
            roborazziOptions=RoborazziOptions(taskType=RoborazziTaskType.Record))
        assertEquals(0,changes)
        compose.onNodeWithTag("tutorial_hide").performClick()
        compose.onNodeWithTag("tutorial_guide").assertDoesNotExist()
    }
    @Test fun panelAvoidsSpotlightInPortraitLandscapeAndNearEdges() {
        for((width,height) in listOf(360f to 800f,320f to 568f,800f to 360f)) {
            for(center in listOf(Offset(width/2,80f),Offset(width/2,height/2),Offset(width/2,height-60f))) {
                val result=spotlightLayout(width,height,Rect(center-Offset(40f,20f),center+Offset(40f,20f)),1f)
                val circleBounds=Rect(result.center-Offset(result.radius,result.radius),result.center+Offset(result.radius,result.radius))
                assertFalse("Panel overlaps target: $result",result.panel.overlaps(circleBounds))
                assertTrue(result.panel.left>=0 && result.panel.top>=0 && result.panel.right<=width && result.panel.bottom<=height)
            }
        }
    }

    @Test fun guidedActionsBlockNextAndAdvanceWhenThePersonCompletesThem() {
        var controller: TutorialController? = null
        var credentialsPressed = 0
        var permissionsPressed = 0
        var levelPressed = 0
        compose.setContent { LockInTheme(themeMode = "DARK") {
            TutorialHost {
                val current = LocalTutorialController.current!!
                SideEffect { controller = current }
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { credentialsPressed++; current.markCredentialsComplete() },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp)
                            .tutorialTarget("account").testTag("credential_action")) { Text("Concluir credenciais") }
                    Button(onClick = { permissionsPressed++; current.updateEssentialPermissions(true) },
                        modifier = Modifier.align(Alignment.Center).tutorialTarget("permissions")
                            .testTag("permission_action")) { Text("Concluir permissões") }
                    Button(onClick = { levelPressed++; current.markLockLevelComplete() },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
                            .tutorialTarget("levels").testTag("level_action")) { Text("Confirmar nível") }
                }
            }
        } }
        compose.waitUntil(5_000) { controller != null }
        compose.runOnIdle { controller!!.start(1, guided = true) }
        compose.onNodeWithTag("tutorial_next").assertIsNotEnabled()
        compose.onNodeWithTag("credential_action").performClick()
        assertEquals(1, credentialsPressed)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Central de permissões").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tutorial_next").assertIsNotEnabled()
        compose.onNodeWithTag("permission_action").performClick()
        assertEquals(1, permissionsPressed)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Instalação por APK").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tutorial_next").assertIsEnabled()
        compose.onNodeWithTag("tutorial_next").performClick()
        compose.onNodeWithTag("tutorial_title").assertTextEquals("Escolha seu nível")
        compose.onNodeWithTag("tutorial_next").assertIsNotEnabled()
        compose.onNodeWithTag("level_action").performClick()
        assertEquals(1, levelPressed)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Como se localizar").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun spotlightRadiusAndRobotCornersChangeWithTheGuideStep() {
        val small = spotlightLayout(360f, 800f, Rect(100f, 200f, 160f, 250f), 1f, .8f)
        val large = spotlightLayout(360f, 800f, Rect(40f, 190f, 320f, 450f), 1f, 1.2f)
        assertTrue(large.radius > small.radius)
        assertEquals(TutorialRobotCorner.BOTTOM_END, tutorialRobotCorner(0))
        assertEquals(TutorialRobotCorner.TOP_END, tutorialRobotCorner(1))
        assertEquals(TutorialRobotCorner.BOTTOM_START, tutorialRobotCorner(2))
        assertEquals(TutorialRobotCorner.TOP_START, tutorialRobotCorner(3))
    }
}
