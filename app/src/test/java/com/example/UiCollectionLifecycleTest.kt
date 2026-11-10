package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiCollectionLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun screenStopsCollectingWhileBackgroundedAndResumesWhenVisible() {
        val prefs = LockInApplication.instance.userPreferences
        @Suppress("UNCHECKED_CAST")
        // Registration also has an application-level reminder scheduler. Theme is UI-only.
        val theme = prefs.javaClass.getDeclaredField("_themeMode").apply { isAccessible = true }
            .get(prefs) as MutableStateFlow<String>
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(theme.subscriptionCount.value > 0) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        assertEquals(0, theme.subscriptionCount.value)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(theme.subscriptionCount.value > 0) }
    }
}
