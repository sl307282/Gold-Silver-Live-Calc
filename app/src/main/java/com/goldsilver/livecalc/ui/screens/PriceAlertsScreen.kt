package com.goldsilver.livecalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val AlertsPrimary = Color(0xFF38BDF8) // Vibrant Sapphire / Sky Blue

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
    val context = LocalContext.current

    val currentGoldPrice = latestRate?.goldPrice24k ?: 76.20
    val currentSilverPrice = latestRate?.silverPrice ?: 0.96

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.priceAlertsTitle, color = AlertsPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = AlertsPrimary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live context price card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 0.5.dp,
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Gold (24K)", color = TextSecondary, fontSize = 12.sp)
                            Text("${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(currentGoldPrice)} $currency", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Silver", color = TextSecondary, fontSize = 12.sp)
                            Text("${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(currentSilverPrice)} $currency", color = SilverPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
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
                        Text("Create Price Alert", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

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
                                        text = "Gold",
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
                                        text = "Silver",
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
                                        text = "Goes Below (≤)",
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
                                        text = "Goes Above (≥)",
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
                            onValueChange = { targetPriceInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatInput(it) },
                            label = { Text("Target Price ($currency/Gram)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            onClick = {
                                val price = com.goldsilver.livecalc.util.IndianCurrencyFormatter.parseAmount(targetPriceInput)
                                if (price > 0) {
                                    val selectedMetal = if (isGold) "GOLD" else "SILVER"
                                    val metalLabel = if (isGold) "Gold" else "Silver"
                                    val selectedCondition = if (isAboveCondition) "ABOVE" else "BELOW"
                                    val conditionLabel = if (isAboveCondition) "Goes Above (≥)" else "Goes Below (≤)"
                                    val formattedPrice = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(price)} $currency"

                                    // Check if duplicate alert exists for same metal, target price, and condition
                                    val isDuplicate = activeAlerts.any { alert ->
                                        alert.metal.equals(selectedMetal, ignoreCase = true) &&
                                        alert.condition.equals(selectedCondition, ignoreCase = true) &&
                                        Math.abs(alert.targetPrice - price) < 0.001
                                    }

                                    if (isDuplicate) {
                                        // Show Duplicate Alert Popup
                                        val duplicateMsg = "Target price already set"
                                        android.widget.Toast.makeText(context, "⚠️ $duplicateMsg", android.widget.Toast.LENGTH_LONG).show()
                                        alertDialogData = AlertDialogData(
                                            title = "Target Price Already Set",
                                            message = "A price alert for $metalLabel ($conditionLabel) at $formattedPrice/g is already set and active.",
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

                                        // Show Success Popup
                                        val successMsg = "Target price $formattedPrice set successfully"
                                        android.widget.Toast.makeText(context, "✅ $successMsg", android.widget.Toast.LENGTH_LONG).show()
                                        alertDialogData = AlertDialogData(
                                            title = "Target Price Set Successfully",
                                            message = "Target price $formattedPrice/g ($conditionLabel) set successfully for $metalLabel.",
                                            isSuccess = true
                                        )
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter a valid target price", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertsPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set Alert", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }



            // Active Alerts Section
            item {
                Text(
                    text = "ACTIVE ALERTS (${activeAlerts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeAlerts.isEmpty()) {
                item {
                    Text(
                        text = "No active alerts. We'll monitor prices in the background and notify you when they hit your targets.",
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
                    Text("Multiple Alerts Limit")
                }
            },
            text = {
                Text("Free users can set only 1 active alert at a time. Upgrade to Premium for unlimited target alerts in the background!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPremium(true)
                        showLimitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertsPrimary)
                ) {
                    Text("Unlock Premium", color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Close", color = TextSecondary)
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
                    Text("OK", color = DarkBackground, fontWeight = FontWeight.Bold)
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
    val metalName = if (alert.metal == "GOLD") "Gold 24K" else "Silver"
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
                    contentDescription = "Alert",
                    tint = if (alert.isActive) metalColor else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$metalName $conditionSymbol ${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(alert.targetPrice)} $currency",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    val createdDate = Date(alert.createdAt)
                    val statusText = if (alert.triggeredAt != null) {
                        val triggeredDate = Date(alert.triggeredAt)
                        "Created: ${sdf.format(createdDate)}\nLast Hit: ${sdf.format(triggeredDate)}"
                    } else {
                        "Created: ${sdf.format(createdDate)}"
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
                    contentDescription = "Delete Alert",
                    tint = AccentRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
