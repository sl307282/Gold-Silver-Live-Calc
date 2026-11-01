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
import com.goldsilver.livecalc.data.local.entities.RateEntity
import com.goldsilver.livecalc.ui.components.HistoryChartPoint
import com.goldsilver.livecalc.ui.components.HistoryLineChart
import com.goldsilver.livecalc.ui.components.PriceRangeBar
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.ui.viewmodel.Period
import com.goldsilver.livecalc.util.LocalAppStrings
import com.goldsilver.livecalc.util.PriceConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsHistoryScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historicalRates by viewModel.trendsHistoricalRates.collectAsStateWithLifecycle()
    val latestRate by viewModel.latestRate.collectAsStateWithLifecycle()
    val previousRate by viewModel.previousTradingDayRate.collectAsStateWithLifecycle()
    val goldChange by viewModel.goldChangePercent.collectAsStateWithLifecycle()
    val silverChange by viewModel.silverChangePercent.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val isConvertedAtTodayRate by viewModel.isConvertedAtTodayRate.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var isGoldSelected by remember { mutableStateOf(true) }
    val selectedRange = when (uiState.selectedPeriod) {
        Period.D7 -> "7D"
        Period.D30 -> "30D"
        Period.Y1 -> "1Y"
    }

    // Dedupe the series strictly by calendar date: keep latest record per day;
    // append/replace today's entry with latest live rate, and label final point "Today"
    val chartPoints = remember(historicalRates, latestRate, isGoldSelected, selectedRange) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val todayStr = dateFormat.format(Date())

        // Map calendar date key -> latest record of that day
        val dailyMap = mutableMapOf<String, RateEntity>()
        historicalRates.forEach { rate ->
            val key = if (!rate.date.isNullOrBlank() && rate.date.length == 10) {
                rate.date
            } else {
                dateFormat.format(Date(rate.timestamp))
            }
            val existing = dailyMap[key]
            if (existing == null || rate.timestamp >= existing.timestamp) {
                dailyMap[key] = rate
            }
        }

        // Replace or insert today's entry with live rate from repository
        var hasLiveToday = false
        latestRate?.let { live ->
            val liveKey = if (!live.date.isNullOrBlank() && live.date.length == 10) {
                live.date
            } else {
                todayStr
            }
            dailyMap[liveKey] = live
            hasLiveToday = (liveKey == todayStr)
        }

        // Chronologically sorted strictly unique calendar days
        val sortedRates = dailyMap.toSortedMap().values.toList()

        val periodRates = when (selectedRange) {
            "7D" -> sortedRates.takeLast(7)
            "30D" -> sortedRates.takeLast(30)
            else -> sortedRates // 1Y
        }

        periodRates.mapIndexed { idx, rate ->
            val price = if (isGoldSelected) rate.goldPrice24k else rate.silverPrice
            val key = if (!rate.date.isNullOrBlank() && rate.date.length == 10) {
                rate.date
            } else {
                dateFormat.format(Date(rate.timestamp))
            }
            val isFinalPoint = (idx == periodRates.size - 1)
            val isPointToday = (key == todayStr) || (isFinalPoint && hasLiveToday)

            HistoryChartPoint(
                timestamp = rate.timestamp,
                price = price,
                dateKey = key,
                isToday = isPointToday
            )
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
            contentPadding = PaddingValues(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gold / Silver Segmented Toggle (Restored above period chips)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isGoldSelected) GoldPrimary else Color.Transparent)
                            .clickable { isGoldSelected = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.metalGold,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isGoldSelected) DarkBackground else TextSecondary,
                            fontWeight = if (isGoldSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isGoldSelected) SilverPrimary else Color.Transparent)
                            .clickable { isGoldSelected = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.metalSilver,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (!isGoldSelected) DarkBackground else TextSecondary,
                            fontWeight = if (!isGoldSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Range Selectors (7D, 30D, 1Y)
            item {
                val rangeOptions = listOf(
                    Triple("7D", strings.range7D, Period.D7),
                    Triple("30D", strings.range30D, Period.D30),
                    Triple("1Y", strings.range1Y, Period.Y1)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rangeOptions.forEach { (rangeKey, rangeLabel, period) ->
                        val isRangeSelected = selectedRange == rangeKey
                        val activeColor = if (isGoldSelected) GoldPrimary else SilverPrimary
                        val isLocked = rangeKey == "1Y" && !isPremium

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setPeriod(period) }
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

            // Trend Chart Container
            item {
                val isRangeLocked = selectedRange == "1Y" && !isPremium

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

            // Stat Summary Card (Title follows the Gold / Silver toggle)
            item {
                if (chartPoints.isNotEmpty()) {
                    val prices = chartPoints.map { it.price }
                    val maxPrice = prices.maxOrNull() ?: 0.0
                    val minPrice = prices.minOrNull() ?: 0.0
                    val avgPrice = prices.average()
                    val metalName = if (isGoldSelected) strings.metalGold else strings.metalSilver

                    // Today's Change = currentRate − previousClose from the rates repository (same source as the home screen)
                    val currentRate = if (isGoldSelected) latestRate?.goldPrice24k else latestRate?.silverPrice
                    val previousClose = if (isGoldSelected) {
                        latestRate?.prevTradingDayGoldPrice ?: previousRate?.goldPrice24k
                    } else {
                        latestRate?.prevTradingDaySilverPrice ?: previousRate?.silverPrice
                    }
                    val todayChangePercent = if (isGoldSelected) goldChange else silverChange

                    val todayDiff = if (currentRate != null && previousClose != null && previousClose > 0.0) {
                        currentRate - previousClose
                    } else null
                    val todayPercent = todayChangePercent ?: if (todayDiff != null && previousClose != null && previousClose > 0.0) {
                        (todayDiff / previousClose) * 100.0
                    } else null

                    // Change (first vs last point)
                    val firstPoint = chartPoints.firstOrNull()
                    val lastPoint = chartPoints.lastOrNull()
                    val periodDiff = if (firstPoint != null && lastPoint != null) {
                        lastPoint.price - firstPoint.price
                    } else null
                    val periodPercent = if (firstPoint != null && lastPoint != null && firstPoint.price > 0.0) {
                        ((lastPoint.price - firstPoint.price) / firstPoint.price) * 100.0
                    } else null

                    val startDateFormat = if (selectedRange == "1Y") {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    } else {
                        SimpleDateFormat("dd MMM", Locale.getDefault())
                    }
                    val changeHint = if (firstPoint != null) {
                        "From ${startDateFormat.format(Date(firstPoint.timestamp))} to today"
                    } else ""

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

                            // 1. Today's Change: arrow + unsigned amount + unsigned %: "▼ ₹422.92 (2.98%)"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.todaysChange, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                if (todayDiff != null && todayPercent != null) {
                                    val isUp = todayDiff > 0.001
                                    val isDown = todayDiff < -0.001
                                    val arrow = if (isUp) "▲" else if (isDown) "▼" else ""
                                    val color = if (isUp) AccentGreen else if (isDown) AccentRed else TextMuted
                                    val unsignedAmount = PriceConverter.formatAbsolute(todayDiff, selectedCurrency)
                                    val unsignedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(todayPercent))

                                    Text(
                                        text = "$arrow $unsignedAmount ($unsignedPercent%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text("—", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 2. Dynamic Change row: label + hint on the left, arrow + unsigned amount with / g + unsigned % on the right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.resolvedLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    if (changeHint.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = changeHint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                                if (periodDiff != null && periodPercent != null) {
                                    val isPeriodUp = periodDiff > 0.001
                                    val isPeriodDown = periodDiff < -0.001
                                    val arrow = if (isPeriodUp) "▲" else if (isPeriodDown) "▼" else ""
                                    val color = if (isPeriodUp) AccentGreen else if (isPeriodDown) AccentRed else TextMuted
                                    val unsignedAmount = PriceConverter.format(kotlin.math.abs(periodDiff), selectedCurrency, perGram = true)
                                    val unsignedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(periodPercent))

                                    Text(
                                        text = "$arrow $unsignedAmount ($unsignedPercent%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text("—", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }

                            // Takeaway sentence below the change row
                            if (periodDiff != null && periodPercent != null) {
                                val isPeriodUp = periodDiff > 0.001
                                val isPeriodDown = periodDiff < -0.001
                                val arrow = if (isPeriodUp) "▲" else if (isPeriodDown) "▼" else ""
                                val color = if (isPeriodUp) AccentGreen else if (isPeriodDown) AccentRed else TextMuted
                                val unsignedAmount = PriceConverter.format(kotlin.math.abs(periodDiff), selectedCurrency, perGram = true)
                                val unsignedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(periodPercent))

                                val takeawaySentence = if (isPeriodUp) {
                                    "$metalName is up $unsignedPercent% ${uiState.takeawaySpan} ($arrow $unsignedAmount)"
                                } else if (isPeriodDown) {
                                    "$metalName is down $unsignedPercent% ${uiState.takeawaySpan} ($arrow $unsignedAmount)"
                                } else {
                                    "$metalName price remained steady ${uiState.takeawaySpan}"
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = takeawaySentence,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (!isPeriodUp && !isPeriodDown) TextSecondary else color,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 3. Range labelled "Range (High − Low)" in neutral white with / g
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.range, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = PriceConverter.format(maxPrice - minPrice, selectedCurrency, perGram = true),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            PriceRangeBar(
                                low = minPrice,
                                high = maxPrice,
                                current = currentRate ?: chartPoints.lastOrNull()?.price ?: 0.0,
                                periodLabel = when (uiState.selectedPeriod) {
                                    Period.D7 -> "7-day"
                                    Period.D30 -> "30-day"
                                    Period.Y1 -> "1-year"
                                },
                                formatter = { price -> PriceConverter.formatAmount(price, selectedCurrency) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))

                            // 4. Highest Rate (Neutral white with / g)
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

                            // 5. Lowest Rate (Neutral white with / g)
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

                            // 6. Average Price (Neutral white with / g)
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

            // Daily Rates List (Renders below summary with bottom-nav padding)
            item {
                if (chartPoints.isNotEmpty()) {
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
                            reverseDailyList.forEachIndexed { revIdx, pt ->
                                val origIdx = chartPoints.size - 1 - revIdx
                                val prevPrice = if (origIdx > 0) chartPoints[origIdx - 1].price else null
                                val diff = prevPrice?.let { pt.price - it }
                                val diffPercent = if (prevPrice != null && prevPrice > 0.0) {
                                    ((pt.price - prevPrice) / prevPrice) * 100.0
                                } else null

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dateStr = dailyDateFormat.format(Date(pt.timestamp))
                                    Text(
                                        text = if (pt.isToday) "${strings.today} • $dateStr" else dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (pt.isToday) (if (isGoldSelected) GoldPrimary else SilverPrimary) else TextSecondary,
                                        fontWeight = if (pt.isToday) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    Text(
                                        text = PriceConverter.format(pt.price, selectedCurrency, perGram = true),
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
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
