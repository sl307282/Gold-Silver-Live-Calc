package com.goldsilver.livecalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.goldsilver.livecalc.R

private val AlertsPrimary: Color
    get() = if (isSystemDarkThemeGlobal) Color(0xFF38BDF8) else Color(0xFF0284C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.goldsilver.livecalc.util.LocalAppStrings.current
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val latestRate by viewModel.latestRate.collectAsStateWithLifecycle()
    val previousRate by viewModel.previousTradingDayRate.collectAsStateWithLifecycle()
    val goldChange by viewModel.goldChangePercent.collectAsStateWithLifecycle()
    val silverChange by viewModel.silverChangePercent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentGoldPrice = latestRate?.goldPrice24k
    val currentSilverPrice = latestRate?.silverPrice
    val prevGoldPrice = latestRate?.prevTradingDayGoldPrice ?: previousRate?.goldPrice24k
    val prevSilverPrice = latestRate?.prevTradingDaySilverPrice ?: previousRate?.silverPrice

    val goldDiffAmount = if (currentGoldPrice != null && prevGoldPrice != null && prevGoldPrice > 0) currentGoldPrice - prevGoldPrice else null
    val silverDiffAmount = if (currentSilverPrice != null && prevSilverPrice != null && prevSilverPrice > 0) currentSilverPrice - prevSilverPrice else null

    val effectiveGoldChange = goldChange ?: if (goldDiffAmount != null && prevGoldPrice != null && prevGoldPrice > 0) (goldDiffAmount / prevGoldPrice) * 100.0 else null
    val effectiveSilverChange = silverChange ?: if (silverDiffAmount != null && prevSilverPrice != null && prevSilverPrice > 0) (silverDiffAmount / prevSilverPrice) * 100.0 else null

    val lastUpdatedText = remember(latestRate?.timestamp) {
        latestRate?.timestamp?.let { ts ->
            val sdf = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
            sdf.format(Date(ts))
        } ?: "—"
    }

    // Form states
    var isGold by remember { mutableStateOf(true) }
    var targetPriceInput by remember { mutableStateOf("") }
    var isAboveCondition by remember { mutableStateOf(false) } // Default: Goes Below (false: BELOW, true: ABOVE)

    val activeAlerts = remember(alerts) { alerts.filter { it.isActive } }

    var showLimitDialog by remember { mutableStateOf(false) }
    var alertDialogData by remember { mutableStateOf<AlertDialogData?>(null) }

    // Auto-disappear popup after 2 seconds
    LaunchedEffect(alertDialogData) {
        if (alertDialogData != null) {
            kotlinx.coroutines.delay(2000L)
            alertDialogData = null
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val submitAlert: () -> Unit = {
        val price = com.goldsilver.livecalc.util.CurrencyFormatter.parseAmount(targetPriceInput)
        if (price > 0) {
            val selectedMetal = if (isGold) "GOLD" else "SILVER"
            val metalLabel = if (isGold) context.getString(R.string.metal_gold) else context.getString(R.string.metal_silver)
            val selectedCondition = if (isAboveCondition) "ABOVE" else "BELOW"
            val conditionLabel = if (isAboveCondition) context.getString(R.string.rises_above) else context.getString(R.string.falls_below)
            val formattedPrice = com.goldsilver.livecalc.util.CurrencyFormatter.formatCurrency(price, currency)

            // Check if duplicate alert exists for same metal, target price, and condition
            val isDuplicate = activeAlerts.any { alert ->
                alert.metal.equals(selectedMetal, ignoreCase = true) &&
                alert.condition.equals(selectedCondition, ignoreCase = true) &&
                Math.abs(alert.targetPrice - price) < 0.001
            }

            if (isDuplicate) {
                // Show Duplicate Alert Popup
                val duplicateMsg = context.getString(R.string.alert_duplicate_toast)
                android.widget.Toast.makeText(context, "⚠️ $duplicateMsg", android.widget.Toast.LENGTH_LONG).show()
                alertDialogData = AlertDialogData(
                    title = context.getString(R.string.alert_duplicate_title),
                    message = context.getString(R.string.alert_duplicate_message, metalLabel, conditionLabel, formattedPrice),
                    isSuccess = false
                )
            } else if (!isPremium && activeAlerts.size >= 1) {
                // Verify monetization limits: max 1 alert for free tier
                showLimitDialog = true
            } else {
                // Save new Alert
                viewModel.addAlert(
                    metal = selectedMetal,
                    targetPrice = price,
                    condition = selectedCondition
                )
                targetPriceInput = ""
                keyboardController?.hide()
                focusManager.clearFocus()

                // Show Success Popup
                val successMsg = context.getString(R.string.alert_success_toast, formattedPrice)
                android.widget.Toast.makeText(context, "✅ $successMsg", android.widget.Toast.LENGTH_LONG).show()
                alertDialogData = AlertDialogData(
                    title = context.getString(R.string.alert_success_title),
                    message = context.getString(R.string.alert_success_message, formattedPrice, conditionLabel, metalLabel),
                    isSuccess = true
                )
            }
        } else {
            android.widget.Toast.makeText(context, context.getString(R.string.alert_invalid_price), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.priceAlertsTitle, color = AlertsPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = AlertsPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Hero Price Card
            item {
                val heroCardBrush = if (isSystemDarkThemeGlobal) {
                    Brush.linearGradient(listOf(DarkSurface, Color(0xFF221E14)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFFFFDF0), Color(0xFFFFF9E6)))
                }
                val heroBorderColor = if (isSystemDarkThemeGlobal) {
                    GoldPrimary.copy(alpha = 0.3f)
                } else {
                    GoldPrimary.copy(alpha = 0.35f)
                }
                val heroElevation = if (!isSystemDarkThemeGlobal) 3.dp else 0.dp

                val goldAlpha by animateFloatAsState(targetValue = if (isGold) 1f else 0.6f, label = "goldAlpha")
                val silverAlpha by animateFloatAsState(targetValue = if (!isGold) 1f else 0.6f, label = "silverAlpha")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = heroBorderColor,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = heroElevation),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .background(brush = heroCardBrush)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Gold Column
                            val goldHighlightBg = if (isGold) GoldPrimary.copy(alpha = 0.10f) else Color.Transparent
                            val goldHighlightBorder = if (isGold) GoldPrimary.copy(alpha = 0.45f) else Color.Transparent

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(goldAlpha)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(goldHighlightBg)
                                    .border(
                                        width = 1.dp,
                                        color = goldHighlightBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isGold = true }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.gold24k,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currentGoldPrice?.let { CurrencyFormatter.formatCurrency(it, currency) } ?: "—"} / g",
                                    color = GoldPrimary,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                MetalPriceChangeText(
                                    diffAmount = goldDiffAmount,
                                    changePercent = effectiveGoldChange,
                                    currency = currency
                                )
                            }

                            // Silver Column
                            val silverHighlightColor = if (isSystemDarkThemeGlobal) Color.White else SilverPrimary
                            val silverHighlightBg = if (!isGold) silverHighlightColor.copy(alpha = 0.10f) else Color.Transparent
                            val silverHighlightBorder = if (!isGold) silverHighlightColor.copy(alpha = 0.45f) else Color.Transparent

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(silverAlpha)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(silverHighlightBg)
                                    .border(
                                        width = 1.dp,
                                        color = silverHighlightBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isGold = false }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.metalSilver,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currentSilverPrice?.let { CurrencyFormatter.formatCurrency(it, currency) } ?: "—"} / g",
                                    color = if (isSystemDarkThemeGlobal) Color.White else TextPrimary,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                MetalPriceChangeText(
                                    diffAmount = silverDiffAmount,
                                    changePercent = effectiveSilverChange,
                                    currency = currency
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${strings.lastUpdated} • $lastUpdatedText",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Create Alert Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.create_price_alert), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        // Metal select
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isDark = isSystemDarkThemeGlobal
                            // Gold button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isGold) GoldPrimary 
                                        else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F5F9))
                                    )
                                    .border(
                                        width = if (isGold) 2.dp else 1.dp,
                                        color = if (isGold) GoldPrimary 
                                                else (if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isGold = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🪙",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.metal_gold),
                                        color = if (isGold) DarkBackground else (if (isDark) Color.White else Color(0xFF1E293B)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            // Silver button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (!isGold) SilverPrimary 
                                        else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F5F9))
                                    )
                                    .border(
                                        width = if (!isGold) 2.dp else 1.dp,
                                        color = if (!isGold) SilverPrimary 
                                                else (if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isGold = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🥈",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.metal_silver),
                                        color = if (!isGold) DarkBackground else (if (isDark) Color.White else Color(0xFF1E293B)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Trigger Condition select (Left: Goes Below, Right: Goes Above)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isDark = isSystemDarkThemeGlobal
                            // Goes Below button (Left Side)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (!isAboveCondition) AlertsPrimary 
                                        else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F5F9))
                                    )
                                    .border(
                                        width = if (!isAboveCondition) 2.dp else 1.dp,
                                        color = if (!isAboveCondition) AlertsPrimary 
                                                else (if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isAboveCondition = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "📉",
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.falls_below),
                                        color = if (!isAboveCondition) Color(0xFF0F172A) else (if (isDark) Color.White else Color(0xFF1E293B)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                            // Goes Above button (Right Side)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isAboveCondition) AlertsPrimary 
                                        else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F5F9))
                                    )
                                    .border(
                                        width = if (isAboveCondition) 2.dp else 1.dp,
                                        color = if (isAboveCondition) AlertsPrimary 
                                                else (if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isAboveCondition = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "📈",
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.rises_above),
                                        color = if (isAboveCondition) Color(0xFF0F172A) else (if (isDark) Color.White else Color(0xFF1E293B)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }

                        // Target Price Input
                        OutlinedTextField(
                            value = targetPriceInput,
                            onValueChange = { targetPriceInput = com.goldsilver.livecalc.util.CurrencyFormatter.formatInput(it, currency) },
                            label = { Text(stringResource(R.string.target_price_format, currency)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitAlert() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AlertsPrimary,
                                focusedLabelColor = AlertsPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Save Alert Button
                        Button(
                            onClick = submitAlert,
                            colors = ButtonDefaults.buttonColors(containerColor = AlertsPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_set_alert), color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }



            // Active Alerts Section
            item {
                Text(
                    text = stringResource(R.string.active_alerts_count, activeAlerts.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeAlerts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_active_alerts),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(activeAlerts) { alert ->
                    AlertItem(
                        alert = alert,
                        currency = currency,
                        onDelete = { viewModel.removeAlert(alert) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Limit reached dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = AlertsPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.limit_dialog_title))
                }
            },
            text = {
                Text(stringResource(R.string.limit_dialog_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPremium(true)
                        showLimitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertsPrimary)
                ) {
                    Text(stringResource(R.string.unlock_premium), color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(stringResource(R.string.btn_close), color = TextSecondary)
                }
            }
        )
    }

    // Success or Duplicate Popup Dialog
    alertDialogData?.let { data ->
        AlertDialog(
            onDismissRequest = { alertDialogData = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (data.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (data.isSuccess) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = data.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = data.message,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { alertDialogData = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (data.isSuccess) Color(0xFF10B981) else AlertsPrimary
                    )
                ) {
                    Text(stringResource(R.string.btn_ok), color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            tonalElevation = 8.dp
        )
    }
}

private data class AlertDialogData(
    val title: String,
    val message: String,
    val isSuccess: Boolean
)

@Composable
fun AlertItem(
    alert: AlertEntity,
    currency: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metalName = if (alert.metal == "GOLD") "${stringResource(R.string.metal_gold)} 24K" else stringResource(R.string.metal_silver)
    val metalColor = if (alert.metal == "GOLD") GoldPrimary else SilverPrimary
    val conditionSymbol = if (alert.condition == "ABOVE") "≥" else "≤"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 0.5.dp,
                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = stringResource(R.string.alert_content_desc),
                    tint = if (alert.isActive) metalColor else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$metalName $conditionSymbol ${com.goldsilver.livecalc.util.CurrencyFormatter.formatCurrency(alert.targetPrice, currency)}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    val createdDate = Date(alert.createdAt)
                    val statusText = if (alert.triggeredAt != null) {
                        val triggeredDate = Date(alert.triggeredAt)
                        stringResource(R.string.alert_created_and_hit, sdf.format(createdDate), sdf.format(triggeredDate))
                    } else {
                        stringResource(R.string.alert_created_at, sdf.format(createdDate))
                    }

                    Text(
                        text = statusText,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_alert_cd),
                    tint = AccentRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MetalPriceChangeText(
    diffAmount: Double?,
    changePercent: Double?,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (diffAmount != null && changePercent != null) {
        val isUp = diffAmount > 0.001
        val isDown = diffAmount < -0.001
        val arrow = if (isUp) "▲" else if (isDown) "▼" else "—"
        val color = if (isUp) Color(0xFF4ADE80) else if (isDown) Color(0xFFFF6B6B) else TextMuted
        val absDiff = CurrencyFormatter.formatCurrency(Math.abs(diffAmount), currency)
        val pctStr = String.format(Locale.US, "%.2f", Math.abs(changePercent))

        Text(
            text = "$arrow $absDiff · $pctStr%",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = modifier
        )
    } else {
        Text(
            text = "—",
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier
        )
    }
}
