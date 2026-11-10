package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * AppBlockerAccessibilityService
 *
 * Monitors Google Play Store (com.android.vending) and Package Installers in real-time.
 * Scans on-screen UI nodes (AccessibilityNodeInfo) to detect if an app displayed for installation
 * matches any item in the Lock In installation blacklist.
 *
 * If a match is detected:
 * - Immediately executes performGlobalAction(GLOBAL_ACTION_BACK) to kick the user out of the app page.
 * - Displays a notification Toast informing the user that the app is blacklisted from installation.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockerAccess"

        // Default list of commonly blocked addictive apps to block if custom blacklist is empty
        val DEFAULT_BLACKLIST = listOf(
            "TikTok",
            "Instagram",
            "Kwai",
            "Free Fire",
            "Roblox",
            "Twitter",
            "X",
            "Tinder",
            "Snapchat",
            "Brawl Stars"
        )

        /**
         * Instruction guide string to guide the user in Android Settings
         */
        const val ACTIVATION_INSTRUCTIONS =
            "Para ativar o bloqueador em tempo real de instalações:\n" +
            "1. Abra as Configurações do Aparelho.\n" +
            "2. Vá em 'Acessibilidade'.\n" +
            "3. Toque em 'Apps Instalados' ou 'Serviços Baixados'.\n" +
            "4. Selecione 'Lock In - Bloqueador de Instalação' (ou Lock In).\n" +
            "5. Ative a chave e confirme a permissão."

        /**
         * Opens the Android Accessibility Settings screen directly.
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening accessibility settings", e)
            }
        }
    }

    private var lastActionEpoch = 0L
    private val scanBudget = NodeScanBudget()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AppBlockerAccessibilityService conectado e ativo para monitorar Play Store e Instaladores.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            val pkg = event.packageName?.toString() ?: return
            val isPlayStore = pkg == "com.android.vending"
            val isInstaller = pkg.contains("packageinstaller") || pkg.contains("installer")

            if (!isPlayStore && !isInstaller) {
                return
            }

            // Throttle rapid repeated events within 400ms to avoid feedback loops
            val now = System.currentTimeMillis()
            if (now - lastActionEpoch < 400L) {
                return
            }

            lastActionEpoch = now
            scanBudget.reset()
            val rootNode = rootInActiveWindow ?: event.source ?: return

            // 1. Retrieve the active blacklist (from PlayStoreBlacklistManager or fallback)
            val activeBlacklist = PlayStoreBlacklistManager.blacklistedApps.value.ifEmpty { DEFAULT_BLACKLIST }

            // 2. Read interface nodes (AccessibilityNodeInfo)
            val onScreenTexts = mutableListOf<String>()
            try {
                collectNodeTexts(rootNode, onScreenTexts, depth = 0)
            } finally {
                try { rootNode.recycle() } catch (_: Exception) {}
            }

            // CRITICAL: If an uninstallation of ANY other application is occurring, ALLOW IT FREELY!
            if (LockInAccessibilityService.isAnyUninstallActive()) {
                return
            }

            val isUninstallationPrompt = onScreenTexts.any {
                val lower = it.lowercase()
                lower.contains("desinstalar") || lower.contains("uninstall") ||
                        lower.contains("deseja desinstalar") || lower.contains("desinstalação") ||
                        lower.contains("remover") || lower.contains("excluir")
            }
            if (isUninstallationPrompt) {
                LockInAccessibilityService.isUninstallingOtherApp = true
                return
            }

            // 3. Check if any text corresponds to an item in the blacklist
            val matchedApp = findBlacklistMatch(onScreenTexts, activeBlacklist)

            if (matchedApp != null) {
                lastActionEpoch = now
                Log.w(TAG, "Aplicativo Proibido detectado na Play Store/Instalador: '$matchedApp'. Executando GLOBAL_ACTION_BACK imediatamente!")

                // 4. Execute performGlobalAction(GLOBAL_ACTION_BACK) immediately
                val actionResult = performGlobalAction(GLOBAL_ACTION_BACK)
                Log.d(TAG, "Resultado do performGlobalAction(GLOBAL_ACTION_BACK): $actionResult")

                // Show informative toast to the user
                mainHandler.post {
                    try {
                        Toast.makeText(
                            applicationContext,
                            "Bloqueado pelo Lock In: '$matchedApp' está na sua lista negra de instalação!",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {}
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Safe catch in onAccessibilityEvent: ${t.message}", t)
        }
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, texts: MutableList<String>, depth: Int) {
        if (node == null || depth > 8 || texts.size >= 80) return

        try {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                texts.add(text)
            }

            val desc = node.contentDescription?.toString()?.trim()
            if (!desc.isNullOrBlank()) {
                texts.add(desc)
            }

            for (i in 0 until node.childCount) {
                if (!scanBudget.tryAcquire()) break
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                try {
                    collectNodeTexts(child, texts, depth + 1)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    private fun findBlacklistMatch(texts: List<String>, blacklist: List<String>): String? {
        for (item in blacklist) {
            val normalizedItem = item.lowercase().trim()
            if (normalizedItem.isBlank()) continue

            for (text in texts) {
                val normalizedText = text.lowercase().trim()
                if (normalizedText == normalizedItem) {
                    return item
                }
                // Match word boundaries for compound titles e.g. "TikTok - Vídeos Curtos"
                if (normalizedText.startsWith("$normalizedItem ") ||
                    normalizedText.endsWith(" $normalizedItem") ||
                    normalizedText.contains(" $normalizedItem ") ||
                    normalizedText.startsWith("$normalizedItem:") ||
                    normalizedText.startsWith("$normalizedItem -") ||
                    normalizedText.startsWith("$normalizedItem –")) {
                    return item
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppBlockerAccessibilityService interrompido.")
    }
}
