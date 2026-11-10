package com.example.widget

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetUpdateQueueTest {
    @Test fun tenThousandRequestsKeepOnlyOnePendingRefresh() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var active = 0
        var peak = 0
        val queue = WidgetUpdateQueue(backgroundScope) {
            calls++
            active++
            peak = maxOf(peak, active)
            if (calls == 1) gate.await()
            active--
        }
        queue.request(context)
        runCurrent()
        assertEquals(1, calls)
        repeat(10_000) { queue.request(context) }
        runCurrent()
        assertEquals(1, calls)
        gate.complete(Unit)
        runCurrent()
        assertEquals(2, calls)
        assertEquals(1, peak)
        queue.request(context)
        runCurrent()
        assertEquals(3, calls)
    }
    @Test fun cancelledWorkerDoesNotKeepProcessingRequests() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        var calls = 0
        val queue = WidgetUpdateQueue(scope) { calls++ }
        scope.cancel()
        runCurrent()
        repeat(10_000) { queue.request(RuntimeEnvironment.getApplication()) }
        runCurrent()
        assertEquals(0, calls)
    }
}
