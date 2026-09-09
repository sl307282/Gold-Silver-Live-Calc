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
import com.goldsilver.livecalc.ui.theme.GoldPrimary
import com.goldsilver.livecalc.ui.theme.isSystemDarkThemeGlobal
import com.goldsilver.livecalc.util.LocalAppStrings
import androidx.compose.material3.HorizontalDivider

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun CustomBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    val items = listOf(
        NavItem("dashboard", strings.tabRates, Icons.Default.Home),
        NavItem("charts", strings.tabTrends, Icons.AutoMirrored.Filled.TrendingUp),
        NavItem("hallmark", strings.tabHallmark, Icons.Default.CheckCircle),
        NavItem("settings", strings.tabSettings, Icons.Default.Settings)
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
                val selectedColor = if (item.route == "hallmark") Color(0xFF10B981) else GoldPrimary
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) selectedColor else (if (isSystemDarkThemeGlobal) Color(0xFF8E8E93) else Color(0xFF757575)), 
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
