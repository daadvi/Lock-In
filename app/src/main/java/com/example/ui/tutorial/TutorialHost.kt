package com.example.ui.tutorial

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.example.Screen

internal data class TutorialAnchor(val owner: Any, val bounds: Rect)

/** Actions that the guided first-use tutorial waits for before it can continue. */
enum class TutorialAction {
    NONE,
    CREDENTIALS,
    ESSENTIAL_PERMISSIONS,
    LOCK_LEVEL
}

class TutorialController {
    var chapter by mutableStateOf<Int?>(null)
    var visible by mutableStateOf(false)
        private set
    var guidedOnboarding by mutableStateOf(false)
        private set
    var credentialsComplete by mutableStateOf(false)
        private set
    var essentialPermissionsComplete by mutableStateOf(false)
        private set
    var lockLevelComplete by mutableStateOf(false)
        private set
    internal val anchors = mutableStateMapOf<String, TutorialAnchor>()
    val target: String? get() = chapter?.let(::tutorialTargetId)

    fun start(chapter: Int, guided: Boolean = false) {
        this.chapter = chapter.coerceIn(tutorialLessons.indices)
        guidedOnboarding = guided
        visible = true
    }

    fun hide() {
        visible = false
    }

    fun finish() {
        chapter = null
        visible = false
        guidedOnboarding = false
    }

    fun markCredentialsComplete() {
        credentialsComplete = true
    }

    fun updateEssentialPermissions(complete: Boolean) {
        essentialPermissionsComplete = complete
    }

    fun markLockLevelComplete() {
        lockLevelComplete = true
    }

    fun actionRequired(chapter: Int): TutorialAction =
        if (guidedOnboarding) tutorialLessons[chapter.coerceIn(tutorialLessons.indices)].action else TutorialAction.NONE

    fun isActionComplete(action: TutorialAction): Boolean = when (action) {
        TutorialAction.NONE -> true
        TutorialAction.CREDENTIALS -> credentialsComplete
        TutorialAction.ESSENTIAL_PERMISSIONS -> essentialPermissionsComplete
        TutorialAction.LOCK_LEVEL -> lockLevelComplete
    }
}
val LocalTutorialController = staticCompositionLocalOf<TutorialController?> { null }

@Composable
fun TutorialHost(onStart: () -> Unit = {}, onNavigate: (Int) -> Unit = {}, onFinish: () -> Unit = {},
                 content: @Composable () -> Unit) {
    val controller = remember { TutorialController() }
    val startCallback by rememberUpdatedState(onStart)
    val navigateCallback by rememberUpdatedState(onNavigate)
    CompositionLocalProvider(LocalTutorialController provides controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (controller.visible && controller.chapter != null) {
                TutorialGuide(
                    controller = controller,
                    embedded = true,
                    onHide = { controller.hide() },
                    onComplete = {
                        controller.finish()
                        onFinish()
                    }
                )
            }
        }
        LaunchedEffect(controller.chapter, controller.visible) {
            controller.chapter?.let {
                if (controller.visible) {
                    startCallback()
                    navigateCallback(it)
                }
            }
        }
    }
}

/** Anchors follow actual layout/scrolling; never guesses screen coordinates. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val controller = LocalTutorialController.current
    if (controller == null) return@composed this
    val owner = remember { Any() }
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(controller, id) {
        onDispose { if (controller.anchors[id]?.owner === owner) controller.anchors.remove(id) }
    }
    var placed by remember { mutableStateOf(false) }
    var targetSize by remember { mutableStateOf(IntSize.Zero) }
    val clearance = with(LocalDensity.current) { 108.dp.toPx() }
    LaunchedEffect(controller.target, id, placed) {
        if (controller.target == id && placed) {
            // Let a parent lazy list finish positioning its item before relocating the child.
            kotlinx.coroutines.delay(100)
            requester.bringIntoView(Rect(0f, -clearance, targetSize.width.toFloat(), targetSize.height + clearance))
        }
    }
    this.bringIntoViewRequester(requester).onGloballyPositioned {
        placed = true
        targetSize = it.size
        val anchor = TutorialAnchor(owner, it.boundsInWindow())
        if (controller.anchors[id] != anchor) controller.anchors[id] = anchor
    }
}

fun tutorialTargetId(chapter: Int): String = when (chapter) {
    0, 5, 27 -> "overview"
    1 -> "account"
    2, 3 -> "permissions"
    4 -> "levels"
    6 -> "device_time"
    7, 8, 10, 11, 12 -> "apps"
    9 -> "groups"
    13 -> "focus"
    14, 15 -> "routines"
    16 -> "statistics"
    17 -> "adherence"
    18 -> "streak"
    19 -> "friends"
    20 -> "island"
    21 -> "reset"
    22 -> "account"
    23 -> "password"
    24 -> "play_store"
    25 -> "websites"
    else -> "uninstall"
}
fun tutorialScreen(chapter: Int): Screen = when (chapter) {
    0, 5, 27 -> Screen.DASHBOARD
    6 -> Screen.CLOCK_LIMIT
    in 7..12 -> Screen.APP_SELECTION
    in 13..15 -> Screen.FOCUS_TIME
    in 16..18 -> Screen.STATISTICS
    19 -> Screen.FRIENDS_PANEL
    else -> Screen.SETTINGS
}
fun settingsTutorialTarget(title: String): String = when (title) {
    "Conta & Sincronização em Nuvem" -> "account"
    "Central de Permissões" -> "permissions"
    "Nível de Lock (Bloqueio)" -> "levels"
    "Estilização da Ilha & Cores" -> "island"
    "Tempo de Reset do Telefone" -> "reset"
    "Senha de Confiança" -> "password"
    "Blacklist da Play Store" -> "play_store"
    "Bloqueio de Sites (Blacklist Web)" -> "websites"
    "Proteção Contra Desinstalação" -> "uninstall"
    else -> "settings_$title"
}
