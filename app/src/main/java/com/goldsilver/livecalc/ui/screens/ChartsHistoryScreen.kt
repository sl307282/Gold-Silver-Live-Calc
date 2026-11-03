package com.goldsilver.livecalc.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.goldsilver.livecalc.ui.components.LocalTabReselectFlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
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
import androidx.compose.ui.res.stringResource
import com.goldsilver.livecalc.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp =
    (start.value + (stop.value - start.value) * fraction).dp

private fun lerpSp(start: Float, stop: Float, fraction: Float): TextUnit =
    (start + (stop - start) * fraction).sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsHistoryScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBarPadding: Dp = 80.dp
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

    var showAllWeeklyRows by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedRange) {
        showAllWeeklyRows = false
    }

    val is1Y = selectedRange == "1Y"

    val weeklyCloses = remember(chartPoints) {
        val cal = Calendar.getInstance(TimeZone.getDefault(), Locale.US)
        val weekMap = linkedMapOf<String, HistoryChartPoint>()
        chartPoints.forEach { pt ->
            cal.timeInMillis = pt.timestamp
            val year = cal.get(Calendar.YEAR)
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            val month = cal.get(Calendar.MONTH)
            val adjustedYear = if (month == Calendar.DECEMBER && week == 1) {
                year + 1
            } else if (month == Calendar.JANUARY && week >= 52) {
                year - 1
            } else {
                year
            }
            val key = "$adjustedYear-$week"
            weekMap[key] = pt
        }
        weekMap.values.toList()
    }

    val chronologicalList = remember(is1Y, chartPoints, weeklyCloses) {
        if (is1Y) weeklyCloses else chartPoints
    }
    val fullRatesList = remember(chronologicalList) {
        chronologicalList.asReversed()
    }
    val displayRatesList = remember(is1Y, fullRatesList, showAllWeeklyRows) {
        if (is1Y && !showAllWeeklyRows) fullRatesList.take(30) else fullRatesList
    }
    val hasShowMore = is1Y && fullRatesList.size > 30 && !showAllWeeklyRows

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val density = LocalDensity.current
    val collapseRangeDp = 68.dp
    val collapseRangePx = with(density) { collapseRangeDp.toPx() }

    var collapseOffset by remember { mutableFloatStateOf(0f) }
    val collapseFraction by remember {
        derivedStateOf {
            if (collapseRangePx > 0f) (collapseOffset / collapseRangePx).coerceIn(0f, 1f) else 0f
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val nestedScrollConnection = remember(collapseRangePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f) {
                    // Scrolling up (content moving up): collapse header
                    val prevOffset = collapseOffset
                    val newOffset = (prevOffset - delta).coerceIn(0f, collapseRangePx)
                    val consumed = prevOffset - newOffset
                    collapseOffset = newOffset
                    return Offset(0f, consumed)
                }
                if (delta > 0f && collapseOffset > 0f) {
                    // Scrolling down (content moving down): expand header (enterAlways)
                    val prevOffset = collapseOffset
                    val newOffset = (prevOffset - delta).coerceIn(0f, collapseRangePx)
                    val consumed = prevOffset - newOffset
                    collapseOffset = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (collapseOffset > 0f && collapseOffset < collapseRangePx) {
                    val target = when {
                        available.y < -300f -> collapseRangePx
                        available.y > 300f -> 0f
                        collapseOffset > collapseRangePx / 2f -> collapseRangePx
                        else -> 0f
                    }
                    val anim = Animatable(collapseOffset)
                    anim.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) {
                        collapseOffset = value
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            if (collapseOffset > 0f && !listState.isScrollInProgress) {
                val anim = Animatable(collapseOffset)
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) {
                    collapseOffset = value
                }
            }
        }
    }

    val tabReselectFlow = LocalTabReselectFlow.current

    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
        collapseOffset = 0f
        tabReselectFlow.collect { route ->
            if (route == "charts") {
                collapseOffset = 0f
                listState.scrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
            ) {
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
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back),
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

                // Toggle & Period layout interpolated by collapseFraction
                val toggleHeight = lerpDp(48.dp, 34.dp, collapseFraction)
                val togglePadding = lerpDp(3.dp, 2.dp, collapseFraction)
                val toggleCornerRadius = lerpDp(10.dp, 8.dp, collapseFraction)
                val toggleInnerCornerRadius = lerpDp(8.dp, 6.dp, collapseFraction)
                val toggleTextSize = lerpSp(14f, 12f, collapseFraction)
                val toggleSpacerBelow = lerpDp(10.dp, 0.dp, collapseFraction)
                val periodHeight = lerpDp(40.dp, 0.dp, collapseFraction)
                val periodSpacerBelow = lerpDp(12.dp, 8.dp, collapseFraction)

                // Gold / Silver Segmented Toggle (Pinned, compact on scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(toggleHeight)
                        .clip(RoundedCornerShape(toggleCornerRadius))
                        .background(DarkSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(toggleCornerRadius)
                        )
                        .padding(togglePadding)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(toggleInnerCornerRadius))
                            .background(if (isGoldSelected) GoldPrimary else Color.Transparent)
                            .clickable { isGoldSelected = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.metalGold,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = toggleTextSize),
                            color = if (isGoldSelected) DarkBackground else TextSecondary,
                            fontWeight = if (isGoldSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(toggleInnerCornerRadius))
                            .background(if (!isGoldSelected) SilverPrimary else Color.Transparent)
                            .clickable { isGoldSelected = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.metalSilver,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = toggleTextSize),
                            color = if (!isGoldSelected) DarkBackground else TextSecondary,
                            fontWeight = if (!isGoldSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                if (collapseFraction < 1f) {
                    Spacer(modifier = Modifier.height(toggleSpacerBelow))

                    // Range Selectors (7D, 30D, 1Y) (Collapses and disappears on scroll)
                    val rangeOptions = listOf(
                        Triple("7D", strings.range7D, Period.D7),
                        Triple("30D", strings.range30D, Period.D30),
                        Triple("1Y", strings.range1Y, Period.Y1)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(periodHeight)
                            .clipToBounds()
                            .graphicsLayer {
                                alpha = (1f - collapseFraction * 1.4f).coerceIn(0f, 1f)
                                translationY = -collapseFraction * 20f
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
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
                }

                Spacer(modifier = Modifier.height(periodSpacerBelow))

                // 1 dp bottom divider that appears only once the list is scrolled or header collapsed
                val dividerColor = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                val showDivider = collapseFraction > 0.05f || isScrolled
                val animatedDividerColor by animateColorAsState(
                    targetValue = if (showDivider) dividerColor else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "headerDividerColor"
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = animatedDividerColor
                )
            }
        },
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = bottomBarPadding + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (chartPoints.isEmpty()) {
                item(key = "skeleton_chart") {
                    ChartCardSkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item(key = "skeleton_summary") {
                    MarketSummarySkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item(key = "skeleton_rates") {
                    RatesTableSkeleton()
                }
            } else {
                // Trend Chart Container
                item(key = "chart_container") {
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Stat Summary Card (Title follows the Gold / Silver toggle)
            item(key = "market_summary") {
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
                        stringResource(R.string.from_date_to_today, startDateFormat.format(Date(firstPoint.timestamp)))
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
                                    val unsignedAmount = PriceConverter.formatAmount(kotlin.math.abs(periodDiff), selectedCurrency)
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
                                val unsignedAmount = PriceConverter.formatAmount(kotlin.math.abs(periodDiff), selectedCurrency)
                                val unsignedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(periodPercent))

                                val takeawaySentence = if (isPeriodUp) {
                                    stringResource(R.string.trend_summary_up, metalName, unsignedPercent, uiState.takeawaySpan, arrow, unsignedAmount)
                                } else if (isPeriodDown) {
                                    stringResource(R.string.trend_summary_down, metalName, unsignedPercent, uiState.takeawaySpan, arrow, unsignedAmount)
                                } else {
                                    stringResource(R.string.trend_summary_steady, metalName, uiState.takeawaySpan)
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
                                    Period.D7 -> stringResource(R.string.period_7_day)
                                    Period.D30 -> stringResource(R.string.period_30_day)
                                    Period.Y1 -> stringResource(R.string.period_1_year)
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Daily / Weekly Rates List (Direct LazyColumn items)
                val dailyDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val ratesTitle = "${if (isGoldSelected) strings.metalGold else strings.metalSilver} ${if (is1Y) strings.weeklyRates else strings.dailyRates}"
                val tableBorderColor = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f)

                // Header Item: top corners rounded, open bottom
                item(key = "rates_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(DarkSurface)
                            .drawBehind {
                                val stroke = 0.5.dp.toPx()
                                val r = 16.dp.toPx()
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(0f, h)
                                    lineTo(0f, r)
                                    quadraticTo(0f, 0f, r, 0f)
                                    lineTo(w - r, 0f)
                                    quadraticTo(w, 0f, w, r)
                                    lineTo(w, h)
                                }
                                drawPath(path, tableBorderColor, style = Stroke(width = stroke))
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = ratesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))
                        // Column headers
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
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = strings.changeHeader,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1.0f),
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.04f))
                    }
                }

                // Individual Rate Rows
                itemsIndexed(
                    items = displayRatesList,
                    key = { _, pt -> pt.dateKey }
                ) { revIdx, pt ->
                    val origIdx = chronologicalList.size - 1 - revIdx
                    val prevPrice = if (origIdx > 0) chronologicalList[origIdx - 1].price else null
                    val diff = prevPrice?.let { pt.price - it }
                    val diffPercent = if (prevPrice != null && prevPrice > 0.0) {
                        ((pt.price - prevPrice) / prevPrice) * 100.0
                    } else null

                    val isCardBottom = !hasShowMore && (revIdx == displayRatesList.size - 1)

                    val rowModifier = if (isCardBottom) {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(DarkSurface)
                            .drawBehind {
                                val stroke = 0.5.dp.toPx()
                                val r = 16.dp.toPx()
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(0f, h - r)
                                    quadraticTo(0f, h, r, h)
                                    lineTo(w - r, h)
                                    quadraticTo(w, h, w, h - r)
                                    lineTo(w, 0f)
                                }
                                drawPath(path, tableBorderColor, style = Stroke(width = stroke))
                            }
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .drawBehind {
                                val stroke = 0.5.dp.toPx()
                                drawLine(tableBorderColor, Offset(0f, 0f), Offset(0f, size.height), stroke)
                                drawLine(tableBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
                            }
                    }

                    Column(
                        modifier = rowModifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = if (isCardBottom) 12.dp else 0.dp)
                    ) {
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
                                textAlign = TextAlign.End
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

                        if (!isCardBottom) {
                            HorizontalDivider(
                                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f)
                            )
                        }
                    }
                }

                // Show more button for 1Y weekly rates
                if (hasShowMore) {
                    item(key = "rates_show_more") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(DarkSurface)
                                .drawBehind {
                                    val stroke = 0.5.dp.toPx()
                                    val r = 16.dp.toPx()
                                    val w = size.width
                                    val h = size.height
                                    val path = Path().apply {
                                        moveTo(0f, 0f)
                                        lineTo(0f, h - r)
                                        quadraticTo(0f, h, r, h)
                                        lineTo(w - r, h)
                                        quadraticTo(w, h, w, h - r)
                                        lineTo(w, 0f)
                                    }
                                    drawPath(path, tableBorderColor, style = Stroke(width = stroke))
                                }
                        ) {
                            HorizontalDivider(
                                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.04f)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAllWeeklyRows = true }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = strings.showMore,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isGoldSelected) GoldPrimary else SilverPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = strings.showMore,
                                        tint = if (isGoldSelected) GoldPrimary else SilverPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val isDark = isSystemDarkThemeGlobal
    val baseColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFE5E5EA)
    val highlightColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF2F2F7)

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
private fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    val shimmerBrush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}

@Composable
private fun ChartCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 0.5.dp,
                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SkeletonBox(modifier = Modifier.size(width = 36.dp, height = 12.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(modifier = Modifier.size(width = 72.dp, height = 18.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SkeletonBox(modifier = Modifier.size(width = 54.dp, height = 12.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 22.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    SkeletonBox(modifier = Modifier.size(width = 36.dp, height = 12.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonBox(modifier = Modifier.size(width = 72.dp, height = 18.dp))
                }
            }
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun MarketSummarySkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SkeletonBox(modifier = Modifier.size(width = 160.dp, height = 20.dp))
            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))
            repeat(4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBox(modifier = Modifier.size(width = 100.dp, height = 14.dp))
                    SkeletonBox(modifier = Modifier.size(width = 80.dp, height = 14.dp))
                }
                HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f))
            }
        }
    }
}

@Composable
private fun RatesTableSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
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
            SkeletonBox(modifier = Modifier.size(width = 140.dp, height = 20.dp))
            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f))
            repeat(5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 14.dp))
                    SkeletonBox(modifier = Modifier.size(width = 80.dp, height = 14.dp))
                    SkeletonBox(modifier = Modifier.size(width = 60.dp, height = 14.dp))
                }
                HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.03f))
            }
        }
    }
}

