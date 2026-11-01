package com.goldsilver.livecalc.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.components.PriceChangeIndicator
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.util.CurrencyFormatter
import com.goldsilver.livecalc.util.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: GoldSilverViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val latestRate by viewModel.latestRate.collectAsStateWithLifecycle()
    val previousRate by viewModel.previousTradingDayRate.collectAsStateWithLifecycle()
    val goldChange by viewModel.goldChangePercent.collectAsStateWithLifecycle()
    val silverChange by viewModel.silverChangePercent.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val lastUpdatedText = remember(latestRate) {
        latestRate?.let {
            val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
            sdf.format(Date(it.timestamp))
        } ?: "..."
    }

    Scaffold(
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Compact Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings.liveRates, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.refreshRates(force = true) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text("${strings.lastUpdated} • $lastUpdatedText", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = GoldPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = strings.refresh,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Live Gold Card
                item {
                    latestRate?.let { rate ->
                        val prevGoldPrice = rate.prevTradingDayGoldPrice ?: previousRate?.goldPrice24k
                        val goldDiffAmount = if (prevGoldPrice != null && prevGoldPrice > 0) rate.goldPrice24k - prevGoldPrice else null

                        MetalRateCard(
                            title = "Gold (XAU)",
                            primaryPrice = "${CurrencyFormatter.formatCurrency(rate.goldPrice24k, currency)} / g",
                            headlineFigure = "10 g · ${CurrencyFormatter.formatCurrency(rate.goldPrice24k * 10, currency)}",
                            changePercent = goldChange,
                            diffAmount = goldDiffAmount,
                            currency = currency,
                            isGold = true,
                            purityList = listOf(
                                "24K (99.9%)" to rate.goldPrice24k,
                                "22K (91.6%)" to rate.goldPrice22k,
                                "18K (75.0%)" to rate.goldPrice18k,
                                "14K (58.3%)" to rate.goldPrice14k
                            ),
                            weightList = listOf(
                                "1 g" to rate.goldPrice24k,
                                "10 g" to rate.goldPrice24k * 10,
                                "1 tola (11.66 g)" to rate.goldPrice24k * 11.6638,
                                "1 kg" to rate.goldPrice24k * 1000
                            ),
                            onCardClick = { onNavigate("gold_calc") }
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldPrimary)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Live Silver Card
                item {
                    latestRate?.let { rate ->
                        val prevSilverPrice = rate.prevTradingDaySilverPrice ?: previousRate?.silverPrice
                        val silverDiffAmount = if (prevSilverPrice != null && prevSilverPrice > 0) rate.silverPrice - prevSilverPrice else null

                        MetalRateCard(
                            title = "Silver (XAG)",
                            primaryPrice = "${CurrencyFormatter.formatCurrency(rate.silverPrice, currency)} / g",
                            headlineFigure = "1 kg · ${CurrencyFormatter.formatCurrency(rate.silverPrice * 1000, currency)}",
                            changePercent = silverChange,
                            diffAmount = silverDiffAmount,
                            currency = currency,
                            isGold = false,
                            purityList = listOf(
                                "999" to rate.silverPrice,
                                "925 (Sterling)" to rate.silverPrice * 0.925
                            ),
                            weightList = listOf(
                                "1 g" to rate.silverPrice,
                                "10 g" to rate.silverPrice * 10,
                                "1 tola (11.66 g)" to rate.silverPrice * 11.6638,
                                "1 kg" to rate.silverPrice * 1000
                            ),
                            onCardClick = { onNavigate("silver_calc") }
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SilverPrimary)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Navigation / Action Buttons Grid
                item {
                    Text(
                        text = strings.quickCalculator.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Gold Calculator",
                                subtitle = "Calculate price",
                                icon = Icons.Filled.Calculate,
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD770), // Radiant Gold
                                        Color(0xFFE5A93B)  // Deep Luxury Gold
                                    )
                                ),
                                borderColor = Color(0xFFF59E0B),
                                titleColor = Color(0xFF1A1404),
                                subtitleColor = Color(0xFF6B4702),
                                iconColor = Color(0xFF1A1404),
                                onClick = { onNavigate("gold_calc") },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Silver Calculator",
                                subtitle = "Calculate price",
                                icon = Icons.Filled.Calculate,
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF1F5F9), // Radiant Silver White
                                        Color(0xFFCBD5E1)  // Smooth Platinum Silver
                                    )
                                ),
                                borderColor = Color(0xFF94A3B8),
                                titleColor = Color(0xFF0F172A),
                                subtitleColor = Color(0xFF475569),
                                iconColor = Color(0xFF0F172A),
                                onClick = { onNavigate("silver_calc") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Hallmark",
                                subtitle = "Verify purity",
                                icon = Icons.Filled.CheckCircle,
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFA7F3D0), // Mint Green
                                        Color(0xFF34D399)  // Emerald Green
                                    )
                                ),
                                borderColor = Color(0xFF10B981),
                                titleColor = Color(0xFF064E3B),
                                subtitleColor = Color(0xFF065F46),
                                iconColor = Color(0xFF064E3B),
                                onClick = { onNavigate("hallmark") },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Alerts",
                                subtitle = "Price alerts",
                                icon = Icons.Filled.Notifications,
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFBAE6FD), // Sky Blue
                                        Color(0xFF38BDF8)  // Sapphire Blue
                                    )
                                ),
                                borderColor = Color(0xFF0284C7),
                                titleColor = Color(0xFF0C4A6E),
                                subtitleColor = Color(0xFF075985),
                                iconColor = Color(0xFF0C4A6E),
                                onClick = { onNavigate("alerts") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MetalRateCard(
    title: String,
    primaryPrice: String,
    headlineFigure: String? = null,
    changePercent: Double?,
    diffAmount: Double?,
    currency: String = "INR",
    isGold: Boolean,
    purityList: List<Pair<String, Double>>,
    weightList: List<Pair<String, Double>>,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "chevronRotation"
    )

    val cardBrush = if (isSystemDarkThemeGlobal) {
        if (isGold) {
            Brush.linearGradient(colors = listOf(DarkSurface, Color(0xFF221E14)))
        } else {
            Brush.linearGradient(colors = listOf(DarkSurface, Color(0xFF1E2224)))
        }
    } else {
        if (isGold) {
            Brush.linearGradient(colors = listOf(Color(0xFFFFFDF0), Color(0xFFFFF9E6)))
        } else {
            Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)))
        }
    }

    val themeColor = if (isGold) GoldPrimary else SilverPrimary

    val cardElevation = if (!isSystemDarkThemeGlobal) {
        if (isGold) 3.dp else 1.dp
    } else {
        0.dp
    }

    val cardBorderColor = if (isSystemDarkThemeGlobal) {
        themeColor.copy(alpha = 0.3f)
    } else {
        if (isGold) {
            GoldPrimary.copy(alpha = 0.35f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = 0.5.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(brush = cardBrush)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = themeColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(themeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = strings.spot,
                            color = themeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                PriceChangeIndicator(changePercent = changePercent)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = primaryPrice,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (!isExpanded && headlineFigure != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = headlineFigure,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (diffAmount != null && changePercent != null) {
                val isUp = diffAmount > 0.001
                val isDown = diffAmount < -0.001
                val arrow = if (isUp) "▲" else if (isDown) "▼" else "—"
                val color = if (isUp) Color(0xFF4ADE80) else if (isDown) Color(0xFFFF6B6B) else TextMuted
                val absDiff = CurrencyFormatter.formatCurrency(Math.abs(diffAmount), currency)
                val pctStr = String.format(Locale.US, "%.2f", Math.abs(changePercent))

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$arrow $absDiff ($pctStr%) ${strings.vsYesterday}",
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))

                // Section 1: PURITY
                Text(
                    text = strings.sectionPurity,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                purityList.forEachIndexed { index, (label, value) ->
                    BreakdownItemRow(label = label, value = value, currency = currency, isFirst = index == 0)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: WEIGHT
                Text(
                    text = strings.sectionWeight,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                weightList.forEachIndexed { index, (label, value) ->
                    BreakdownItemRow(label = label, value = value, currency = currency, isFirst = index == 0)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand / Collapse Full-width Text Button with Ripple and Rotating Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) strings.showLess else strings.showBreakdown,
                    color = themeColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }
        }
    }
}

@Composable
private fun BreakdownItemRow(label: String, value: Double, currency: String, isFirst: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isFirst) TextPrimary else TextSecondary.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = CurrencyFormatter.formatCurrency(value, currency),
            color = if (isFirst) TextPrimary else TextPrimary.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundBrush: Brush,
    borderColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardHeight = 72.dp
    val iconBadgeSize = 34.dp
    val iconSize = 18.dp
    val chevronSize = 14.dp

    Card(
        modifier = modifier
            .height(cardHeight)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(iconBadgeSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = subtitleColor.copy(alpha = 0.8f),
                modifier = Modifier.size(chevronSize)
            )
        }
    }
}
