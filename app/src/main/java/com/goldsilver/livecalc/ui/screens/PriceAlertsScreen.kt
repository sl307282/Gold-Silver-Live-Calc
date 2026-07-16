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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val latestRate by viewModel.latestRate.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("rate_alerts")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            android.widget.Toast.makeText(
                context,
                "✅ Gold & Silver price alerts enabled.\nMonitoring rates in the background and notifying you when your target alerts are met.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } else {
            viewModel.setNotificationsEnabled(false)
            android.widget.Toast.makeText(
                context,
                "Notification permission is required to enable alerts.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onToggleNotification: (Boolean) -> Unit = { checked ->
        if (checked) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    viewModel.setNotificationsEnabled(true)
                    try {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("rate_alerts")
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    android.widget.Toast.makeText(
                        context,
                        "✅ Gold & Silver price alerts enabled.\nMonitoring rates in the background and notifying you when your target alerts are met.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                viewModel.setNotificationsEnabled(true)
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("rate_alerts")
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                android.widget.Toast.makeText(
                    context,
                    "✅ Gold & Silver price alerts enabled.\nMonitoring rates in the background and notifying you when your target alerts are met.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } else {
            viewModel.setNotificationsEnabled(false)
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("rate_alerts")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    val currentGoldPrice = latestRate?.goldPrice24k ?: 76.20
    val currentSilverPrice = latestRate?.silverPrice ?: 0.96

    // Form states
    var isGold by remember { mutableStateOf(true) }
    var targetPriceInput by remember { mutableStateOf("") }
    var isAboveCondition by remember { mutableStateOf(true) } // true: ABOVE, false: BELOW

    val activeAlerts = remember(alerts) { alerts.filter { it.isActive } }

    var showLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Price Alerts", color = GoldPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
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
                            Text(String.format("%.2f %s", currentGoldPrice, currency), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Silver", color = TextSecondary, fontSize = 12.sp)
                            Text(String.format("%.2f %s", currentSilverPrice, currency), color = SilverPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isGold) GoldPrimary else Color.Transparent)
                                    .clickable { isGold = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Gold", color = if (isGold) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isGold) SilverPrimary else Color.Transparent)
                                    .clickable { isGold = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Silver", color = if (!isGold) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Trigger Condition select
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAboveCondition) GoldPrimary else Color.Transparent)
                                    .clickable { isAboveCondition = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Goes Above (≥)", color = if (isAboveCondition) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isAboveCondition) GoldPrimary else Color.Transparent)
                                    .clickable { isAboveCondition = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Goes Below (≤)", color = if (!isAboveCondition) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Target Price Input
                        OutlinedTextField(
                            value = targetPriceInput,
                            onValueChange = { targetPriceInput = it },
                            label = { Text("Target Price ($currency/Gram)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                focusedLabelColor = GoldPrimary,
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
                                val price = targetPriceInput.toDoubleOrNull() ?: 0.0
                                if (price > 0) {
                                    // Verify monetization limits: max 1 alert for free tier
                                    if (!isPremium && activeAlerts.size >= 1) {
                                        showLimitDialog = true
                                    } else {
                                        viewModel.addAlert(
                                            metal = if (isGold) "GOLD" else "SILVER",
                                            targetPrice = price,
                                            condition = if (isAboveCondition) "ABOVE" else "BELOW"
                                        )
                                        targetPriceInput = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set Alert", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notification Toggle Card
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleNotification(!isNotificationsEnabled) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = SilverPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Gold & Silver Price Alerts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Switch(
                                checked = isNotificationsEnabled,
                                onCheckedChange = { onToggleNotification(it) },
                                thumbContent = if (isNotificationsEnabled) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            tint = DarkBackground
                                        )
                                    }
                                } else null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoldPrimary,
                                    checkedTrackColor = GoldPrimary.copy(alpha = 0.3f),
                                    checkedBorderColor = GoldPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkSurfaceElevated,
                                    uncheckedBorderColor = TextMuted.copy(alpha = 0.5f)
                                )
                            )
                        }
                        if (isNotificationsEnabled) {
                            Text(
                                text = "✅ Gold & Silver price alerts enabled.\nMonitoring rates in the background and notifying you when your target alerts are met.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = GoldPrimary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
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
}

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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Alert",
                    tint = if (alert.isActive) metalColor else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$metalName $conditionSymbol ${String.format("%.2f", alert.targetPrice)} $currency",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    val createdDate = Date(alert.createdAt)
                    val statusText = if (alert.triggeredAt != null) {
                        val triggeredDate = Date(alert.triggeredAt)
                        "Created: ${sdf.format(createdDate)} | Last Hit: ${sdf.format(triggeredDate)}"
                    } else {
                        "Created: ${sdf.format(createdDate)}"
                    }

                    Text(
                        text = statusText,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = AccentRed
                )
            }
        }
    }
}
