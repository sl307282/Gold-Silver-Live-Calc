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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsState()
    val language by viewModel.language.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val backgroundTheme by viewModel.backgroundTheme.collectAsState()


    var currencyExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = GoldPrimary, fontWeight = FontWeight.Bold) },
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
            // Premium Subscription Card removed

            // General Settings Card
            item {
                Text(
                    text = "PREFERENCES",
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
                                Text("Display Currency", color = TextPrimary)
                            }
                            Box {
                                Text(currency, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                DropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false }
                                ) {
                                    listOf("USD", "INR", "EUR", "AED").forEach { curr ->
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

                        

                        // Language Selector Row
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
                                Text("App Language", color = TextPrimary)
                            }
                            Box {
                                Text(language, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                DropdownMenu(
                                    expanded = languageExpanded,
                                    onDismissRequest = { languageExpanded = false }
                                ) {
                                    listOf("English", "Hindi", "Spanish").forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang) },
                                            onClick = {
                                                viewModel.setLanguage(lang)
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
                                Text("Background Theme", color = TextPrimary)
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
                    text = "SUPPORT & FEEDBACK",
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
                                Text("Rate Us", color = TextPrimary)
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
                                Text("Share App", color = TextPrimary)
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
