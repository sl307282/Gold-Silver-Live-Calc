package com.goldsilver.livecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
                val latestVersionName by viewModel.latestVersionName.collectAsState()
                val updateMessage by viewModel.updateMessage.collectAsState()

                if (showUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissUpdateDialog() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Update Available",
                                    tint = com.goldsilver.livecalc.ui.theme.GoldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Update Available!",
                                    color = com.goldsilver.livecalc.ui.theme.GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        text = {
                            Text(
                                text = updateMessage.ifBlank { "A newer version of Gold & Silver Live Calc (v$latestVersionName) is available. Please update to access the latest market rates, real-time alerts, and performance improvements." },
                                color = com.goldsilver.livecalc.ui.theme.TextPrimary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("market://details?id=com.goldsilver.livecalc")
                                    ).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        val webIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.goldsilver.livecalc")
                                        ).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(webIntent)
                                    }
                                    viewModel.dismissUpdateDialog()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.goldsilver.livecalc.ui.theme.GoldPrimary
                                )
                            ) {
                                Text(
                                    text = "Update Now",
                                    color = com.goldsilver.livecalc.ui.theme.DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                                Text(
                                    text = "Later",
                                    color = com.goldsilver.livecalc.ui.theme.TextSecondary
                                )
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
