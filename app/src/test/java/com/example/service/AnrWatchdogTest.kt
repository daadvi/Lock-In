package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regra de detecção de travamento da main thread usada pelo AnrWatchdog. */
class AnrWatchdogTest {

    @Test
    fun freshHeartbeatIsNotStalled() {
        assertFalse(AnrWatchdog.isStalled(lastBeatMs = 1_000L, nowMs = 1_500L))
    }

    @Test
    fun heartbeatOlderThanThresholdIsStalled() {
        assertTrue(AnrWatchdog.isStalled(lastBeatMs = 1_000L, nowMs = 5_500L))
        assertTrue(AnrWatchdog.isStalled(lastBeatMs = 1_000L, nowMs = 5_500L, thresholdMs = 4_500L))
    }

    @Test
    fun thresholdBoundaryIsInclusive() {
        assertTrue(AnrWatchdog.isStalled(lastBeatMs = 1_000L, nowMs = 5_500L, thresholdMs = 4_500L))
        assertFalse(AnrWatchdog.isStalled(lastBeatMs = 1_000L, nowMs = 5_499L, thresholdMs = 4_500L))
    }

    @Test
    fun watchdogThatNeverStartedDoesNotReportStalled() {
        assertFalse(AnrWatchdog.isStalled(lastBeatMs = 0L, nowMs = 99_000L))
    }
}
