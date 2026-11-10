package com.example.service

import android.os.SystemClock

/** Bounds child lookups, including null and text-free nodes, across one event.
 * The deadline is checked between Binder calls; it cannot interrupt a blocked call.
 */
internal class NodeScanBudget(
    private val maxLookups: Int = 120,
    private val maxMillis: Long = 60,
    private val clock: () -> Long = { SystemClock.uptimeMillis() }
) {
    private var remaining = 0
    private var started = 0L

    fun reset() {
        remaining = maxLookups
        started = clock()
    }

    fun tryAcquire(): Boolean {
        if (remaining <= 0 || clock() - started >= maxMillis) return false
        remaining--
        return true
    }
}
