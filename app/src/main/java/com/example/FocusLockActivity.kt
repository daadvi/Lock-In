package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FocusTimeManager
import com.example.service.PhoneAppHelper
import com.example.ui.components.EmergencyUnlockScreen
import com.example.ui.components.LockInBranding
import com.example.ui.components.SlideUpEmergencyButton
import com.example.ui.theme.BlockyFontFamily
import kotlinx.coroutines.delay

class FocusLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Display over lock screen when phone is turned on, but DO NOT keep screen on permanently
        // so the phone can be turned off / sleep to save battery.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        // Tapjacking / overlay snooping protection
        window.decorView.filterTouchesWhenObscured = true

        // Strict lock: Back button does nothing
        onBackPressedDispatcher.addCallback(this) {
            // Unbreakable lock - No exit allowed
        }

        setContent {
            FocusLockScreen(
                onFinishSession = {
                    finish()
                }
            )
        }
    }

    companion object {
        @Volatile
        var isLaunchingAllowedApp: Boolean = false
        @Volatile
        var lastAllowedLaunchEpoch: Long = 0L
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val now = System.currentTimeMillis()
        if (isLaunchingAllowedApp || (now - lastAllowedLaunchEpoch < 6000L)) {
            // User deliberately opened an allowed app or phone dialer from the dock!
            // Let the allowed app open and stay in front without snatching the screen back.
            isLaunchingAllowedApp = false
            return
        }
        if (FocusTimeManager.isFocusTimeActive(this)) {
            FocusTimeManager.bringFocusLockToFront(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check if focus session expired
        if (!FocusTimeManager.isFocusTimeActive(this)) {
            finish()
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode && FocusTimeManager.isFocusTimeActive(this)) {
            FocusTimeManager.bringFocusLockToFront(this)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode && FocusTimeManager.isFocusTimeActive(this)) {
            FocusTimeManager.bringFocusLockToFront(this)
        }
    }
}

data class AllowedAppItem(
    val packageName: String,
    val appName: String,
    val iconBitmap: Bitmap?
)

@Composable
fun FocusLockScreen(
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    var showEmergencyUnlockScreen by remember { mutableStateOf(false) }

    if (showEmergencyUnlockScreen) {
        EmergencyUnlockScreen(
            onDismiss = { showEmergencyUnlockScreen = false },
            onUnlockConfirmed = { onFinishSession() }
        )
        return
    }

    var remainingSec by remember { mutableLongStateOf(FocusTimeManager.getRemainingSeconds(context)) }
    val totalMinutes = remember { FocusTimeManager.getTotalFocusMinutes(context) }
    val endTimeStr = remember { FocusTimeManager.getEndTimeFormatted(context) }
    val routineName = remember { FocusTimeManager.getActiveRoutineName(context) ?: "Focus Time" }
    val allowedPackages = remember {
        FocusTimeManager.getAllowedPackages(context)
            .filterNot { PhoneAppHelper.isPhoneDialerApp(context, it) }
            .take(3)
    }

    // Load allowed app items asynchronously on IO dispatcher to keep UI 60fps
    val allowedAppItems = remember { mutableStateListOf<AllowedAppItem>() }
    LaunchedEffect(allowedPackages) {
        val loaded = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val items = mutableListOf<AllowedAppItem>()
            allowedPackages.take(3).forEach { pkg ->
                if (PhoneAppHelper.isPhoneDialerApp(context, pkg)) return@forEach
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = appInfo.loadLabel(pm).toString()
                    if (PhoneAppHelper.isPhoneDialerApp(context, pkg, label)) return@forEach
                    val icon = appInfo.loadIcon(pm)
                    val bmp = drawableToBitmap(icon)
                    items.add(AllowedAppItem(pkg, label, bmp))
                } catch (_: Exception) {
                }
            }

            // Fallback to installed essential apps if none were chosen so dock is populated
            if (items.isEmpty()) {
                val candidatePkgs = listOf(
                    "com.google.android.apps.messaging",
                    "com.android.mms",
                    "com.google.android.gm",
                    "com.google.android.apps.maps"
                )
                for (cand in candidatePkgs) {
                    if (items.size >= 3) break
                    if (PhoneAppHelper.isPhoneDialerApp(context, cand)) continue
                    try {
                        val appInfo = pm.getApplicationInfo(cand, 0)
                        val label = appInfo.loadLabel(pm).toString()
                        if (PhoneAppHelper.isPhoneDialerApp(context, cand, label)) continue
                        val icon = appInfo.loadIcon(pm)
                        val bmp = drawableToBitmap(icon)
                        items.add(AllowedAppItem(cand, label, bmp))
                    } catch (_: Exception) {
                    }
                }
            }
            items.take(3)
        }
        allowedAppItems.clear()
        allowedAppItems.addAll(loaded)
        val loadedPkgs = loaded.map { it.packageName }.filter { it.isNotBlank() }
        if (loadedPkgs.isNotEmpty()) {
            FocusTimeManager.setEmergencyAllowedPackages(context, loadedPkgs)
        }
    }

    // Live countdown ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val sec = FocusTimeManager.getRemainingSeconds(context)
            remainingSec = sec
            if (sec <= 0L && !FocusTimeManager.isFocusTimeActive(context)) {
                onFinishSession()
                break
            }
        }
    }

    val hours = remainingSec / 3600
    val minutes = (remainingSec % 3600) / 60
    val seconds = remainingSec % 60

    val primaryClockText = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    // Pure Dark Palette matching user's uploaded reference image
    // Minimalist Dark Bento Palette
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF131315)
    val cardBorder = Color(0xFF222226)
    val textWhite = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF7E7E88)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Centered App Name (Lock in) with Minimalist Logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LockInBranding(
                        iconSize = 18.dp,
                        textSize = 15.sp,
                        textColor = textWhite,
                        iconTint = Color(0xFFFF5500)
                    )
                }

                // CENTER BENTO SECTION: Centered Digital Clock with Finished Time directly below
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Primary Centered Clock Card with Scheduled Finish Time Integrated
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(cardBackground)
                            .border(1.dp, cardBorder, RoundedCornerShape(28.dp))
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = primaryClockText,
                                fontSize = if (hours > 0) 54.sp else 68.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = BlockyFontFamily,
                                color = textWhite,
                                letterSpacing = 3.sp,
                                modifier = Modifier.testTag("focus_lock_clock")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TEMPO RESTANTE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BlockyFontFamily,
                                color = textMuted,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // End Time (horário que o timer será finalizado) directly underneath the timer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B1B1E))
                                    .border(1.dp, Color(0xFF2B2B30), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "TÉRMINO: ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = textMuted,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = endTimeStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BlockyFontFamily,
                                    color = textWhite,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // BOTTOM AREA: Slide-up Emergency Button (Left) + Allowed Apps Pill (Center) + Phone Dialer (Right)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("focus_emergency_dock"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 1. LEFT SIDE: Slide-up button (upward slider) for Emergency Unlock
                        SlideUpEmergencyButton(
                            onTrigger = {
                                showEmergencyUnlockScreen = true
                            }
                        )

                        // 2. CENTER: Capsule Pill with whitelisted apps
                        if (allowedAppItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(cardBackground)
                                    .border(1.dp, cardBorder, RoundedCornerShape(32.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    allowedAppItems.forEach { appItem ->
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF222228))
                                                .clickable {
                                                    launchAllowedApp(context, appItem.packageName)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (appItem.iconBitmap != null) {
                                                Image(
                                                    bitmap = appItem.iconBitmap.asImageBitmap(),
                                                    contentDescription = appItem.appName,
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Text(
                                                    text = appItem.appName.firstOrNull()?.uppercase() ?: "A",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = BlockyFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // 3. RIGHT SIDE: Circular black & white emergency button for Phone Dialer
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    launchDialer(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Telefone de Emergência",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun launchAllowedApp(context: Context, packageName: String) {
    try {
        FocusLockActivity.isLaunchingAllowedApp = true
        FocusLockActivity.lastAllowedLaunchEpoch = System.currentTimeMillis()
        com.example.service.FocusTimeManager.notifyAllowedAppLaunched(packageName)
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                context is android.app.Activity && km != null && km.isKeyguardLocked) {
                km.requestDismissKeyguard(context, object : android.app.KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            FocusLockActivity.isLaunchingAllowedApp = false
                        }
                    }
                    override fun onDismissCancelled() {
                        FocusLockActivity.isLaunchingAllowedApp = false
                    }
                    override fun onDismissError() {
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            FocusLockActivity.isLaunchingAllowedApp = false
                        }
                    }
                })
            } else {
                context.startActivity(intent)
            }
        } else {
            FocusLockActivity.isLaunchingAllowedApp = false
        }
    } catch (_: Exception) {
        FocusLockActivity.isLaunchingAllowedApp = false
    }
}

private fun launchDialer(context: Context) {
    try {
        FocusLockActivity.isLaunchingAllowedApp = true
        FocusLockActivity.lastAllowedLaunchEpoch = System.currentTimeMillis()
        val intent = Intent(Intent.ACTION_DIAL).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                context is android.app.Activity && km != null && km.isKeyguardLocked) {
            km.requestDismissKeyguard(context, object : android.app.KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        FocusLockActivity.isLaunchingAllowedApp = false
                    }
                }
                override fun onDismissCancelled() {
                    FocusLockActivity.isLaunchingAllowedApp = false
                }
                override fun onDismissError() {
                    FocusLockActivity.isLaunchingAllowedApp = false
                }
            })
        } else {
            context.startActivity(intent)
        }
    } catch (_: Exception) {
        FocusLockActivity.isLaunchingAllowedApp = false
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val bitmap = Bitmap.createBitmap(
        if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96,
        if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
