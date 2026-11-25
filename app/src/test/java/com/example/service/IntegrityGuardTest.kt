package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regras puras do detector de degradacao da protecao (leva 1 anti-burla). */
class IntegrityGuardTest {

    @Test
    fun blockingWithAccessibilityAndOverlayIsHealthy() {
        assertFalse(IntegrityGuard.isDegraded(blockActive = true, accessibilityOn = true, overlayOn = true))
    }

    @Test
    fun blockingWithAccessibilityOffIsDegraded() {
        assertTrue(IntegrityGuard.isDegraded(blockActive = true, accessibilityOn = false, overlayOn = true))
    }

    @Test
    fun blockingWithOverlayRevokedIsDegraded() {
        assertTrue(IntegrityGuard.isDegraded(blockActive = true, accessibilityOn = true, overlayOn = false))
    }

    @Test
    fun noBlockMeansNothingToRecover() {
        assertFalse(IntegrityGuard.isDegraded(blockActive = false, accessibilityOn = false, overlayOn = false))
    }

    @Test
    fun reopenRespectsCooldown() {
        assertFalse(IntegrityGuard.shouldReopen(nowMs = 10_000L, lastReopenMs = 9_000L, cooldownMs = 60_000L))
        assertTrue(IntegrityGuard.shouldReopen(nowMs = 70_000L, lastReopenMs = 10_000L, cooldownMs = 60_000L))
    }
}
