package com.goldsilver.livecalc.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.util.LocalAppStrings
import com.goldsilver.livecalc.util.getSupportedLanguages
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val backgroundTheme by viewModel.backgroundTheme.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
            android.widget.Toast.makeText(context, "✅ ${strings.alertsEnabledDesc}", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setNotificationsEnabled(false)
            android.widget.Toast.makeText(context, strings.alertsDisabledDesc, android.widget.Toast.LENGTH_SHORT).show()
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
                } else {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                viewModel.setNotificationsEnabled(true)
            }
        } else {
            viewModel.setNotificationsEnabled(false)
        }
    }

    var currencyExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, color = GoldPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = GoldPrimary)
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
            // General Settings Card
            item {
                Text(
                    text = strings.preferencesAndAlerts,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

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
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Price Alert Notifications Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleNotification(!isNotificationsEnabled) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = SilverPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(strings.priceAlertNotifications, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = if (isNotificationsEnabled) strings.alertsEnabledDesc else strings.alertsDisabledDesc,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
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

                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                        // Currency Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currencyExpanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = "Currency", tint = SilverPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(strings.displayCurrency, color = TextPrimary)
                            }
                            Box {
                                Text(currency, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                DropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false }
                                ) {
                                    listOf("INR", "USD", "EUR", "AED", "GBP").forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text(curr) },
                                            onClick = {
                                                viewModel.setCurrency(curr)
                                                currencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                        // Language Selector Row (7 Languages Supported)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { languageExpanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = "Language", tint = SilverPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(strings.appLanguage, color = TextPrimary)
                            }
                            Box {
                                val currentDisplayName = getSupportedLanguages().firstOrNull { it.first.equals(language, ignoreCase = true) }?.second ?: language
                                Text(currentDisplayName, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                DropdownMenu(
                                    expanded = languageExpanded,
                                    onDismissRequest = { languageExpanded = false }
                                ) {
                                    getSupportedLanguages().forEach { (code, displayName) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(displayName, fontWeight = if (language.equals(code, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal)
                                                    if (code != displayName) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("($code)", color = TextMuted, fontSize = 12.sp)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.setLanguage(code)
                                                languageExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                        // Theme Row (active)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { themeExpanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = "Theme", tint = SilverPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(strings.backgroundTheme, color = TextPrimary)
                            }
                            Box {
                                Text(backgroundTheme, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                DropdownMenu(
                                    expanded = themeExpanded,
                                    onDismissRequest = { themeExpanded = false }
                                ) {
                                    listOf("Ivory", "Cream", "Mist", "Sage", "Lavender", "Peach", "Sky Blue", "Sand", "Night").forEach { themeName ->
                                        DropdownMenuItem(
                                            text = { Text(themeName) },
                                            onClick = {
                                                viewModel.setBackgroundTheme(themeName)
                                                themeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Support Card
            item {
                Text(
                    text = strings.aboutAndSupport,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

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
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Rate Us Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val marketUri = "market://details?id=com.goldsilver.livecalc"
                                    val playStoreUri = "https://play.google.com/store/apps/details?id=com.goldsilver.livecalc"
                                    val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUri)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(playStoreIntent)
                                    } catch (e: Exception) {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(browserIntent)
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Rate Us", tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(strings.rateApp, color = TextPrimary)
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                        // Share us Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Gold & Silver Live Calculator")
                                        putExtra(Intent.EXTRA_TEXT, "Check out Gold & Silver Live Calculator for real-time rates and trend charts! Download here: https://play.google.com/store/apps/details?id=${context.packageName}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = "Share us", tint = SilverPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(strings.shareApp, color = TextPrimary)
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f))

                        // Check for Updates Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.checkForAppUpdate(silent = false, context = context)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = "Check for Updates", tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(strings.updateAvailable, color = TextPrimary)
                                    Text("${strings.appVersion} ${com.goldsilver.livecalc.BuildConfig.VERSION_NAME}", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
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
