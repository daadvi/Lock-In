package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.LockInApplication
import com.example.UninstallProtectionActivity
import com.example.ui.components.HighFrictionChallengeType
import com.example.ui.components.HighFrictionSecurityChallengeFlow
import com.example.ui.theme.LockInTheme

/** A real accessibility window: visible even when an OEM suppresses background Activity launches. */
internal class UninstallChallengeOverlay(private val service: AccessibilityService) {
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null
    val isShowing: Boolean get() = view != null

    fun show(mode: String): Boolean {
        if (isShowing) return true
        val manager = service.getSystemService(WindowManager::class.java) ?: return false
        val lifecycleOwner = OverlayOwner()
        val content = ComposeView(service)
        return try {
            lifecycleOwner.create()
            content.setViewTreeLifecycleOwner(lifecycleOwner)
            content.setViewTreeViewModelStoreOwner(lifecycleOwner)
            content.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            content.filterTouchesWhenObscured = true
            content.setOnKeyListener { _, code, event ->
                if (code == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) { cancel(); true } else false
            }
            val prefs = LockInApplication.instance.userPreferences
            val accent = runCatching { Color(prefs.textColorHex.value.toColorInt()) }.getOrDefault(Color(0xFFFF5500))
            content.setContent {
                val theme by prefs.themeMode.collectAsStateWithLifecycle()
                LockInTheme(customTextColor = accent, themeMode = theme) {
                    HighFrictionSecurityChallengeFlow(
                        challengeType = if (mode == UninstallProtectionActivity.MODE_DEACTIVATE_ADMIN)
                            HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN else HighFrictionChallengeType.UNINSTALL_LOCK_IN,
                        correctPassword = prefs.getTrustPassword(), accentColor = accent,
                        onCancel = ::cancel,
                        onSuccess = {
                            // The non-exported activity owns the Android removal result/cancellation flow.
                            // Start only after the user has completed the visible password/phrase challenge.
                            DeviceAdminManager.setUninstallAuthorized(service, true)
                            try {
                                service.startActivity(Intent(service, UninstallProtectionActivity::class.java).apply {
                                    putExtra(UninstallProtectionActivity.EXTRA_CHALLENGE_MODE, mode)
                                    putExtra(UninstallProtectionActivity.EXTRA_VERIFIED_OVERLAY, true)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                })
                                dismiss()
                            } catch (_: Exception) {
                                DeviceAdminManager.setUninstallAuthorized(service, false)
                                android.widget.Toast.makeText(service, "Não foi possível abrir a confirmação do Android. Tente novamente.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT
            ).apply {
                title = "Lock In • Confirmação de desinstalação"
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
            view = content
            owner = lifecycleOwner
            manager.addView(content, params)
            lifecycleOwner.resume()
            true
        } catch (_: Exception) {
            runCatching { manager.removeViewImmediate(content) }
            content.disposeComposition()
            lifecycleOwner.destroy()
            view = null
            owner = null
            false
        }
    }

    private fun cancel() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        dismiss()
    }

    fun dismiss() {
        view?.let { content ->
            content.disposeComposition()
            runCatching { service.getSystemService(WindowManager::class.java)?.removeViewImmediate(content) }
        }
        view = null
        owner?.destroy()
        owner = null
    }

    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
        override val viewModelStore = ViewModelStore()
        fun create() { savedState.performAttach(); savedState.performRestore(null); registry.currentState = Lifecycle.State.CREATED }
        fun resume() { registry.currentState = Lifecycle.State.RESUMED }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED; viewModelStore.clear() }
    }
}
