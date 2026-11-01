package com.example.service

import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import java.util.Locale

object PhoneAppHelper {

    private val KNOWN_PHONE_PACKAGES = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.android.incallui",
        "com.google.android.incallui",
        "com.motorola.incallui",
        "com.miui.incallui",
        "com.coloros.incallui",
        "com.asus.incallui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.sh.smart.caller",
        "com.asus.contacts",
        "com.sonyericsson.android.socialphonebook",
        "com.lge.dialer",
        "com.huawei.android.contacts",
        "com.coloros.phoneno",
        "com.oneplus.dialer",
        "com.transsion.phonemaster",
        "com.motorola.dialer"
    )

    /**
     * Determines whether an application package is the system Phone / Dialer app.
     * The phone app must never be selectable as an allowed app because it is already
     * permanently accessible on the lock screen via the dedicated circular black & white button.
     */
    fun isPhoneDialerApp(context: Context, packageName: String, appLabel: String = ""): Boolean {
        if (KNOWN_PHONE_PACKAGES.contains(packageName)) {
            return true
        }

        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val defaultDialer = telecom?.defaultDialerPackage
            if (!defaultDialer.isNullOrBlank() && packageName == defaultDialer) {
                return true
            }
        } catch (_: Exception) {}

        try {
            val dialerIntent = Intent(Intent.ACTION_DIAL)
            val resolveList = context.packageManager.queryIntentActivities(dialerIntent, 0)
            if (resolveList.any { it.activityInfo.packageName == packageName }) {
                return true
            }
        } catch (_: Exception) {}

        val lowerPkg = packageName.lowercase(Locale.ROOT)
        if (lowerPkg.contains(".dialer") ||
            lowerPkg.contains("incallui") ||
            lowerPkg.contains("callui") ||
            lowerPkg.endsWith(".phone") ||
            lowerPkg.contains("telecom") ||
            lowerPkg == "com.android.contacts" && lowerPkg.contains("dial")
        ) {
            return true
        }

        val lowerLabel = appLabel.lowercase(Locale.ROOT)
        if (lowerLabel == "telefone" || lowerLabel == "phone" || lowerLabel == "discador") {
            return true
        }

        return false
    }
}
