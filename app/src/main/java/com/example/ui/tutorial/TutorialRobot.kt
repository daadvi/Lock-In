package com.example.ui.tutorial

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import kotlinx.coroutines.delay

enum class RobotExpression(val label: String, val openness: Float, val brow: Float, val smile: Float) {
    HAPPY("feliz", 1f, 0f, 1f), AFRAID("com medo", 1.35f, -.5f, 0f),
    SUSPICIOUS("suspeito", .45f, -.8f, -.1f), CLEVER("sagaz", .7f, -.5f, .6f),
    AWAKE("desperto", 1.2f, 0f, .2f), SMART("inteligente", .85f, 0f, .5f),
    DETERMINED("determinado", .65f, .65f, 0f), LAZY("preguiçoso", .22f, 0f, -.2f),
    VERY_DETERMINED("muito determinado", .55f, 1f, -.3f)
}

fun tutorialExpression(chapter: Int): RobotExpression = when (chapter) {
    0, 19, 27 -> RobotExpression.HAPPY
    3, 26 -> RobotExpression.AFRAID
    10, 25 -> RobotExpression.SUSPICIOUS
    9, 14, 15 -> RobotExpression.CLEVER
    1, 6, 17 -> RobotExpression.AWAKE
    2, 7, 16, 20, 22 -> RobotExpression.SMART
    5, 8, 18, 21, 23 -> RobotExpression.DETERMINED
    11 -> RobotExpression.LAZY
    else -> RobotExpression.VERY_DETERMINED
}

@Composable
fun TutorialRobot(modifier: Modifier = Modifier, expression: RobotExpression = RobotExpression.HAPPY,
                  animate: Boolean = true) {
    val context = LocalContext.current
    val art = RobotArt.cached
    LaunchedEffect(Unit) {
        if (RobotArt.cached == null)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { RobotArt.load(context.resources) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var blink by remember { mutableFloatStateOf(1f) }
    // Blink only while this guide is visible/resumed, respecting Android's animation setting.
    LaunchedEffect(lifecycle, animate) {
        if (!animate) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f)
                return@repeatOnLifecycle
            try {
                while (true) { delay(4200); blink = .05f; delay(130); blink = 1f }
            } finally { blink = 1f }
        }
    }
    val eyes by animateFloatAsState(expression.openness * blink, tween(if (animate) 220 else 0), label = "robot_eyes")
    val brow by animateFloatAsState(expression.brow, tween(if (animate) 350 else 0), label = "robot_brows")
    val smile by animateFloatAsState(expression.smile, tween(if (animate) 350 else 0), label = "robot_smile")
    Box(modifier.semantics { contentDescription = "Robô guia: ${expression.label}" }) {
        art?.let { Image(it, null, Modifier.matchParentSize().testTag("robot_art")) }
        if (art != null) Canvas(Modifier.matchParentSize()) {
            val scale = size.minDimension
            val offset = Offset((size.width-scale)/2, (size.height-scale)/2)
            fun point(x: Float, y: Float) = offset + Offset(x * scale, y * scale)
            val ink = Color(0xFFF5F1EB)
            val orange = Color(0xFFFF7724)
            val eyeColor = if (expression == RobotExpression.VERY_DETERMINED) orange else ink
            for ((i,x) in listOf(.35f, .49f).withIndex()) {
                val height = .052f * eyes * if (expression == RobotExpression.CLEVER && i == 1) .18f else 1f
                drawRoundRect(eyeColor, point(x, .495f-height/2), Size(scale*.025f, scale*height), CornerRadius(scale*.012f))
                if (brow != 0f) drawLine(ink, point(x-.009f,.449f-(if(i==0) 1 else -1)*brow*.008f),
                    point(x+.034f,.449f+(if(i==0) 1 else -1)*brow*.008f), scale*.007f, StrokeCap.Round)
            }
            if (expression == RobotExpression.AFRAID) {
                drawOval(ink, point(.414f,.529f), Size(scale*.025f,scale*.032f), style = Stroke(scale*.006f))
            } else {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(point(.397f,.541f).x,point(.397f,.541f).y)
                    quadraticBezierTo(point(.426f,.541f+smile*.028f).x,point(.426f,.541f+smile*.028f).y,
                        point(.456f,.541f).x,point(.456f,.541f).y)
                }
                drawPath(path, ink, style = Stroke(scale*.007f, cap = StrokeCap.Round))
            }
            if (expression == RobotExpression.SMART) {
                for(x in listOf(.333f,.473f)) drawRoundRect(ink, point(x,.459f), Size(scale*.06f,scale*.068f),
                    CornerRadius(scale*.014f), style=Stroke(scale*.004f))
                drawLine(ink,point(.393f,.482f),point(.473f,.482f),scale*.004f)
            }
        }
    }
}

// One shared, sampled bitmap (~1.5 MiB), decoded off the UI thread; no Activity retained.
private object RobotArt {
    var cached by mutableStateOf<ImageBitmap?>(null)
        private set
    @Synchronized fun load(resources: android.content.res.Resources): ImageBitmap = cached ?: run {
        android.graphics.BitmapFactory.decodeResource(resources, R.drawable.tutorial_robot_base,
            android.graphics.BitmapFactory.Options().apply { inSampleSize = 2; inScaled = false })
            .asImageBitmap().also { cached = it }
    }
}
