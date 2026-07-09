package com.goldsilver.livecalc.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.components.PriceChangeIndicator
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: GoldSilverViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestRate by viewModel.latestRate.collectAsState()
    val historicalRates by viewModel.historicalRates.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    // Determine change percentages
    val goldChange = remember(latestRate, historicalRates) {
        if (historicalRates.size >= 2 && latestRate != null) {
            val prev = historicalRates[historicalRates.size - 2].goldPrice24k
            val curr = latestRate!!.goldPrice24k
            ((curr - prev) / prev) * 100
        } else {
            0.18 // default mock positive fluctuation
        }
    }

    val silverChange = remember(latestRate, historicalRates) {
        if (historicalRates.size >= 2 && latestRate != null) {
            val prev = historicalRates[historicalRates.size - 2].silverPrice
            val curr = latestRate!!.silverPrice
            ((curr - prev) / prev) * 100
        } else {
            -0.12 // default mock negative fluctuation
        }
    }

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
                    Text("Live Rates", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.refreshRates(force = true) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text("Updated • $lastUpdatedText", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
                // Reduced top spacer
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }

            // Live Gold Card
            item {
                latestRate?.let { rate ->
                    MetalRateCard(
                        title = "GOLD (XAU)",
                        primaryPrice = String.format("%.2f %s/g", rate.goldPrice24k, currency),
                        changePercent = goldChange,
                        isGold = true,
                        purityList = listOf(
                            "24K (Pure 99.9%)" to rate.goldPrice24k,
                            "22K (Jewelry 91.6%)" to rate.goldPrice22k,
                            "18K (Standard 75.0%)" to rate.goldPrice18k,
                            "14K (Economy 58.3%)" to rate.goldPrice14k
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
                        title = "SILVER (XAG)",
                        primaryPrice = String.format("%.2f %s/g", rate.silverPrice, currency),
                        changePercent = silverChange,
                        isGold = false,
                        purityList = listOf(
                            "1 Gram" to rate.silverPrice,
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
                    text = "QUICK TOOLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            title = "Gold Calculator",
                            icon = Icons.Filled.Calculate,
                            color = GoldPrimary,
                            onClick = { onNavigate("gold_calc") },
                            modifier = Modifier.weight(1f),
                            backgroundBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD573), // Bright Gold
                                    Color(0xFFE5A93B)  // Rich Gold
                                )
                            ),
                            textColor = Color(0xFF1E1E1E),
                            borderColor = Color(0xFFE5A93B).copy(alpha = 0.5f)
                        )
                        QuickActionButton(
                            title = "Silver Calculator",
                            icon = Icons.Filled.Calculate,
                            color = SilverPrimary,
                            onClick = { onNavigate("silver_calc") },
                            modifier = Modifier.weight(1f),
                            backgroundBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE2E8F0), // Light Silver
                                    Color(0xFF94A3B8)  // Medium Silver
                                )
                            ),
                            textColor = Color(0xFF1E1E1E),
                            borderColor = Color(0xFF94A3B8).copy(alpha = 0.5f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            title = "Verify Hallmark",
                            icon = Icons.Filled.CheckCircle,
                            color = Color(0xFF4CAF50), // Green accent
                            onClick = { onNavigate("hallmark") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "Price Alerts",
                            icon = Icons.Filled.Notifications,
                            color = Color(0xFF2196F3), // Blue accent
                            onClick = { onNavigate("alerts") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Premium Paywall Card removed
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
    changePercent: Double,
    isGold: Boolean,
    purityList: List<Pair<String, Double>>,
    currency: String,
    modifier: Modifier = Modifier
) {
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
                    Text(
                        text = if (currency == "INR") "(Indian Market)" else "(International)",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
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
                            text = String.format(Locale.getDefault(), "%.2f %s", value, currency),
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
                        text = if (isExpanded) "See less" else "See more",
                        color = themeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "See less" else "See more",
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
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundBrush: Brush? = null,
    textColor: Color = TextPrimary,
    borderColor: Color? = null
) {
    val cardElevation = if (!isSystemDarkThemeGlobal) 1.5.dp else 0.dp
    val defaultBorderColor = if (isSystemDarkThemeGlobal) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val finalBorderColor = borderColor ?: defaultBorderColor

    Card(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                width = 0.5.dp,
                color = finalBorderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = if (backgroundBrush != null) Color.Transparent else DarkSurface)
    ) {
        val rowModifier = if (backgroundBrush != null) {
            Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(horizontal = 12.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        }
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (backgroundBrush != null) {
                            Color.Black.copy(alpha = 0.12f)
                        } else {
                            color.copy(alpha = if (isSystemDarkThemeGlobal) 0.15f else 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (backgroundBrush != null) Color(0xFF1E1E1E) else color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }
    }
}
