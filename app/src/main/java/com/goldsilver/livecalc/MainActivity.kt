package com.goldsilver.livecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goldsilver.livecalc.ota.OtaState
import com.goldsilver.livecalc.ui.components.CustomBottomNavigation
import com.goldsilver.livecalc.ui.screens.*
import com.goldsilver.livecalc.ui.theme.GoldSilverLiveCalcTheme
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle native splash screen transition
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            GoldSilverLiveCalcTheme {
                val navController = rememberNavController()
                val viewModel: GoldSilverViewModel = viewModel()
                val context = LocalContext.current

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
                val latestVersionName by viewModel.latestVersionName.collectAsState()
                val updateMessage by viewModel.updateMessage.collectAsState()
                val otaState by viewModel.otaState.collectAsState()
                val otaProgress by viewModel.otaProgress.collectAsState()
                val otaError by viewModel.otaError.collectAsState()
                val apkDownloadUrl by viewModel.apkDownloadUrl.collectAsState()

                // Animated progress for smooth progress bar
                val animatedProgress by animateFloatAsState(
                    targetValue = otaProgress,
                    animationSpec = tween(durationMillis = 300),
                    label = "ota_progress"
                )

                if (showUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            // Block dismissal while downloading
                            if (otaState != OtaState.DOWNLOADING) {
                                viewModel.dismissUpdateDialog()
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (otaState) {
                                        OtaState.READY -> Icons.Default.CheckCircle
                                        OtaState.ERROR -> Icons.Default.ErrorOutline
                                        else -> Icons.Default.SystemUpdate
                                    },
                                    contentDescription = "Update",
                                    tint = when (otaState) {
                                        OtaState.READY -> Color(0xFF4CAF50)
                                        OtaState.ERROR -> Color(0xFFEF5350)
                                        else -> com.goldsilver.livecalc.ui.theme.GoldPrimary
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (otaState) {
                                        OtaState.DOWNLOADING -> "Downloading Update…"
                                        OtaState.READY -> "Ready to Install!"
                                        OtaState.ERROR -> "Download Failed"
                                        else -> "Update Available!"
                                    },
                                    color = when (otaState) {
                                        OtaState.READY -> Color(0xFF4CAF50)
                                        OtaState.ERROR -> Color(0xFFEF5350)
                                        else -> com.goldsilver.livecalc.ui.theme.GoldPrimary
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (otaState) {
                                    OtaState.IDLE -> {
                                        val isPlayStoreUrl = apkDownloadUrl.contains("play.google.com") || apkDownloadUrl.contains("market://")
                                        Text(
                                            text = updateMessage.ifBlank {
                                                "Gold & Silver Live Calc v$latestVersionName is available with the latest market rates and improvements."
                                            },
                                            color = com.goldsilver.livecalc.ui.theme.TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        if (apkDownloadUrl.isNotBlank() && !isPlayStoreUrl) {
                                            Text(
                                                text = "Tap \"Download & Install\" to update instantly — no Play Store needed.",
                                                color = com.goldsilver.livecalc.ui.theme.TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    OtaState.DOWNLOADING -> {
                                        Text(
                                            text = "Downloading v$latestVersionName…",
                                            color = com.goldsilver.livecalc.ui.theme.TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            color = com.goldsilver.livecalc.ui.theme.GoldPrimary,
                                            trackColor = com.goldsilver.livecalc.ui.theme.GoldPrimary.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                        Text(
                                            text = "${(animatedProgress * 100).toInt()}%",
                                            color = com.goldsilver.livecalc.ui.theme.GoldPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "Please keep the app open until the download completes.",
                                            color = com.goldsilver.livecalc.ui.theme.TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    OtaState.READY -> {
                                        LinearProgressIndicator(
                                            progress = { 1f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            color = Color(0xFF4CAF50),
                                            trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                        Text(
                                            text = "v$latestVersionName downloaded successfully! Tap \"Install Now\" to complete the update.",
                                            color = com.goldsilver.livecalc.ui.theme.TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    OtaState.ERROR -> {
                                        Text(
                                            text = otaError ?: "An unexpected error occurred during download.",
                                            color = Color(0xFFEF5350),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "You can try again or visit the Play Store to update.",
                                            color = com.goldsilver.livecalc.ui.theme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            val isPlayStoreUrl = apkDownloadUrl.contains("play.google.com") || apkDownloadUrl.contains("market://")
                            Button(
                                onClick = {
                                    when (otaState) {
                                        OtaState.IDLE -> {
                                            if (apkDownloadUrl.isNotBlank() && !isPlayStoreUrl) {
                                                viewModel.startOtaDownload()
                                            } else {
                                                // Redirect to Play Store
                                                val targetUrl = apkDownloadUrl.ifBlank { "market://details?id=com.goldsilver.livecalc" }
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(targetUrl)
                                                ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                                try { startActivity(intent) } catch (e: Exception) {
                                                    startActivity(android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.goldsilver.livecalc")
                                                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                                                }
                                                viewModel.dismissUpdateDialog()
                                            }
                                        }
                                        OtaState.READY -> viewModel.installOta(context)
                                        OtaState.ERROR -> viewModel.startOtaDownload() // Retry
                                        OtaState.DOWNLOADING -> { /* Do nothing — wait */ }
                                    }
                                },
                                enabled = otaState != OtaState.DOWNLOADING,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (otaState) {
                                        OtaState.READY -> Color(0xFF4CAF50)
                                        OtaState.ERROR -> com.goldsilver.livecalc.ui.theme.GoldPrimary
                                        else -> com.goldsilver.livecalc.ui.theme.GoldPrimary
                                    }
                                )
                            ) {
                                Text(
                                    text = when (otaState) {
                                        OtaState.IDLE -> if (apkDownloadUrl.isNotBlank() && !isPlayStoreUrl) "Download & Install" else "Update Now"
                                        OtaState.DOWNLOADING -> "Downloading…"
                                        OtaState.READY -> "Install Now"
                                        OtaState.ERROR -> "Retry Download"
                                    },
                                    color = com.goldsilver.livecalc.ui.theme.DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            if (otaState != OtaState.DOWNLOADING) {
                                TextButton(onClick = { viewModel.cancelOta() }) {
                                    Text(
                                        text = "Later",
                                        color = com.goldsilver.livecalc.ui.theme.TextSecondary
                                    )
                                }
                            }
                        },
                        containerColor = com.goldsilver.livecalc.ui.theme.DarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    )
                }

                // Determine if bottom navigation should be visible
                val showBottomBar = currentRoute in listOf(
                    "dashboard",
                    "charts",
                    "hallmark",
                    "settings"
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            CustomBottomNavigation(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo("dashboard") {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate("dashboard") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            HomeDashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { route ->
                                    navController.navigate(route)
                                }
                            )
                        }
                        composable("gold_calc") {
                            GoldCalculatorScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("silver_calc") {
                            SilverCalculatorScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("charts") {
                            ChartsHistoryScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("alerts") {
                            PriceAlertsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("hallmark") {
                            HallmarkVerificationScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
