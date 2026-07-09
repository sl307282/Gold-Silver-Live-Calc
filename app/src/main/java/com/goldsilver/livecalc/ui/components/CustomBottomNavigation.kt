package com.goldsilver.livecalc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.DarkSurface
import com.goldsilver.livecalc.ui.theme.DarkSurfaceElevated
import com.goldsilver.livecalc.ui.theme.GoldPrimary
import com.goldsilver.livecalc.ui.theme.TextMuted
import com.goldsilver.livecalc.ui.theme.TextPrimary
import com.goldsilver.livecalc.ui.theme.isSystemDarkThemeGlobal
import androidx.compose.material3.HorizontalDivider

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : NavigationItem("dashboard", "Rates", Icons.Default.Home)
    object Charts : NavigationItem("charts", "Trends", Icons.AutoMirrored.Filled.TrendingUp)
    object Hallmark : NavigationItem("hallmark", "Hallmark", Icons.Default.CheckCircle)
    object Settings : NavigationItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun CustomBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationItem.Dashboard,
        NavigationItem.Charts,
        NavigationItem.Hallmark,
        NavigationItem.Settings
    )

    Column(modifier = modifier) {
        HorizontalDivider(
            color = if (isSystemDarkThemeGlobal) Color(0xFF2C2C2C) else Color(0xFFE5E5EA),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1.0f, label = "scale")
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) GoldPrimary else (if (isSystemDarkThemeGlobal) Color(0xFF8E8E93) else Color(0xFF757575)), 
                    label = "color"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onNavigate(item.route)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = tintColor,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        color = tintColor,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

