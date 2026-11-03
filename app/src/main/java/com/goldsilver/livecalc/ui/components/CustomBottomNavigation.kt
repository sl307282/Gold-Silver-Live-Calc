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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.goldsilver.livecalc.ui.theme.GoldPrimary
import com.goldsilver.livecalc.util.LocalAppStrings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Event broadcast when an active bottom navigation tab is tapped/reselected.
 * Value emitted is the route string (e.g. "dashboard", "charts", "hallmark", "settings").
 */
val LocalTabReselectFlow = staticCompositionLocalOf<MutableSharedFlow<String>> {
    MutableSharedFlow()
}

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.88f
) {
    val tabReselectFlow = LocalTabReselectFlow.current
    val coroutineScope = rememberCoroutineScope()

    CustomBottomNavigationContent(
        currentRoute = currentRoute,
        onItemClick = { itemRoute ->
            if (currentRoute == itemRoute) {
                // User reselected the active tab -> broadcast scroll to top event
                coroutineScope.launch {
                    tabReselectFlow.emit(itemRoute)
                }
            } else {
                // Navigate to a new tab: do NOT save state or restore state, always open fresh at top
                navController.navigate(itemRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = false
                        inclusive = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        },
        modifier = modifier,
        backgroundAlpha = backgroundAlpha
    )
}

@Composable
fun CustomBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.88f
) {
    CustomBottomNavigationContent(
        currentRoute = currentRoute,
        onItemClick = onNavigate,
        modifier = modifier,
        backgroundAlpha = backgroundAlpha
    )
}

@Composable
private fun CustomBottomNavigationContent(
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.88f
) {
    val strings = LocalAppStrings.current

    val items = listOf(
        NavItem("dashboard", strings.tabRates, Icons.Default.Home),
        NavItem("charts", strings.tabTrends, Icons.AutoMirrored.Filled.TrendingUp),
        NavItem("hallmark", strings.tabHallmark, Icons.Default.CheckCircle),
        NavItem("settings", strings.tabSettings, Icons.Default.Settings)
    )

    val navBgColor = MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha)
    val navDividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = navBgColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            HorizontalDivider(
                color = navDividerColor,
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 1.0f, label = "scale")
                    val selectedColor = GoldPrimary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val tintColor by animateColorAsState(
                        targetValue = if (isSelected) selectedColor else inactiveColor, 
                        label = "color"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemClick(item.route)
                            }
                            .padding(vertical = 2.dp),
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            color = tintColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
