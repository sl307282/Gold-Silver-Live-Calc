package com.goldsilver.livecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goldsilver.livecalc.ui.components.AppUpdateDialog
import com.goldsilver.livecalc.ui.components.CustomBottomNavigation
import com.goldsilver.livecalc.ui.screens.*
import com.goldsilver.livecalc.ui.theme.GoldSilverLiveCalcTheme
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: GoldSilverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ensure the splash screen stays visible for at least 1.5 seconds
        var isMinimumTimePassed = false
        lifecycleScope.launch {
            delay(1500)
            isMinimumTimePassed = true
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
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
            // For Android 11 and below, we bypass the AndroidX library completely to avoid double logos
            // and use our pixel-perfect layered windowBackground. We simply switch to the main theme here.
            setTheme(R.style.Theme_GoldSilverLiveCalc)
        }

        super.onCreate(savedInstanceState)
        
        // On Android 11 and below, manually delay the first frame draw to hold the legacy splash screen
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
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
}
