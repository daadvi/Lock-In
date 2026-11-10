package com.example.service

import org.junit.Assert.*
import org.junit.Test

class NodeScanBudgetTest {
    @Test fun boundsLookupAttemptsEvenWhenEveryChildIsNullOrHasNoText() {
        val budget = NodeScanBudget(maxLookups = 120, clock = { 0L })
        budget.reset()
        var lookups = 0
        repeat(10_000) { if (budget.tryAcquire()) lookups++ }
        assertEquals(120, lookups)
        budget.reset()
        assertTrue(budget.tryAcquire())
    }
    @Test fun stopsAfterSlowBinderCallAndResetsForNextEvent() {
        var now = 10L
        val budget = NodeScanBudget(clock = { now })
        budget.reset()
        assertTrue(budget.tryAcquire())
        now += 61
        assertFalse(budget.tryAcquire())
        budget.reset()
        assertTrue(budget.tryAcquire())
    }
}
