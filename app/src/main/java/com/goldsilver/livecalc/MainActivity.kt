package com.goldsilver.livecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goldsilver.livecalc.ui.components.AppUpdateDialog
import com.goldsilver.livecalc.ui.components.CustomBottomNavigation
import com.goldsilver.livecalc.ui.screens.*
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.ui.viewmodel.SplashViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: GoldSilverViewModel by viewModels()
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install androidx.core:core-splashscreen
        val splashScreen = installSplashScreen()
        // Hold system splash until cached Room rates and preferences are ready (max 1500 ms)
        splashScreen.setKeepOnScreenCondition {
            !splashViewModel.isReady.value
        }

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)
        actionBar?.hide()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        setContent {
            val language by viewModel.language.collectAsStateWithLifecycle()
            val appStrings = remember(language) { com.goldsilver.livecalc.util.getAppStrings(language) }
            val isRtl = remember(language) { com.goldsilver.livecalc.util.isRtlLanguage(language) }
            val layoutDirection = if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

            CompositionLocalProvider(
                com.goldsilver.livecalc.util.LocalAppStrings provides appStrings,
                androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
            ) {
                GoldSilverLiveCalcTheme {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val isReduced = remember { isReducedMotion(context) }
                    var isSplashContinuationVisible by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        val startTime = System.currentTimeMillis()
                        while (!viewModel.isInitialLoadComplete.value && (System.currentTimeMillis() - startTime) < 800L) {
                            delay(50L)
                        }
                        if (!isReduced) {
                            delay(150L)
                        }
                        isSplashContinuationVisible = false
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.setNotificationsEnabled(true)
                    }
                }

                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
                val latestVersionName by viewModel.latestVersionName.collectAsStateWithLifecycle()
                val updateMessage by viewModel.updateMessage.collectAsStateWithLifecycle()

                if (showUpdateDialog) {
                    AppUpdateDialog(
                        versionName = latestVersionName,
                        updateMessage = updateMessage,
                        onDismiss = { viewModel.dismissUpdateDialog() }
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
                    containerColor = DarkBackground,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        composable("dashboard") {
                            HomeDashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { route ->
                                    navController.navigate(route)
                                }
                            )
                        }
                        composable(
                            route = "gold_calc?purity={purity}",
                            arguments = listOf(navArgument("purity") {
                                type = NavType.StringType
                                defaultValue = "24K"
                            })
                        ) { backStackEntry ->
                            val purity = backStackEntry.arguments?.getString("purity") ?: "24K"
                            GoldCalculatorScreen(
                                viewModel = viewModel,
                                initialPurity = purity,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "silver_calc?purity={purity}",
                            arguments = listOf(navArgument("purity") {
                                type = NavType.StringType
                                defaultValue = "999"
                            })
                        ) { backStackEntry ->
                            val purity = backStackEntry.arguments?.getString("purity") ?: "999"
                            SilverCalculatorScreen(
                                viewModel = viewModel,
                                initialPurity = purity,
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
                            HallmarkScreen(
                                onNavigateToCalculator = { isGold, purity ->
                                    val route = if (isGold) "gold_calc?purity=$purity" else "silver_calc?purity=$purity"
                                    navController.navigate(route)
                                }
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

                // Branded splash continuation overlay
                AnimatedVisibility(
                    visible = isSplashContinuationVisible,
                    enter = EnterTransition.None,
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = if (isReduced) 0 else 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                ) {
                    SplashContent()
                }
            }
        }
    }
}
}
}
