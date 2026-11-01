package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import androidx.core.content.ContextCompat
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.LockInApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingIslandService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: LinearLayout? = null
    private var timerTextView: TextView? = null
    private var labelTextView: TextView? = null
    private var dotView: View? = null
    private var progressBarContainer: LinearLayout? = null
    private var progressBarView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var observeJob: Job? = null
    private var watcherJob: Job? = null
    private var prefsJob: Job? = null
    private var demoJob: Job? = null

    private var isDemoMode: Boolean = false
    private var demoSeconds: Long = 15 * 60L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    watcherJob?.cancel()
                    floatingView?.visibility = View.GONE
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    startForegroundAppWatcher()
                    val state = LimitTimerManager.sessionState.value
                    if (state.isRunning && floatingView != null && !isDemoMode) {
                        floatingView?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForegroundServiceNotification()
        if (Settings.canDrawOverlays(this)) {
            setupOverlayView()
        }
        observeTimerState()
        observePreferences()
        startForegroundAppWatcher()

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(
                this,
                screenReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null && Settings.canDrawOverlays(this)) {
            setupOverlayView()
        }

        when (intent?.action) {
            ACTION_TEST_ISLAND -> {
                isDemoMode = true
                startDemoSimulation()
            }
            ACTION_STOP_TEST -> {
                isDemoMode = false
                demoJob?.cancel()
                val current = LimitTimerManager.sessionState.value
                if (!current.isRunning) {
                    floatingView?.visibility = View.GONE
                }
            }
            ACTION_SHOW -> {
                if (floatingView == null && Settings.canDrawOverlays(this)) {
                    setupOverlayView()
                }
                floatingView?.visibility = View.VISIBLE
            }
            ACTION_HIDE -> {
                isDemoMode = false
                demoJob?.cancel()
                floatingView?.visibility = View.GONE
            }
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "lock_in_island_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lock in - Ilha Flutuante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoramento ativo de limites e ilha flutuante"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Lock in Ativo")
            .setContentText("Ilha flutuante e monitoramento de tempo prontos")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    1001,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            Log.e("FloatingIslandService", "startForeground error", e)
        }
    }

    private fun setupOverlayView() {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        if (floatingView != null && floatingView?.isAttachedToWindow == true) {
            val isRunning = LimitTimerManager.sessionState.value.isRunning
            floatingView?.visibility = if (isDemoMode || isRunning) View.VISIBLE else View.GONE
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val prefs = (applicationContext as? LockInApplication)?.userPreferences ?: LockInApplication.instance.userPreferences

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val initialX = (screenWidth * prefs.floatingIslandPosX.value).toInt()
        val initialY = (screenHeight * prefs.floatingIslandPosY.value).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val safeMarginX = dpToPx(8)
        val safeMarginTop = dpToPx(36)
        val safeMarginBottom = dpToPx(64)

        val clampedX = initialX.coerceIn(safeMarginX, (screenWidth - dpToPx(130)).coerceAtLeast(safeMarginX))
        val clampedY = initialY.coerceIn(safeMarginTop, (screenHeight - dpToPx(80)).coerceAtLeast(safeMarginTop))

        windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampedX
            y = clampedY
        }

        val textColor = try {
            AndroidColor.parseColor(prefs.textColorHex.value)
        } catch (_: Exception) {
            AndroidColor.parseColor("#FF5500")
        }

        // Build Nothing OS / CMF dark matte aesthetic floating capsule
        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(7), dpToPx(15), dpToPx(7))
            alpha = prefs.floatingIslandOpacity.value

            val backgroundShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(AndroidColor.parseColor("#141416"))
                setStroke(dpToPx(1), AndroidColor.parseColor("#2E2E34"))
            }
            background = backgroundShape
            elevation = dpToPx(10).toFloat()
        }

        // Small indicator dot
        dotView = View(this).apply {
            val dotShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(2).toFloat()
                setColor(textColor)
            }
            background = dotShape
            val params = LinearLayout.LayoutParams(dpToPx(7), dpToPx(7)).apply {
                marginEnd = dpToPx(9)
            }
            layoutParams = params
        }
        floatingView?.addView(dotView)

        // Text labels inside the island
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        labelTextView = TextView(this).apply {
            text = "LOCK IN"
            textSize = 9f
            typeface = Typeface.MONOSPACE
            setTextColor(AndroidColor.parseColor("#9E9EA4"))
            letterSpacing = 0.08f
        }
        textContainer.addView(labelTextView)

        timerTextView = TextView(this).apply {
            text = "15:00"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(AndroidColor.WHITE)
        }
        textContainer.addView(timerTextView)

        // Micro Progress Bar indicating remaining proportion of limit
        progressBarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(dpToPx(65), dpToPx(3)).apply {
                topMargin = dpToPx(3)
            }
            layoutParams = params
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(2).toFloat()
                setColor(AndroidColor.parseColor("#26262B"))
            }
        }

        progressBarView = View(this).apply {
            val barShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(2).toFloat()
                setColor(textColor)
            }
            background = barShape
            layoutParams = LinearLayout.LayoutParams(dpToPx(65), LinearLayout.LayoutParams.MATCH_PARENT)
        }
        progressBarContainer?.addView(progressBarView)
        textContainer.addView(progressBarContainer)

        floatingView?.addView(textContainer)

        // Touch & Drag listener with 60fps smooth edge snapping and click detection
        var startTouchX = 0f
        var startTouchY = 0f
        var initialViewX = 0
        var initialViewY = 0
        var isDragging = false
        var currentScreenWidth = 1080
        var currentScreenHeight = 2400
        var viewWidth = dpToPx(120)
        var viewHeight = dpToPx(44)
        var minX = dpToPx(8)
        var maxX = 1000
        var minY = dpToPx(36)
        var maxY = 2200
        var lastLayoutX = -1
        var lastLayoutY = -1

        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    initialViewX = windowLayoutParams?.x ?: 0
                    initialViewY = windowLayoutParams?.y ?: 0
                    isDragging = false

                    val metrics = resources.displayMetrics
                    currentScreenWidth = metrics.widthPixels
                    currentScreenHeight = metrics.heightPixels
                    viewWidth = floatingView?.width?.takeIf { it > 0 } ?: dpToPx(120)
                    viewHeight = floatingView?.height?.takeIf { it > 0 } ?: dpToPx(44)
                    minX = dpToPx(8)
                    maxX = (currentScreenWidth - viewWidth - dpToPx(8)).coerceAtLeast(minX)
                    minY = dpToPx(36)
                    maxY = (currentScreenHeight - viewHeight - dpToPx(60)).coerceAtLeast(minY)
                    lastLayoutX = initialViewX
                    lastLayoutY = initialViewY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val dy = event.rawY - startTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val newX = (initialViewX + dx.toInt()).coerceIn(minX, maxX)
                        val newY = (initialViewY + dy.toInt()).coerceIn(minY, maxY)
                        if (abs(newX - lastLayoutX) >= 2 || abs(newY - lastLayoutY) >= 2) {
                            windowLayoutParams?.let { params ->
                                params.x = newX
                                params.y = newY
                                lastLayoutX = newX
                                lastLayoutY = newY
                                safeUpdateLayout(params)
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Single tap: bring Lock in to front
                        bringAppToFront()
                    } else {
                        // Drag completed: magnetic snap to nearest horizontal edge for clean dock
                        windowLayoutParams?.let { params ->
                            val midPoint = params.x + (viewWidth / 2)
                            val snapX = if (midPoint < (currentScreenWidth / 2)) minX else maxX
                            params.x = snapX
                            safeUpdateLayout(params)
                            try {
                                floatingView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            } catch (_: Exception) {}

                            val ratioX = (params.x.toFloat() / currentScreenWidth).coerceIn(0.02f, 0.98f)
                            val ratioY = (params.y.toFloat() / currentScreenHeight).coerceIn(0.04f, 0.96f)
                            prefs.setFloatingIslandSettings(
                                size = prefs.floatingIslandSize.value,
                                opacity = prefs.floatingIslandOpacity.value,
                                posX = ratioX,
                                posY = ratioY
                            )
                        }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            if (floatingView?.isAttachedToWindow != true) {
                windowManager?.addView(floatingView, windowLayoutParams)
            }
            // If demo mode is active or timer is currently running, keep visible (unless Lock In itself is in foreground)
            val isRunning = LimitTimerManager.sessionState.value.isRunning
            floatingView?.visibility = if (!isLockInInForeground && (isDemoMode || isRunning)) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e("FloatingIslandService", "Error adding overlay view", e)
        }
    }

    private fun safeUpdateLayout(params: WindowManager.LayoutParams?) {
        if (params == null) return
        val view = floatingView ?: return
        try {
            if (view.isAttachedToWindow) {
                windowManager?.updateViewLayout(view, params)
            }
        } catch (e: Exception) {
            Log.w("FloatingIslandService", "safeUpdateLayout failed: ${e.message}")
        }
    }

    private fun bringAppToFront() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun observeTimerState() {
        observeJob?.cancel()
        observeJob = scope.launch {
            LimitTimerManager.sessionState.collectLatest { state ->
                if (isDemoMode) {
                    // Demo mode has priority
                    return@collectLatest
                }

                val isPausedRule = state.appName?.contains("Pausado", ignoreCase = true) == true
                val shouldShow = (state.isRunning && state.remainingSeconds > 0) || isPausedRule

                if (shouldShow) {
                    if (floatingView == null && Settings.canDrawOverlays(this@FloatingIslandService)) {
                        setupOverlayView()
                    }
                    if (!isLockInInForeground && floatingView?.visibility != View.VISIBLE) {
                        floatingView?.visibility = View.VISIBLE
                    }
                    val minutes = state.remainingSeconds / 60
                    val seconds = state.remainingSeconds % 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)
                    if (timerTextView?.text != timeStr) {
                        timerTextView?.text = timeStr
                    }

                    val prefs = LockInApplication.instance.userPreferences
                    val accentColor = try {
                        AndroidColor.parseColor(prefs.textColorHex.value)
                    } catch (_: Exception) {
                        AndroidColor.parseColor("#FF5500")
                    }

                    // Progress bar calculation
                    val totalSec = if (state.totalLimitSeconds > 0) state.totalLimitSeconds else (15 * 60L)
                    val progress = (state.remainingSeconds.toFloat() / totalSec.toFloat()).coerceIn(0.02f, 1f)
                    val containerWidth = dpToPx(65)
                    val fillWidth = (containerWidth * progress).toInt().coerceAtLeast(dpToPx(3))

                    val isUrgent = state.remainingSeconds in 1..120 // Less than 2 minutes remaining
                    val statusColor = when {
                        isUrgent -> AndroidColor.parseColor("#EF4444") // Flashing alert red
                        progress < 0.25f -> AndroidColor.parseColor("#F59E0B") // Warning amber
                        state.isAppGroup -> accentColor
                        else -> accentColor
                    }

                    val currentLp = progressBarView?.layoutParams
                    if (currentLp == null || currentLp.width != fillWidth) {
                        progressBarView?.layoutParams = LinearLayout.LayoutParams(fillWidth, LinearLayout.LayoutParams.MATCH_PARENT)
                    }
                    (progressBarView?.background as? GradientDrawable)?.setColor(statusColor)

                    if (isPausedRule) {
                        labelTextView?.text = "PAUSADO"
                        labelTextView?.setTextColor(AndroidColor.parseColor("#A1A1AA"))
                        timerTextView?.setTextColor(AndroidColor.parseColor("#71717A"))
                        (dotView?.background as? GradientDrawable)?.setColor(AndroidColor.parseColor("#71717A"))
                    } else {
                        val isShort = state.isShortVideo
                        val isVideoApp = AppVideoCategoryHelper.isVideoPlatform(state.packageName, state.websiteDomain)
                        val hasShortVideos = AppVideoCategoryHelper.hasShortVideos(state.packageName ?: "") ||
                                AppVideoCategoryHelper.hasShortVideosForDomain(state.websiteDomain)

                        val baseTypeLabel = when {
                            state.isAppGroup && !state.appGroupName.isNullOrBlank() -> "GRUPO • ${state.appGroupName.take(9).uppercase()}"
                            state.isAppGroup -> "TEMPO EM GRUPO"
                            isShort -> {
                                if (state.packageName?.contains("instagram") == true) "REELS" else "SHORTS"
                            }
                            isVideoApp -> {
                                if (hasShortVideos) "VÍDEO LONGO" else "VÍDEO"
                            }
                            !state.appName.isNullOrBlank() -> "APP"
                            else -> "APP"
                        }
                        val typeLabel = if (state.isPictureInPicture) "PIP • $baseTypeLabel" else baseTypeLabel
                        labelTextView?.text = typeLabel
                        when {
                            state.isAppGroup -> {
                                labelTextView?.setTextColor(accentColor)
                            }
                            isShort -> {
                                labelTextView?.setTextColor(accentColor)
                            }
                            typeLabel.contains("VÍDEO") -> {
                                labelTextView?.setTextColor(AndroidColor.parseColor("#38BDF8")) // Cyan for long videos
                            }
                            else -> {
                                labelTextView?.setTextColor(AndroidColor.parseColor("#E4E4E7")) // Crisp white-gray for app timer
                            }
                        }

                        if (isUrgent) {
                            timerTextView?.setTextColor(AndroidColor.parseColor("#EF4444"))
                            (dotView?.background as? GradientDrawable)?.setColor(AndroidColor.parseColor("#EF4444"))
                        } else {
                            timerTextView?.setTextColor(AndroidColor.WHITE)
                            (dotView?.background as? GradientDrawable)?.setColor(accentColor)
                        }
                    }

                } else {
                    if (!isDemoMode) {
                        floatingView?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observePreferences() {
        val prefs = LockInApplication.instance.userPreferences
        prefsJob?.cancel()
        prefsJob = scope.launch {
            launch {
                prefs.textColorHex.collectLatest { hex ->
                    val color = try {
                        AndroidColor.parseColor(hex)
                    } catch (_: Exception) {
                        AndroidColor.parseColor("#FF5500")
                    }
                    val isPausedRule = LimitTimerManager.sessionState.value.appName?.contains("Pausado", ignoreCase = true) == true
                    if (!isPausedRule) {
                        (dotView?.background as? GradientDrawable)?.setColor(color)
                    }
                }
            }
            launch {
                prefs.floatingIslandOpacity.collectLatest { opacity ->
                    floatingView?.alpha = opacity
                }
            }
            launch {
                prefs.floatingIslandSize.collectLatest { size ->
                    applySizePreset(size)
                }
            }
            launch {
                prefs.floatingIslandPosX.collectLatest { ratioX ->
                    val metrics = resources.displayMetrics
                    val sWidth = metrics.widthPixels
                    val viewWidth = floatingView?.width?.takeIf { it > 0 } ?: dpToPx(120)
                    val targetX = (sWidth * ratioX).toInt().coerceIn(dpToPx(8), (sWidth - viewWidth - dpToPx(8)).coerceAtLeast(dpToPx(8)))
                    windowLayoutParams?.let { params ->
                        if (params.x != targetX) {
                            params.x = targetX
                            safeUpdateLayout(params)
                        }
                    }
                }
            }
            launch {
                prefs.floatingIslandPosY.collectLatest { ratioY ->
                    val metrics = resources.displayMetrics
                    val sHeight = metrics.heightPixels
                    val viewHeight = floatingView?.height?.takeIf { it > 0 } ?: dpToPx(44)
                    val targetY = (sHeight * ratioY).toInt().coerceIn(dpToPx(36), (sHeight - viewHeight - dpToPx(60)).coerceAtLeast(dpToPx(36)))
                    windowLayoutParams?.let { params ->
                        if (params.y != targetY) {
                            params.y = targetY
                            safeUpdateLayout(params)
                        }
                    }
                }
            }
        }
    }

    private fun applySizePreset(size: String) {
        when (size) {
            "SMALL" -> {
                floatingView?.setPadding(dpToPx(10), dpToPx(5), dpToPx(12), dpToPx(5))
                timerTextView?.textSize = 13f
                labelTextView?.textSize = 8f
            }
            "LARGE" -> {
                floatingView?.setPadding(dpToPx(18), dpToPx(9), dpToPx(20), dpToPx(9))
                timerTextView?.textSize = 18f
                labelTextView?.textSize = 10.5f
            }
            else -> { // MEDIUM
                floatingView?.setPadding(dpToPx(14), dpToPx(7), dpToPx(15), dpToPx(7))
                timerTextView?.textSize = 15f
                labelTextView?.textSize = 9f
            }
        }
        floatingView?.requestLayout()
        windowLayoutParams?.let { params ->
            safeUpdateLayout(params)
        }
    }

    private fun startDemoSimulation() {
        demoJob?.cancel()
        demoSeconds = 14 * 60L + 59L

        if (floatingView == null && Settings.canDrawOverlays(this)) {
            setupOverlayView()
        }
        floatingView?.visibility = View.VISIBLE
        labelTextView?.text = "LOCK IN"

        demoJob = scope.launch {
            while (isDemoMode && demoSeconds > 0) {
                val min = demoSeconds / 60
                val sec = demoSeconds % 60
                timerTextView?.text = String.format("%02d:%02d", min, sec)
                delay(1000L)
                demoSeconds--
            }
            if (isDemoMode && demoSeconds <= 0) {
                timerTextView?.text = "00:00"
            }
        }
    }

    /**
     * Fallback watcher: queries foreground apps via UsageStatsManager
     * This guarantees limits and floating island work on Android devices even if
     * Accessibility Service has system restrictions.
     */
    private fun startForegroundAppWatcher() {
        watcherJob?.cancel()
        watcherJob = scope.launch(Dispatchers.Default) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive) {
                if (LockInAccessibilityService.instance != null) {
                    delay(15000L)
                    continue
                }
                delay(3000L)
                try {
                    val foregroundPackage = getForegroundPackage(usageStatsManager)
                    if (foregroundPackage != null) {
                        if (foregroundPackage == packageName) {
                            LimitTimerManager.stopTimer(applicationContext)
                        } else if (BankingSecurityHelper.isBankingOrFinancialApp(foregroundPackage) || BankingSecurityHelper.isBankingProtectionPaused()) {
                            // Completely exempt banking applications from floating overlay and monitoring
                            if (floatingView?.visibility == View.VISIBLE) {
                                scope.launch(Dispatchers.Main) { floatingView?.visibility = View.GONE }
                            }
                            LimitTimerManager.stopTimer(applicationContext)
                        } else {
                            val currentState = LimitTimerManager.sessionState.value
                            // If accessibility service is already actively monitoring this exact package,
                            // do NOT override its real-time Shorts vs Long video detection!
                            if (currentState.isRunning && currentState.packageName == foregroundPackage) {
                                continue
                            }

                            val isTikTok = foregroundPackage.contains("tiktok") ||
                                    foregroundPackage.contains("zhiliaoapp") ||
                                    foregroundPackage.contains("trill")

                            LimitTimerManager.onAppForegroundChanged(
                                context = applicationContext,
                                packageName = foregroundPackage,
                                isShortVideo = isTikTok
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun getForegroundPackage(usm: UsageStatsManager?): String? {
        if (usm == null) return null
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 4000
        val events = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        observeJob?.cancel()
        watcherJob?.cancel()
        prefsJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        if (floatingView != null) {
            try {
                if (floatingView?.isAttachedToWindow == true) {
                    windowManager?.removeView(floatingView)
                }
            } catch (_: Exception) {}
            floatingView = null
        }
        if (instance == this) {
            instance = null
        }
    }

    fun updateVisibilityForForegroundState() {
        scope.launch(Dispatchers.Main) {
            if (isLockInInForeground) {
                floatingView?.visibility = View.GONE
            } else {
                val isRunning = LimitTimerManager.sessionState.value.isRunning
                if (isDemoMode || isRunning) {
                    if (floatingView == null && Settings.canDrawOverlays(this@FloatingIslandService)) {
                        setupOverlayView()
                    }
                    floatingView?.visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        @Volatile
        var isLockInInForeground: Boolean = false
            private set

        @Volatile
        var instance: FloatingIslandService? = null
            private set

        fun setLockInInForeground(inForeground: Boolean) {
            isLockInInForeground = inForeground
            instance?.updateVisibilityForForegroundState()
        }

        const val ACTION_SHOW = "com.example.service.ACTION_SHOW"
        const val ACTION_HIDE = "com.example.service.ACTION_HIDE"
        const val ACTION_TEST_ISLAND = "com.example.service.ACTION_TEST_ISLAND"
        const val ACTION_STOP_TEST = "com.example.service.ACTION_STOP_TEST"

        fun ensureStarted(context: Context) {
            val intent = Intent(context, FloatingIslandService::class.java)
            startServiceCompat(context, intent)
        }

        fun showIsland(context: Context) {
            val intent = Intent(context, FloatingIslandService::class.java).apply {
                action = ACTION_SHOW
            }
            startServiceCompat(context, intent)
        }

        fun hideIsland(context: Context) {
            val intent = Intent(context, FloatingIslandService::class.java).apply {
                action = ACTION_HIDE
            }
            startServiceCompat(context, intent)
        }

        fun startTestIsland(context: Context) {
            val intent = Intent(context, FloatingIslandService::class.java).apply {
                action = ACTION_TEST_ISLAND
            }
            startServiceCompat(context, intent)
        }

        fun stopTestIsland(context: Context) {
            val intent = Intent(context, FloatingIslandService::class.java).apply {
                action = ACTION_STOP_TEST
            }
            startServiceCompat(context, intent)
        }

        private fun startServiceCompat(context: Context, intent: Intent) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("FloatingIslandService", "Could not start service: ${e.message}")
            }
        }
    }
}
