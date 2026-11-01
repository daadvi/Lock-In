package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.Screen
import com.example.ui.theme.CmfDark

data class DockNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
    val testTag: String
)

@Composable
fun AppBottomDock(
    currentScreen: Screen,
    accentColor: Color,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            DockNavItem(
                screen = Screen.DASHBOARD,
                icon = Icons.Default.Dashboard,
                label = "Início",
                testTag = "dock_nav_dashboard"
            ),
            DockNavItem(
                screen = Screen.FOCUS_TIME,
                icon = Icons.Default.Lock,
                label = "Bloqueio & Rotinas",
                testTag = "dock_nav_lock_routines"
            ),
            DockNavItem(
                screen = Screen.STATISTICS,
                icon = Icons.Default.BarChart,
                label = "Status",
                testTag = "dock_nav_status"
            ),
            DockNavItem(
                screen = Screen.FRIENDS_PANEL,
                icon = Icons.Default.Groups,
                label = "Friends Panel",
                testTag = "dock_nav_friends"
            ),
            DockNavItem(
                screen = Screen.APP_SELECTION,
                icon = Icons.Default.Apps,
                label = "Apps",
                testTag = "dock_nav_apps"
            ),
            DockNavItem(
                screen = Screen.SETTINGS,
                icon = Icons.Default.Settings,
                label = "Ajustes",
                testTag = "dock_nav_settings"
            )
        )
    }

    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val dockBg = if (isDark) Color(0xFF18181C) else Color(0xFFFFFFFF)
    val dockBorder = if (isDark) Color(0xFF2C2C34) else Color(0xFFE2E2E6)
    val selectedPillBg = if (isDark) Color(0xFF2E2E36) else CmfDark
    val unselectedIconColor = if (isDark) Color(0xFF9E9EA8) else Color(0xFF71717A)

    // Outer floating pill dock with rounded corners and subtle shadow/border
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = if (isDark) Color(0x33000000) else Color(0x14000000),
                spotColor = if (isDark) Color(0x4D000000) else Color(0x1F000000)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(dockBg)
            .border(1.dp, dockBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items.forEach { item ->
                val isSelected = when (item.screen) {
                    Screen.DASHBOARD -> currentScreen == Screen.DASHBOARD
                    Screen.FOCUS_TIME -> currentScreen == Screen.FOCUS_TIME
                    Screen.STATISTICS -> currentScreen == Screen.STATISTICS
                    Screen.FRIENDS_PANEL -> currentScreen == Screen.FRIENDS_PANEL
                    Screen.APP_SELECTION -> currentScreen == Screen.APP_SELECTION || currentScreen == Screen.CLOCK_LIMIT || currentScreen == Screen.WEB_LINKS
                    Screen.SETTINGS -> currentScreen == Screen.SETTINGS || currentScreen == Screen.STYLING || currentScreen == Screen.RESET_SETTINGS || currentScreen == Screen.PERMISSIONS
                    else -> currentScreen == item.screen
                }

                val animatedPillWidth by animateDpAsState(
                    targetValue = if (isSelected) 50.dp else 40.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "dock_pill_width_${item.screen}"
                )

                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) selectedPillBg else Color.Transparent,
                    label = "dock_pill_bg_${item.screen}"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else unselectedIconColor,
                    label = "dock_pill_tint_${item.screen}"
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .width(animatedPillWidth)
                        .clip(RoundedCornerShape(22.dp))
                        .background(animatedBgColor)
                        .clickable { onNavigate(item.screen) }
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
