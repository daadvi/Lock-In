package com.example.service

import com.example.LockInApplication
import com.example.data.local.RestrictedAppEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerPersistenceTest {
    @Test fun stoppingSessionPersistsItsRemainingTimeAfterStateIsCleared() = runBlocking {
        val app = LockInApplication.instance
        val entity = RestrictedAppEntity("test.timer.persist", "Timer", individualRemainingSeconds = 900)
        app.repository.insertOrUpdateApp(entity)
        val manager = LimitTimerManager
        manager.javaClass.getDeclaredField("currentApp").apply { isAccessible = true }.set(manager, entity)
        @Suppress("UNCHECKED_CAST")
        val state = manager.javaClass.getDeclaredField("_sessionState").apply { isAccessible = true }.get(manager) as MutableStateFlow<ActiveSessionState>
        state.value = ActiveSessionState(isRunning = true, packageName = entity.packageName, remainingSeconds = 317)
        val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
        val testScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
        manager.stopTimer(app, testScope)
        assertFalse(state.value.isRunning)
        withTimeout(5000) {
            while (app.repository.getApp(entity.packageName)?.individualRemainingSeconds != 317L) {
                dispatcher.scheduler.runCurrent()
                delay(10)
            }
        }
        assertEquals(317L, app.repository.getApp(entity.packageName)!!.individualRemainingSeconds)
        } finally {
            testScope.cancel()
            app.repository.deleteApp(entity.packageName)
        }
    }
}
