package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity
import com.example.service.DeviceStatsResult
import com.example.service.DeviceUsageStatsHelper
import com.example.service.InstallFilterMode
import com.example.service.StreakManager
import androidx.compose.ui.graphics.Path
import com.example.ui.components.AppIconImage
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.theme.BlockyFontFamily
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

enum class StatsPeriod(val ptLabel: String, val enLabel: String) {
    DAY("Dia", "Day"),
    WEEK("Semana", "Week"),
    MONTH("Mês", "Month"),
    YEAR("Ano", "Year")
}

data class ChartBarData(
    val label: String,
    val valueMinutes: Int,
    val fullDateDesc: String
)

@Composable
fun StatisticsScreen(
    limitedApps: List<RestrictedAppEntity>,
    websites: List<RestrictedWebsiteEntity>,
    sharedRemainingSeconds: Long = 0L,
    sharedLimitMinutes: Int = 20,
    isSharedMode: Boolean = false,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    val streakDays by StreakManager.streakFlow.collectAsState()
    val recordStreak = remember(streakDays) { StreakManager.getRecordStreak(context) }

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.DAY) }
    var periodOffset by remember { mutableIntStateOf(0) }
    var selectedBarIndex by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    var showStreakModal by remember { mutableStateOf(false) }
    var installFilterMode by remember { mutableStateOf(InstallFilterMode.AFTER) }

    // Permission and Real Device Stats State
    var hasUsagePermission by remember {
        mutableStateOf(DeviceUsageStatsHelper.hasUsageAccessPermission(context))
    }
    var deviceStats by remember { mutableStateOf<DeviceStatsResult?>(null) }
    var isLoadingStats by remember { mutableStateOf(true) }

    // Polling permission state in case user returns from system settings
    LaunchedEffect(Unit) {
        while (true) {
            val granted = DeviceUsageStatsHelper.hasUsageAccessPermission(context)
            if (granted != hasUsagePermission) {
                hasUsagePermission = granted
            }
            delay(1500L)
        }
    }

    // Query real device stats whenever period, offset, permission, or install filter mode changes
    LaunchedEffect(selectedPeriod, periodOffset, limitedApps, hasUsagePermission, installFilterMode) {
        isLoadingStats = true
        withContext(Dispatchers.IO) {
            val result = DeviceUsageStatsHelper.queryDeviceStats(
                context = context,
                period = selectedPeriod,
                offset = periodOffset,
                restrictedApps = limitedApps,
                filterMode = installFilterMode
            )
            deviceStats = result
            isLoadingStats = false
        }
    }

    val chartBars = deviceStats?.chartBars ?: emptyList()
    val safeSelectedIndex = selectedBarIndex.coerceIn(0, chartBars.lastIndex.coerceAtLeast(0))
    val currentSelectedBar = chartBars.getOrNull(safeSelectedIndex) ?: chartBars.firstOrNull()

    val totalDeviceMinutes = deviceStats?.totalDeviceMinutes ?: 0
    val savedMinutes = deviceStats?.savedMinutes ?: 0
    val adherenceRatio = deviceStats?.adherenceRatio ?: 1.0f
    val topApps = deviceStats?.topApps ?: emptyList()

    val periodNavigationTitle = deviceStats?.periodLabel ?: when (selectedPeriod) {
        StatsPeriod.DAY -> if (periodOffset == 0) "HOJE" else "DIA"
        StatsPeriod.WEEK -> if (periodOffset == 0) "ESTA SEMANA" else "SEMANA"
        StatsPeriod.MONTH -> "MÊS ATUAL"
        StatsPeriod.YEAR -> "ANO ATUAL"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DotMatrixPattern(modifier = Modifier.fillMaxSize(), spacingDp = 22f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Top Header Bar:
                // Left: Pill button with streak counter (Clickable to view automatic streak of consecutive days)
                // Center: Branding
                // Right: Back / Navigation action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // STREAK BUTTON (Automatic streak)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF0F0F3))
                            .border(1.dp, Color(0xFFE2E2E6), RoundedCornerShape(18.dp))
                            .clickable { showStreakModal = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("stats_streak_top_left_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accentColor)
                            )
                            Text(
                                text = "$streakDays",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CmfDark
                            )
                            Text(
                                text = "STREAK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText
                            )
                        }
                    }

                    // Centered title
                    Text(
                        text = "LOCK IN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Right side back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F0F3))
                            .testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = CmfDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Usage Stats Permission Banner if not granted yet on device
                    if (!hasUsagePermission) {
                        item {
                            NothingCard(
                                title = "ACESSO DE USO DO DISPOSITIVO",
                                badge = "AÇÃO NECESSÁRIA"
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Para que o Lock In leia e exiba o tempo de tela real do seu aparelho e o uso exato dos aplicativos, conceda a permissão de Acesso de Uso no sistema Android.",
                                        fontSize = 11.sp,
                                        color = CmfMutedText,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Abra as Configurações do Android e busque 'Acesso de Uso'.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Conceder Acesso de Uso",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Before vs After Install Filter Button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFEBEBF0))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isBefore = installFilterMode == InstallFilterMode.BEFORE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isBefore) Color(0xFF2B2B2E) else Color.Transparent)
                                    .clickable { installFilterMode = InstallFilterMode.BEFORE }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ANTES",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        color = if (isBefore) Color.White else Color(0xFF6A6A70)
                                    )
                                    Text(
                                        text = "Antes do Lock in",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isBefore) Color.LightGray else Color(0xFF8E8E93)
                                    )
                                }
                            }

                            val isAfter = installFilterMode == InstallFilterMode.AFTER
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAfter) accentColor else Color.Transparent)
                                    .clickable { installFilterMode = InstallFilterMode.AFTER }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "DEPOIS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        color = if (isAfter) Color.White else Color(0xFF6A6A70)
                                    )
                                    Text(
                                        text = "A partir da instalação",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isAfter) Color.White.copy(alpha = 0.9f) else Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    }

                    // 1. TOP HERO BAR CHART CARD (Clean container with real device usage bars)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(CmfWhite)
                                .border(1.dp, CmfBorder, RoundedCornerShape(24.dp))
                                .padding(top = 22.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val maxVal = (chartBars.maxOfOrNull { it.valueMinutes } ?: 60).coerceAtLeast(30)

                                if (isLoadingStats && chartBars.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = accentColor,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        chartBars.forEachIndexed { idx, barData ->
                                            val isSelected = idx == safeSelectedIndex
                                            val heightRatio = if (maxVal > 0) {
                                                (barData.valueMinutes.toFloat() / maxVal).coerceIn(0.08f, 1f)
                                            } else 0.08f

                                            val animatedBarColor by animateColorAsState(
                                                targetValue = if (isSelected) accentColor else Color(0xFF2B2B2E),
                                                label = "bar_color_$idx"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(if (chartBars.size > 14) 0.55f else 0.70f)
                                                        .fillMaxHeight(heightRatio)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(animatedBarColor)
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null,
                                                            onClick = { selectedBarIndex = idx }
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Date & Time detail label
                                Text(
                                    text = currentSelectedBar?.fullDateDesc ?: "SELECIONE UMA BARRA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = CmfDark,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // 2. PERIOD NAVIGATION BAR ( [ < ]  PERÍODO  [ > ] )
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF3F3F5))
                                .border(1.dp, Color(0xFFE2E2E6), RoundedCornerShape(16.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        periodOffset -= 1
                                        selectedBarIndex = 0
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Anterior",
                                        tint = CmfDark
                                    )
                                }

                                Text(
                                    text = periodNavigationTitle.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.5.sp,
                                    color = CmfDark
                                )

                                IconButton(
                                    onClick = {
                                        if (periodOffset < 0) {
                                            periodOffset += 1
                                            selectedBarIndex = 0
                                        }
                                    },
                                    enabled = periodOffset < 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Próximo",
                                        tint = if (periodOffset < 0) CmfDark else Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    // 2. PERIOD SELECTOR TABS (DIA | SEMANA | MÊS | ANO)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF3F3F5))
                                .border(1.dp, Color(0xFFE2E2E6), RoundedCornerShape(16.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatsPeriod.values().forEach { period ->
                                val isSelected = selectedPeriod == period
                                val tabBg by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF2B2B2E) else Color.Transparent,
                                    label = "period_tab_bg_${period.name}"
                                )
                                val tabText by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else Color(0xFF6A6A70),
                                    label = "period_tab_text_${period.name}"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tabBg)
                                        .clickable {
                                            selectedPeriod = period
                                            periodOffset = 0
                                            selectedBarIndex = when (period) {
                                                StatsPeriod.DAY -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                                StatsPeriod.WEEK -> 2
                                                StatsPeriod.MONTH -> (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1).coerceAtLeast(0)
                                                StatsPeriod.YEAR -> Calendar.getInstance().get(Calendar.MONTH)
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = period.enLabel.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp,
                                        color = tabText
                                    )
                                }
                            }
                        }
                    }

                    // 3. LIMITE DE TEMPO RESPEITADO (Gauge Circular Adaptada com Pontinho Preservado)
                    item {
                        val adherencePercent = (adherenceRatio * 100).toInt().coerceIn(0, 100)
                        val isWithinLimits = adherenceRatio >= 0.85f

                        val badgeLabel = when {
                            adherencePercent >= 95 -> "EXCELENTE"
                            adherencePercent >= 80 -> "DENTRO DO LIMITE"
                            adherencePercent >= 50 -> "ATENÇÃO"
                            else -> "LIMITE EXCEDIDO"
                        }

                        val badgeColor = when {
                            adherencePercent >= 80 -> Color(0xFF00C853)
                            adherencePercent >= 50 -> accentColor
                            else -> Color(0xFFD32F2F)
                        }

                        NothingCard(
                            title = "LIMITE DE TEMPO RESPEITADO",
                            badge = badgeLabel
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))

                                // Donut Gauge Container
                                Box(
                                    modifier = Modifier.size(154.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = adherenceRatio.coerceIn(0f, 1f),
                                        label = "donut_progress_anim"
                                    )

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val strokeWidth = 14.dp.toPx()
                                        val radius = (size.minDimension - strokeWidth) / 2f - 4f
                                        val outerRadius = radius + strokeWidth / 2f
                                        val innerRadius = radius - strokeWidth / 2f

                                        val trackColor = Color(0xFFE5E5EB)
                                        val outlineColor = Color(0xFFD6D6DE)

                                        // 1. Base Donut Track (full 360 ring)
                                        drawCircle(
                                            color = trackColor,
                                            radius = radius,
                                            center = center,
                                            style = Stroke(width = strokeWidth)
                                        )

                                        // 2. Active Progress Arc (clockwise from top -90 deg)
                                        val sweepAngle = (animatedProgress * 360f).coerceIn(0f, 360f)
                                        if (sweepAngle > 0f) {
                                            drawArc(
                                                color = accentColor,
                                                startAngle = -90f,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                                topLeft = Offset(center.x - radius, center.y - radius),
                                                size = Size(radius * 2f, radius * 2f)
                                            )
                                        }

                                        // 3. Crisp outer & inner hairline borders
                                        drawCircle(
                                            color = outlineColor,
                                            radius = outerRadius,
                                            center = center,
                                            style = Stroke(width = 1.2f)
                                        )
                                        drawCircle(
                                            color = outlineColor,
                                            radius = innerRadius,
                                            center = center,
                                            style = Stroke(width = 1.2f)
                                        )

                                        // 4. Clean inner hole fill
                                        drawCircle(
                                            color = Color.White,
                                            radius = innerRadius - 1f,
                                            center = center
                                        )

                                        // 5. THE "PONTINHO" (Preserved & Enhanced as user requested):
                                        val dotY = center.y - innerRadius * 0.52f
                                        drawCircle(
                                            color = accentColor.copy(alpha = 0.22f),
                                            radius = 6.dp.toPx(),
                                            center = Offset(center.x, dotY)
                                        )
                                        drawCircle(
                                            color = accentColor,
                                            radius = 3.8.dp.toPx(),
                                            center = Offset(center.x, dotY)
                                        )
                                    }

                                    // Center text inside donut hole
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "$adherencePercent",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = CmfDark,
                                                letterSpacing = (-1).sp
                                            )
                                            Text(
                                                text = "%",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = accentColor,
                                                modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = "RESPEITADO",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = CmfMutedText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Status pill & explanation
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isWithinLimits) "DISCIPLINA MANTIDA COM SUCESSO" else "ATENÇÃO AOS LIMITES DIÁRIOS",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = badgeColor,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isWithinLimits) {
                                        "Você manteve seus limites de uso em $adherencePercent% do período selecionado. Sem quebras de foco ou violações de regras."
                                    } else {
                                        "Seu tempo de uso ultrapassou a meta em alguns momentos. Evite usar o Desbloqueio de Confiança para manter sua sequência intacta."
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CmfMutedText,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    // 4. REAL DEVICE METRICS TILES
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMiniBox(
                                label = "TEMPO DE TELA",
                                value = "${totalDeviceMinutes / 60}h ${totalDeviceMinutes % 60}m",
                                subValue = "Uso real no aparelho",
                                icon = Icons.Default.Timer,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )

                            StatMiniBox(
                                label = "TEMPO POUPADO",
                                value = "${savedMinutes / 60}h ${savedMinutes % 60}m",
                                subValue = "Longe de distrações",
                                icon = Icons.Default.Shield,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 5. APPS BREAKDOWN (Real device usage)
                    item {
                        Text(
                            text = if (installFilterMode == InstallFilterMode.BEFORE) "USO DOS APPS ANTES DO LOCK IN:" else "USO DOS APPS A PARTIR DO LOCK IN:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfMutedText,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (topApps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CmfLightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!hasUsagePermission) "Permissão de uso pendente para listar apps." else "Nenhum aplicativo utilizado no período selecionado.",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CmfMutedText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(topApps.size) { index ->
                            val app = topApps[index]
                            val usedMin = app.foregroundMinutes
                            val progress = if (app.isRestricted && app.limitMinutes > 0) {
                                (usedMin.toFloat() / app.limitMinutes).coerceIn(0f, 1f)
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CmfWhite)
                                    .border(1.dp, CmfBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            AppIconImage(
                                                packageName = app.packageName,
                                                fallbackLetter = app.appName.firstOrNull()?.uppercase() ?: "A",
                                                sizeDp = 30.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = app.appName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CmfDark,
                                                    maxLines = 1
                                                )
                                                if (app.isRestricted) {
                                                    Text(
                                                        text = "MONITORADO • LIMITE ${app.limitMinutes}M",
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = accentColor
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (app.isRestricted && app.limitMinutes > 0) {
                                                "${usedMin}m / ${app.limitMinutes}m"
                                            } else {
                                                if (usedMin >= 60) "${usedMin / 60}h ${usedMin % 60}m" else "${usedMin} min"
                                            },
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (app.isRestricted && usedMin >= app.limitMinutes) accentColor else CmfDark
                                        )
                                    }

                                    if (app.isRestricted && app.limitMinutes > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(CmfLightGray)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progress)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (usedMin >= app.limitMinutes) accentColor else CmfDark)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }

            // AUTOMATIC STREAK DETAIL MODAL (No manual alterations allowed)
            if (showStreakModal) {
                StreakDetailModal(
                    streakDays = streakDays,
                    recordStreak = recordStreak,
                    accentColor = accentColor,
                    onDismiss = { showStreakModal = false }
                )
            }
        }
    }
}

@Composable
private fun StreakDetailModal(
    streakDays: Int,
    recordStreak: Int,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2E)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = streakDays.toString(),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "DIAS SEGUIDOS DE FOCO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CmfMutedText,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Streak squares matching EXACT count of streak days
                if (streakDays == 0) {
                    // Broken square display when streak is broken
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F7))
                            .border(1.dp, Color(0xFFE2E2E6), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2B2B2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                val w = size.width
                                val h = size.height
                                val crackPath = Path().apply {
                                    moveTo(w * 0.48f, 0f)
                                    lineTo(w * 0.36f, h * 0.32f)
                                    lineTo(w * 0.64f, h * 0.58f)
                                    lineTo(w * 0.42f, h * 0.82f)
                                    lineTo(w * 0.54f, h)
                                }
                                drawPath(
                                    path = crackPath,
                                    color = accentColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 3.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "QUADRADO QUEBRADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = accentColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "A sequência foi quebrada. Nenhum dia ativo no momento.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CmfMutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Exactly streakDays squares (e.g. 2 dias = 2 quadrados)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F7))
                            .border(1.dp, Color(0xFFE2E2E6), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DIAS SEGUIDOS CONCLUÍDOS ($streakDays)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CmfMutedText,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val columns = 7
                        val totalRows = (streakDays + columns - 1) / columns
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (r in 0 until totalRows) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (c in 0 until columns) {
                                        val dayIdx = r * columns + c
                                        if (dayIdx < streakDays) {
                                            val isCurrent = (dayIdx == streakDays - 1)
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(if (isCurrent) accentColor else Color(0xFF2B2B2E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${dayIdx + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1º DIA",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText
                            )
                            Text(
                                text = "RECORDE: $recordStreak DIAS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = "HOJE ($streakDays)",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CmfMutedText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clear informative badge explaining automatic progression and no manual cheat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFEFF2))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STREAK AUTOMÁTICO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CmfDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seu streak aumenta automaticamente a cada dia em que você respeita seus limites de foco sem utilizar o Desbloqueio de Confiança. Se você burlar com a senha de segurança, o streak zera automaticamente. Sem atalhos, apenas disciplina pura.",
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = CmfMutedText
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun StatMiniBox(
    label: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CmfWhite)
            .border(1.dp, CmfBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CmfMutedText
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = CmfDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 9.sp,
                color = CmfMutedText
            )
        }
    }
}
