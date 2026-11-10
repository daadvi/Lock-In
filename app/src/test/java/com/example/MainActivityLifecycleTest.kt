package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLifecycleTest {
    @Test fun opensRecreatesAndClosesWithoutLosingActivity() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup().visible()
            assertFalse(controller.get().isFinishing)
            controller.recreate()
            assertFalse(controller.get().isFinishing)
        }
    }
}
