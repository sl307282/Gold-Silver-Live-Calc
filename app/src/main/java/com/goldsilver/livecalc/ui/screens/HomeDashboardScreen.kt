package com.goldsilver.livecalc.ui.screens

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

    // Refresh rotation animation
    val transition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

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
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = strings.refresh,
                            tint = GoldPrimary,
                            modifier = Modifier
                                .size(14.dp)
                                .run { if (isRefreshing) rotate(rotationAngle) else this }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Live Gold Card
                item {
                    latestRate?.let { rate ->
                        MetalRateCard(
                            title = "${strings.goldRates} (XAU)",
                            primaryPrice = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(rate.goldPrice24k)} $currency${strings.perGram}",
                            changePercent = goldChange,
                            isGold = true,
                            purityList = listOf(
                                strings.purity24kDesc to rate.goldPrice24k,
                                strings.purity22kDesc to rate.goldPrice22k,
                                strings.purity18kDesc to rate.goldPrice18k,
                                strings.purity14kDesc to rate.goldPrice14k
                            ),
                            currency = currency
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Live Silver Card
                item {
                    latestRate?.let { rate ->
                        MetalRateCard(
                            title = "${strings.silverRates} (XAG)",
                            primaryPrice = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(rate.silverPrice)} $currency${strings.perGram}",
                            changePercent = silverChange,
                            isGold = false,
                            purityList = listOf(
                                "1 ${strings.weightInGrams.split(" ").first()}" to rate.silverPrice,
                                "1 Tola (11.66g)" to rate.silverPrice * 11.6638,
                                "1 Kilogram" to rate.silverPrice * 1000
                            ),
                            currency = currency
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
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Navigation / Action Buttons Grid
                item {
                    Text(
                        text = strings.quickCalculator.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionButton(
                                title = strings.goldCalculator,
                                subtitle = strings.calculatePrice,
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
                                modifier = Modifier.weight(1f),
                                isLarge = true
                            )
                            QuickActionButton(
                                title = strings.silverCalculator,
                                subtitle = strings.calculatePrice,
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
                                modifier = Modifier.weight(1f),
                                isLarge = true
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionButton(
                                title = strings.tabHallmark,
                                subtitle = strings.hallmarkBannerSubtitle,
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
                                modifier = Modifier.weight(1f),
                                isLarge = false
                            )
                            QuickActionButton(
                                title = strings.tabAlerts,
                                subtitle = strings.priceAlertsBannerSubtitle,
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
                                modifier = Modifier.weight(1f),
                                isLarge = false
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
    changePercent: Double?,
    isGold: Boolean,
    purityList: List<Pair<String, Double>>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var isExpanded by remember { mutableStateOf(false) }

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
            .border(
                width = 0.5.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(16.dp)
            ),
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
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
                            fontSize = 9.sp,
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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Sub rates
                purityList.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, color = TextSecondary, fontSize = 13.sp)
                        Text(
                            text = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(value)} $currency",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Expand/Collapse Toggle Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        color = themeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
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
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val cardHeight = if (isLarge) 78.dp else 65.dp
    val iconBadgeSize = if (isLarge) 42.dp else 36.dp
    val iconSize = if (isLarge) 22.dp else 18.dp
    val titleSize = if (isLarge) 14.5.sp else 12.5.sp
    val subtitleSize = if (isLarge) 10.5.sp else 9.5.sp
    val chevronSize = if (isLarge) 18.dp else 16.dp

    Card(
        modifier = modifier
            .height(cardHeight)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLarge) 3.5.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(horizontal = if (isLarge) 12.dp else 10.dp, vertical = if (isLarge) 8.dp else 6.dp),
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

            Spacer(modifier = Modifier.width(if (isLarge) 10.dp else 8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = titleSize,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = subtitleSize,
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
