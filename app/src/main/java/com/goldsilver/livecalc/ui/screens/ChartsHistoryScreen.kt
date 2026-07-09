package com.goldsilver.livecalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import com.goldsilver.livecalc.ui.components.HistoryLineChart
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
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
    val historicalRates by viewModel.historicalRates.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    var isGoldSelected by remember { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf("7D") } // 7D, 30D, 1Y

    // Filter historical points to ensure one point per calendar day
    val chartPoints = remember(historicalRates, isGoldSelected, selectedRange) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dailyRates = historicalRates
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .map { it.value.last() }
            .sortedBy { it.timestamp }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Historical Trends", color = GoldPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SilverPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Spot",
                                color = SilverPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
                        )
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
                            text = "Gold Rates",
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
                            text = "Silver Rates",
                            color = if (!isGoldSelected) DarkBackground else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Range Selectors
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("7D", "30D", "1Y").forEach { range ->
                        val isRangeSelected = selectedRange == range
                        val activeColor = if (isGoldSelected) GoldPrimary else SilverPrimary
                        
                        val isLocked = range == "1Y" && !isPremium

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRange = range }
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
                                    text = range,
                                    color = if (isRangeSelected) activeColor else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (isLocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Premium Required",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trend Chart container
            item {
                val isRangeLocked = selectedRange == "1Y" && !isPremium

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
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
                                currency = currency,
                                modifier = Modifier.fillMaxSize()
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
                                    contentDescription = "Lock",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "1-Year Trends is a Premium Feature",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Unlock long-term investment graphs and deeper market analytics.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Button(
                                    onClick = { viewModel.setPremium(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    Text("Upgrade to Premium", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        HistoryLineChart(
                            points = chartPoints,
                            isGold = isGoldSelected,
                            currency = currency,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Stat Summary Cards
            item {
                if (chartPoints.isNotEmpty()) {
                    val prices = chartPoints.map { it.second }
                    val maxPrice = prices.maxOrNull() ?: 0.0
                    val minPrice = prices.minOrNull() ?: 0.0
                    val avgPrice = prices.average()
                    val metalName = if (isGoldSelected) "Gold" else "Silver"

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
                                text = "$metalName Market Summary ($selectedRange)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Highest Rate", color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    text = String.format("%.2f %s/g", maxPrice, currency),
                                    color = AccentGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lowest Rate", color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    text = String.format("%.2f %s/g", minPrice, currency),
                                    color = AccentRed,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Average Price", color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    text = String.format("%.2f %s/g", avgPrice, currency),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
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
