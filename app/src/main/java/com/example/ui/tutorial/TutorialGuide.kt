package com.example.ui.tutorial

import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun TutorialEntry(modifier: Modifier = Modifier, guidedOnboarding: Boolean = false) {
    val host = LocalTutorialController.current
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("lock_in_tutorial", 0) }
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = {
        val chapter = preferences.getInt("chapter", 0).coerceIn(tutorialLessons.indices)
        preferences.edit().putBoolean("hidden", false).apply()
        if (host != null) host.start(chapter, guidedOnboarding) else open = true
    }, modifier = modifier.fillMaxWidth().testTag("tutorial_open")) {
        TutorialRobot(Modifier.size(40.dp), animate = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Conheça o Lock In", style = MaterialTheme.typography.titleSmall)
            Text("Guia na tela • sem áudio", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (open) {
        TutorialGuide(
            onHide = { preferences.edit().putBoolean("hidden", true).apply(); open = false },
            onComplete = { open = false }
        )
    }
}

internal data class SpotlightLayout(val center: Offset, val radius: Float, val panel: Rect)

/** Place the message in the largest free region around the real target. */
internal fun spotlightLayout(
    width: Float,
    height: Float,
    target: Rect?,
    density: Float,
    spotlightScale: Float = 1f
): SpotlightLayout {
    val margin = 16 * density
    val requestedCenter = target?.center ?: Offset(width * .5f, height * .22f)
    val maxRadius = (min(width, height) * .38f).coerceAtLeast(24 * density)
    val minRadius = min(44 * density, maxRadius)
    val targetDiameter = target?.let { max(it.height, min(it.width, 180 * density)) }
        ?: min(148 * density, min(width, height) * .42f)
    val radius = ((targetDiameter / 2f + 20 * density) * spotlightScale)
        .coerceIn(minRadius, maxRadius)
    val center = Offset(
        requestedCenter.x.coerceIn(margin + radius, (width - margin - radius).coerceAtLeast(margin + radius)),
        requestedCenter.y.coerceIn(margin + radius, (height - margin - radius).coerceAtLeast(margin + radius))
    )
    val gap = 12 * density
    val areas = listOf(
        Rect(margin, margin, width - margin, (center.y - radius - gap).coerceAtLeast(margin)),
        Rect(margin, (center.y + radius + gap).coerceAtMost(height - margin), width - margin, height - margin),
        Rect(margin, margin, (center.x - radius - gap).coerceAtLeast(margin), height - margin),
        Rect((center.x + radius + gap).coerceAtMost(width - margin), margin, width - margin, height - margin)
    )
    val preferred = areas.filter { it.width >= min(250 * density, width - 2 * margin) && it.height >= 180 * density }
    val area = (preferred.ifEmpty { areas }).maxBy { it.width * it.height }
    val panelWidth = min(area.width.coerceAtLeast(0f), 400 * density)
    val panelHeight = min(area.height.coerceAtLeast(0f), 400 * density)
    val left = area.left + (area.width - panelWidth) / 2f
    val top = if (area.top > center.y) area.bottom - panelHeight else area.top
    return SpotlightLayout(center, radius, Rect(left, top, left + panelWidth, top + panelHeight))
}

internal enum class TutorialRobotCorner { BOTTOM_END, TOP_END, BOTTOM_START, TOP_START }

internal fun tutorialRobotCorner(chapter: Int): TutorialRobotCorner = when (Math.floorMod(chapter, 4)) {
    0 -> TutorialRobotCorner.BOTTOM_END
    1 -> TutorialRobotCorner.TOP_END
    2 -> TutorialRobotCorner.BOTTOM_START
    else -> TutorialRobotCorner.TOP_START
}

@Composable
fun TutorialGuide(
    onHide: () -> Unit,
    onComplete: () -> Unit = onHide,
    controller: TutorialController = remember { TutorialController() },
    embedded: Boolean = false
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("lock_in_tutorial", 0) }
    var index by rememberSaveable {
        mutableIntStateOf((controller.chapter ?: preferences.getInt("chapter", 0)).coerceIn(tutorialLessons.indices))
    }
    val lesson = tutorialLessons[index]
    val action = controller.actionRequired(index)
    val actionComplete = controller.isActionComplete(action)
    val canAdvance = action == TutorialAction.NONE || actionComplete

    fun goTo(next: Int) {
        val destination = next.coerceIn(tutorialLessons.indices)
        if (destination > index && !canAdvance) return
        index = destination
        preferences.edit().putInt("chapter", destination).apply()
    }

    LaunchedEffect(index) { controller.chapter = index }
    LaunchedEffect(index, action, actionComplete) {
        if (action != TutorialAction.NONE && actionComplete && index < tutorialLessons.lastIndex) {
            delay(260)
            goTo(index + 1)
        }
    }

    val hide = {
        preferences.edit().putInt("chapter", index).putBoolean("hidden", true).apply()
        onHide()
    }
    val complete = {
        preferences.edit().putBoolean("completed", true).putBoolean("hidden", false).apply()
        onComplete()
    }

    if (embedded) {
        TutorialGuideOverlay(index, lesson, controller, action, actionComplete, ::goTo, hide, complete)
    } else {
        Dialog(onDismissRequest = hide, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            val view = LocalView.current
            SideEffect {
                (view.parent as? DialogWindowProvider)?.window?.apply {
                    clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                }
            }
            TutorialGuideOverlay(index, lesson, controller, action, actionComplete, ::goTo, hide, complete)
        }
    }
}

@Composable
private fun TutorialGuideOverlay(
    index: Int,
    lesson: TutorialLesson,
    controller: TutorialController,
    action: TutorialAction,
    actionComplete: Boolean,
    goTo: (Int) -> Unit,
    onHide: () -> Unit,
    onComplete: () -> Unit
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    // On guided action steps the Canvas has no pointer handler, so the highlighted real control stays usable.
    val allowsInteraction = controller.guidedOnboarding && action != TutorialAction.NONE
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }.testTag("tutorial_guide")
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val raw = controller.anchors[tutorialTargetId(index)]?.bounds
        val target = raw?.translate(-origin)?.takeIf {
            it.width > 1 && it.height > 1 && it.center.x in 0f..width && it.center.y in 0f..height
        }
        val layout = spotlightLayout(width, height, target, density.density, lesson.spotlightScale)
        val spotlightX by animateFloatAsState(layout.center.x, tween(430), label = "spotlight_x")
        val spotlightY by animateFloatAsState(layout.center.y, tween(430), label = "spotlight_y")
        val spotlightRadius by animateFloatAsState(layout.radius, tween(430), label = "spotlight_radius")
        val center = Offset(spotlightX, spotlightY)

        val scrimModifier = if (allowsInteraction) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}
            )
        }
        Canvas(scrimModifier.testTag("tutorial_scrim")) {
            val mask = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                if (target != null) addOval(Rect(
                    center.x - spotlightRadius, center.y - spotlightRadius,
                    center.x + spotlightRadius, center.y + spotlightRadius
                ))
            }
            drawPath(mask, Color.Black.copy(alpha = .76f))
            if (target != null) drawCircle(Color(0xFFFF873E), spotlightRadius, center, style = Stroke(2.dp.toPx()))
        }

        val panelWidth = with(density) { layout.panel.width.toDp() }.coerceAtLeast(220.dp)
        val panelHeight = with(density) { layout.panel.height.toDp() }.coerceIn(190.dp, 400.dp)
        val panelX by animateDpAsState(with(density) { layout.panel.left.toDp() }, tween(430), label = "companion_x")
        val panelY by animateDpAsState(with(density) { layout.panel.top.toDp() }, tween(430), label = "companion_y")
        val robotSize = if (height < with(density) { 560.dp.toPx() }) 64.dp else 88.dp
        val screenWidth = with(density) { width.toDp() }
        val screenHeight = with(density) { height.toDp() }
        val edge = 18.dp
        val corner = tutorialRobotCorner(index)
        val robotTargetX = when (corner) {
            TutorialRobotCorner.BOTTOM_END, TutorialRobotCorner.TOP_END -> (screenWidth - robotSize - edge).coerceAtLeast(edge)
            TutorialRobotCorner.BOTTOM_START, TutorialRobotCorner.TOP_START -> edge
        }
        val robotTargetY = when (corner) {
            TutorialRobotCorner.BOTTOM_END, TutorialRobotCorner.BOTTOM_START -> (screenHeight - robotSize - edge).coerceAtLeast(edge)
            TutorialRobotCorner.TOP_END, TutorialRobotCorner.TOP_START -> edge
        }
        val robotX by animateDpAsState(robotTargetX, tween(600), label = "robot_x")
        val robotY by animateDpAsState(robotTargetY, tween(600), label = "robot_y")

        Surface(
            Modifier.offset(x = panelX, y = panelY).width(panelWidth).height(panelHeight).testTag("tutorial_companion"),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("${index + 1} / ${tutorialLessons.size}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("tutorial_title"))
                Spacer(Modifier.height(8.dp))
                key(index) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        if (target == null) {
                            Text("Veja esta parte na seção indicada. Ao concluir a configuração, o guia acompanha a próxima tela.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(lesson.speech, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("tutorial_speech"))
                        if (action != TutorialAction.NONE && !actionComplete) {
                            Spacer(Modifier.height(10.dp))
                            Text(tutorialActionHint(action), color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("tutorial_requirement"))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onHide, contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.weight(1f).testTag("tutorial_hide")) { Text("Ocultar") }
                    TextButton(
                        onClick = { if (index == tutorialLessons.lastIndex) onComplete() else goTo(index + 1) },
                        enabled = action == TutorialAction.NONE || actionComplete,
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.weight(1f).testTag("tutorial_next")
                    ) {
                        Text(when {
                            index == tutorialLessons.lastIndex -> "Concluir"
                            action != TutorialAction.NONE && !actionComplete -> "Aguardando"
                            else -> "Próximo"
                        })
                    }
                }
            }
        }
        TutorialRobot(
            Modifier.offset(x = robotX, y = robotY).size(robotSize).testTag("tutorial_robot"),
            tutorialExpression(index)
        )
    }
}

private fun tutorialActionHint(action: TutorialAction): String = when (action) {
    TutorialAction.CREDENTIALS -> "Conclua suas credenciais abaixo para eu seguir automaticamente."
    TutorialAction.ESSENTIAL_PERMISSIONS -> "Ative Acessibilidade, Acesso ao uso e Sobreposição para eu seguir automaticamente."
    TutorialAction.LOCK_LEVEL -> "Escolha um nível e confirme para eu continuar automaticamente."
    TutorialAction.NONE -> ""
}
