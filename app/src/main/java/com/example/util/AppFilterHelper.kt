package com.example.util

import android.content.Context
import java.util.Locale

object AppFilterHelper {

    /**
     * Checks if a package or app name corresponds to "Lock in" itself,
     * so it never appears in the app limit selection list or limited apps list.
     */
    fun isLockInSelfApp(context: Context, packageName: String, appLabel: String = ""): Boolean {
        if (packageName == context.packageName) return true

        val lowerPkg = packageName.lowercase(Locale.ROOT)
        if (lowerPkg.contains("lockin") ||
            lowerPkg.contains("lock_in") ||
            lowerPkg.contains("pzkvtr") ||
            lowerPkg == "com.example" ||
            lowerPkg.startsWith("com.aistudio.lockin")
        ) {
            return true
        }

        val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()
        if (lowerLabel == "lock in" ||
            lowerLabel == "lockin" ||
            lowerLabel.startsWith("lock in") ||
            lowerLabel.endsWith("lock in")
        ) {
            return true
        }

        return false
    }
}
