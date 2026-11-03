package com.goldsilver.livecalc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.viewModels
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
import com.goldsilver.livecalc.ui.components.LocalTabReselectFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.goldsilver.livecalc.ui.screens.*
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : AppCompatActivity() {
    private val viewModel: GoldSilverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("gold_silver_prefs", android.content.Context.MODE_PRIVATE)
        val savedTheme = sharedPrefs.getString("background_theme", "Night") ?: "Night"
        selectedThemeGlobal = savedTheme
        isSystemDarkThemeGlobal = (savedTheme == "Night")
        com.goldsilver.livecalc.util.ThemeTransition.applyThemeToWindow(this, savedTheme)

        val isColdStart = (savedInstanceState == null)

        var isMinimumTimePassed = !isColdStart
        if (isColdStart) {
            // Ensure the splash screen stays visible for at least 1.5 seconds on initial cold launch
            lifecycleScope.launch {
                delay(1500)
                isMinimumTimePassed = true
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            if (isColdStart) {
                // Do not dismiss until both the minimum time has passed AND the required data is loaded
                splashScreen.setKeepOnScreenCondition { 
                    !isMinimumTimePassed || !viewModel.isInitialLoadComplete.value 
                }
                
                // Override the default Android zoom-out with a smooth, cinematic crossfade
                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    val fadeOut = android.animation.ObjectAnimator.ofFloat(
                        splashScreenView.view,
                        android.view.View.ALPHA,
                        1f,
                        0f
                    )
                    fadeOut.duration = 600L // Smooth 600ms fade
                    fadeOut.doOnEnd { splashScreenView.remove() }
                    fadeOut.start()
                }
            } else {
                // On activity recreation (e.g. language change), immediately draw without holding splash
                splashScreen.setKeepOnScreenCondition { false }
            }
        }
        
        setTheme(R.style.Theme_GoldSilverLiveCalc)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        // On Android 11 and below, manually delay the first frame draw to hold the legacy splash screen only on cold start
        if (isColdStart && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            val content = findViewById<android.view.View>(android.R.id.content)
            content.viewTreeObserver.addOnPreDrawListener(
                object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        return if (isMinimumTimePassed && viewModel.isInitialLoadComplete.value) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            true
                        } else {
                            false
                        }
                    }
                }
            )
        }

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

                val tabReselectFlow = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }

                CompositionLocalProvider(LocalTabReselectFlow provides tabReselectFlow) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = DarkBackground,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (showBottomBar) {
                                CustomBottomNavigation(
                                    navController = navController,
                                    currentRoute = currentRoute
                                )
                            }
                        }
                    ) { innerPadding ->
                        val safeNavigateBack: () -> Unit = remember(navController) {
                            var lastClickTime = 0L
                            {
                                val now = android.os.SystemClock.elapsedRealtime()
                                if (now - lastClickTime > 500L) {
                                    lastClickTime = now
                                    if (!navController.popBackStack()) {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") {
                                                saveState = false
                                                inclusive = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                                }
                            }
                        }

                    val bottomBarPadding = innerPadding.calculateBottomPadding()

                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("dashboard") {
                            HomeDashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { route ->
                                    navController.navigate(route)
                                },
                                bottomBarPadding = bottomBarPadding
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
                                onBack = safeNavigateBack
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
                                onBack = safeNavigateBack
                            )
                        }
                        composable("charts") {
                            ChartsHistoryScreen(
                                viewModel = viewModel,
                                onBack = safeNavigateBack,
                                bottomBarPadding = bottomBarPadding
                            )
                        }
                        composable("alerts") {
                            PriceAlertsScreen(
                                viewModel = viewModel,
                                onBack = safeNavigateBack
                            )
                        }
                        composable("hallmark") {
                            HallmarkScreen(
                                onNavigateToCalculator = { isGold, purity ->
                                    val route = if (isGold) "gold_calc?purity=$purity" else "silver_calc?purity=$purity"
                                    navController.navigate(route)
                                },
                                onBack = safeNavigateBack,
                                bottomBarPadding = bottomBarPadding
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = safeNavigateBack,
                                bottomBarPadding = bottomBarPadding
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
