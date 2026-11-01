package com.goldsilver.livecalc.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldsilver.livecalc.ui.components.HistoryLineChart
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.util.LocalAppStrings
import com.goldsilver.livecalc.util.PriceConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsHistoryScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val historicalRates by viewModel.trendsHistoricalRates.collectAsStateWithLifecycle()
    val latestRate by viewModel.latestRate.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val isConvertedAtTodayRate by viewModel.isConvertedAtTodayRate.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var isGoldSelected by remember { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf("7D") } // 7D, 30D, 1Y

    // Smooth skeleton transition on period or metal switch
    var isSwitchingPeriod by remember { mutableStateOf(false) }
    LaunchedEffect(selectedRange, isGoldSelected, selectedCurrency) {
        isSwitchingPeriod = true
        kotlinx.coroutines.delay(180L)
        isSwitchingPeriod = false
    }

    // Build chart points: one per calendar day (closing = latest record that day),
    // then pin today's live rate as the final point so the chart always ends at the current price.
    val chartPoints = remember(historicalRates, latestRate, isGoldSelected, selectedRange) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Group all historical rows by date, pick the latest record per day (= closing rate)
        val dailyRates = historicalRates
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .map { (_, dayRates) -> dayRates.maxByOrNull { it.timestamp }!! }
            .sortedBy { it.timestamp }
            .toMutableList()

        // Replace or append today's entry with the live latest rate (most accurate current price)
        val todayStr = dateFormat.format(Date(System.currentTimeMillis()))
        latestRate?.let { live ->
            dailyRates.removeAll { dateFormat.format(Date(it.timestamp)) == todayStr }
            dailyRates.add(live)
            dailyRates.sortBy { it.timestamp }
        }

        val mapped = dailyRates.map { rate ->
            val price = if (isGoldSelected) rate.goldPrice24k else rate.silverPrice
            rate.timestamp to price
        }

        when (selectedRange) {
            "7D" -> mapped.takeLast(7)
            "30D" -> mapped.takeLast(30)
            else -> mapped // 1Y
        }
    }

    // Auto-sync rates and history when screen opens
    LaunchedEffect(Unit) {
        viewModel.refreshRates(force = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.historicalTrends,
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = GoldPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshRates(force = true) }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = strings.refresh,
                                tint = GoldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Metal Tab Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGoldSelected) GoldPrimary else Color.Transparent)
                            .clickable { isGoldSelected = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.goldRates,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isGoldSelected) DarkBackground else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isGoldSelected) SilverPrimary else Color.Transparent)
                            .clickable { isGoldSelected = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.silverRates,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (!isGoldSelected) DarkBackground else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Range Selectors
            item {
                val rangeOptions = listOf(
                    "7D" to strings.range7D,
                    "30D" to strings.range30D,
                    "1Y" to strings.range1Y
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rangeOptions.forEach { (rangeKey, rangeLabel) ->
                        val isRangeSelected = selectedRange == rangeKey
                        val activeColor = if (isGoldSelected) GoldPrimary else SilverPrimary
                        val isLocked = rangeKey == "1Y" && !isPremium

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRange = rangeKey }
                                .border(
                                    width = if (isRangeSelected) 1.dp else 0.dp,
                                    color = if (isRangeSelected) activeColor else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRangeSelected) activeColor.copy(alpha = 0.15f) else DarkSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rangeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isRangeSelected) activeColor else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isLocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = strings.premiumFeature,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trend Chart Container with Skeleton Loading Support
            item {
                val isRangeLocked = selectedRange == "1Y" && !isPremium

                if (isSwitchingPeriod && chartPoints.isNotEmpty()) {
                    TrendsSkeletonLoader()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                    ) {
                        if (isRangeLocked) {
                            // Blurred canvas representation
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(8.dp)
                            ) {
                                HistoryLineChart(
                                    points = chartPoints,
                                    isGold = isGoldSelected,
                                    currency = selectedCurrency,
                                    modifier = Modifier.fillMaxSize(),
                                    livePrice = if (isGoldSelected) latestRate?.goldPrice24k else latestRate?.silverPrice,
                                    isConvertedAtTodayRate = isConvertedAtTodayRate,
                                    selectedRange = selectedRange
                                )
                            }

                            // Overlay Paywall
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = strings.premiumFeature,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = strings.premiumFeature,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = strings.unlockTrendsDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    Button(
                                        onClick = { viewModel.setPremium(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                    ) {
                                        Text(
                                            text = strings.upgradeToPremium,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = DarkBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            HistoryLineChart(
                                points = chartPoints,
                                isGold = isGoldSelected,
                                currency = selectedCurrency,
                                modifier = Modifier.fillMaxSize(),
                                livePrice = if (isGoldSelected) latestRate?.goldPrice24k else latestRate?.silverPrice,
                                isConvertedAtTodayRate = isConvertedAtTodayRate,
                                selectedRange = selectedRange
                            )
                        }
                    }
                }
            }

            // Stat Summary Card
            item {
                if (chartPoints.isNotEmpty() && !isSwitchingPeriod) {
                    val prices = chartPoints.map { it.second }
                    val maxPrice = prices.maxOrNull() ?: 0.0
                    val minPrice = prices.minOrNull() ?: 0.0
                    val avgPrice = prices.average()
                    val metalName = if (isGoldSelected) strings.metalGold else strings.metalSilver

                    // Today's Change calculation
                    val todayPrice = if (isGoldSelected) latestRate?.goldPrice24k else latestRate?.silverPrice
                    val prevPrice = (if (isGoldSelected) latestRate?.prevTradingDayGoldPrice else latestRate?.prevTradingDaySilverPrice)
                        ?: if (chartPoints.size >= 2) chartPoints[chartPoints.size - 2].second else null

                    val todayDiff = if (todayPrice != null && prevPrice != null && prevPrice > 0.0) {
                        todayPrice - prevPrice
                    } else null
                    val todayPercent = if (todayDiff != null && prevPrice != null && prevPrice > 0.0) {
                        (todayDiff / prevPrice) * 100.0
                    } else null

                    // Period Change calculation (first point vs last point)
                    val firstPoint = chartPoints.firstOrNull()
                    val lastPoint = chartPoints.lastOrNull()
                    val periodDiff = if (firstPoint != null && lastPoint != null) {
                        lastPoint.second - firstPoint.second
                    } else null
                    val periodPercent = if (firstPoint != null && lastPoint != null && firstPoint.second > 0.0) {
                        ((lastPoint.second - firstPoint.second) / firstPoint.second) * 100.0
                    } else null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 0.5.dp,
                                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "$metalName ${strings.marketSummary} ($selectedRange)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                            // 1. Today's Change (e.g. "Today's Change ▼ ₹423.10 (2.98%) vs yesterday")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.todaysChange, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                if (todayDiff != null && todayPercent != null) {
                                    val formattedAmount = PriceConverter.formatAmount(kotlin.math.abs(todayDiff), selectedCurrency)
                                    val formattedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(todayPercent))
                                    when {
                                        todayDiff > 0.001 -> Text(
                                            text = "▲ $formattedAmount ($formattedPercent%) ${strings.vsYesterday}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        todayDiff < -0.001 -> Text(
                                            text = "▼ $formattedAmount ($formattedPercent%) ${strings.vsYesterday}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AccentRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        else -> Text(
                                            text = "0.00 (0.00%) ${strings.vsYesterday}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text("—", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 2. Period Change (first vs last point)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.periodChange, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                if (periodDiff != null && periodPercent != null) {
                                    val formattedAmount = PriceConverter.formatAmount(kotlin.math.abs(periodDiff), selectedCurrency)
                                    val formattedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(periodPercent))
                                    when {
                                        periodDiff > 0.001 -> Text(
                                            text = "▲ +$formattedAmount (+$formattedPercent%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        periodDiff < -0.001 -> Text(
                                            text = "▼ -$formattedAmount (-$formattedPercent%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AccentRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        else -> Text(
                                            text = "0.00 (0.00%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text("—", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 3. Range (High − Low in neutral white)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.range, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = PriceConverter.formatAmount(maxPrice - minPrice, selectedCurrency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 4. Highest Rate (Neutral white)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.highestRate, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = PriceConverter.format(maxPrice, selectedCurrency, perGram = true),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 5. Lowest Rate (Neutral white)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.lowestRate, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = PriceConverter.format(minPrice, selectedCurrency, perGram = true),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 6. Average Price (Neutral white)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.averagePrice, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = PriceConverter.format(avgPrice, selectedCurrency, perGram = true),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Daily Rates List
            item {
                if (chartPoints.isNotEmpty() && !isSwitchingPeriod) {
                    val reverseDailyList = chartPoints.asReversed()
                    val dailyDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 0.5.dp,
                                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${if (isGoldSelected) strings.metalGold else strings.metalSilver} ${strings.dailyRates}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                            // Header row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.dateHeader,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = strings.closeRateHeader,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1.3f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                                Text(
                                    text = strings.changeHeader,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1.0f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.04f))

                            // Rows for each day in selected period (reverse chronological)
                            reverseDailyList.forEachIndexed { revIdx, (timestamp, price) ->
                                val origIdx = chartPoints.size - 1 - revIdx
                                val prevPrice = if (origIdx > 0) chartPoints[origIdx - 1].second else null
                                val diff = prevPrice?.let { price - it }
                                val diffPercent = if (prevPrice != null && prevPrice > 0.0) {
                                    ((price - prevPrice) / prevPrice) * 100.0
                                } else null

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dailyDateFormat.format(Date(timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    Text(
                                        text = PriceConverter.format(price, selectedCurrency, perGram = true),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                    Box(
                                        modifier = Modifier.weight(1.0f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (diff != null && diffPercent != null) {
                                            val absPercentStr = String.format(Locale.US, "%.2f", kotlin.math.abs(diffPercent))
                                            when {
                                                diff > 0.001 -> Text(
                                                    text = "▲ +$absPercentStr%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AccentGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                diff < -0.001 -> Text(
                                                    text = "▼ $absPercentStr%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AccentRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                else -> Text(
                                                    text = "0.00%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "—",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                                if (revIdx < reverseDailyList.size - 1) {
                                    HorizontalDivider(
                                        color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TrendsSkeletonLoader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chart skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = alpha))
            )
        }

        // Summary skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}
