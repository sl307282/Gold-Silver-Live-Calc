package com.goldsilver.livecalc.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldsilver.livecalc.GoldSilverApplication
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import com.goldsilver.livecalc.data.local.entities.RateEntity
import com.goldsilver.livecalc.data.local.entities.VerificationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.goldsilver.livecalc.ui.theme.isSystemDarkThemeGlobal
import com.goldsilver.livecalc.ui.theme.selectedThemeGlobal

class GoldSilverViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GoldSilverApplication
    private val repository = app.repository

    private val sharedPrefs = application.getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)

    // UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // App Update states
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _latestVersionName = MutableStateFlow("")
    val latestVersionName: StateFlow<String> = _latestVersionName.asStateFlow()

    private val _latestVersionCode = MutableStateFlow(0)

    private val _updateMessage = MutableStateFlow("")
    val updateMessage: StateFlow<String> = _updateMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Settings States
    val currency = MutableStateFlow(sharedPrefs.getString("currency", "INR") ?: "INR")
    val language = MutableStateFlow(sharedPrefs.getString("language", "English") ?: "English")
    val isNotificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications_enabled", true))
    val isPremium = MutableStateFlow(true)
    val isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", true))
    val backgroundTheme = MutableStateFlow(sharedPrefs.getString("background_theme", "Night") ?: "Night")
    val firebaseDatabaseUrl = MutableStateFlow(sharedPrefs.getString("firebase_db_url", "https://gold-silver-live-calc-default-rtdb.firebaseio.com/") ?: "https://gold-silver-live-calc-default-rtdb.firebaseio.com/")

    // Rates Data
    val latestRate: StateFlow<RateEntity?> = repository.getLatestRateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val historicalRates: StateFlow<List<RateEntity>> = repository.getHistoricalRatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiError: StateFlow<String?> = repository.apiError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Alerts Data
    val alerts: StateFlow<List<AlertEntity>> = repository.getAllAlertsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hallmark Verification Data
    val verifications: StateFlow<List<VerificationEntity>> = repository.getAllVerificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (!sharedPrefs.getBoolean("currency_default_inr_v1", false)) {
            sharedPrefs.edit()
                .putString("currency", "INR")
                .putBoolean("currency_default_inr_v1", true)
                .apply()
            currency.value = "INR"
        }

        if (!sharedPrefs.getBoolean("theme_default_night_v3", false)) {
            sharedPrefs.edit()
                .putString("background_theme", "Night")
                .putBoolean("dark_mode", true)
                .putBoolean("theme_default_night_v3", true)
                .apply()
        }

        // Sync dark theme global state
        val savedTheme = sharedPrefs.getString("background_theme", "Night") ?: "Night"
        selectedThemeGlobal = savedTheme
        isSystemDarkThemeGlobal = (savedTheme == "Night")
        isDarkMode.value = (savedTheme == "Night")

        // Sync Firebase DB url to repository
        repository.firebaseDatabaseUrl = firebaseDatabaseUrl.value

        viewModelScope.launch {
            if (!sharedPrefs.getBoolean("rates_db_cleared_for_indian_premium_v2", false)) {
                repository.clearAllRates()
                sharedPrefs.edit().putBoolean("rates_db_cleared_for_indian_premium_v2", true).apply()
            }
            // Seed base 30 days data if DB is empty
            repository.seedHistoricalDataIfEmpty(currency.value)
            // Fetch newest rate
            refreshRates(force = false)
        }

        // Check for app updates every time the app starts
        viewModelScope.launch {
            try {
                val config = repository.fetchRemoteConfig()
                val latestCode = config["latest_version_code"] as? Int ?: 1
                val latestName = config["latest_version"] as? String ?: "1.0.0"
                val message = config["update_message"] as? String ?: ""

                if (latestCode > com.goldsilver.livecalc.BuildConfig.VERSION_CODE) {
                    _latestVersionCode.value = latestCode
                    _latestVersionName.value = latestName
                    _updateMessage.value = message
                    _showUpdateDialog.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var refreshJob: kotlinx.coroutines.Job? = null

    // Refresh rates
    fun refreshRates(force: Boolean = true) {
        val hasInternet = isInternetAvailable()
        val dbUrl = firebaseDatabaseUrl.value.trim()

        if (dbUrl.isNotBlank() && !hasInternet) {
            if (force) {
                android.widget.Toast.makeText(
                    app,
                    "No internet connection. Please try again later.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (force) _isRefreshing.value = true else _isLoading.value = true
            val result = repository.fetchRates(currency.value)
            if (force) {
                _isRefreshing.value = false
                if (result.isFailure) {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to refresh rates."
                    android.widget.Toast.makeText(
                        app,
                        errorMsg,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    // Settings modifiers
    fun setCurrency(newCurrency: String) {
        currency.value = newCurrency
        sharedPrefs.edit().putString("currency", newCurrency).apply()
        // Re-fetch rates in new currency
        refreshRates(force = false)
    }

    fun setLanguage(newLanguage: String) {
        language.value = newLanguage
        sharedPrefs.edit().putString("language", newLanguage).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        isNotificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        isSystemDarkThemeGlobal = enabled
        sharedPrefs.edit().putBoolean("dark_mode", enabled).apply()
        val newTheme = if (enabled) "Night" else "Ivory"
        backgroundTheme.value = newTheme
        selectedThemeGlobal = newTheme
        sharedPrefs.edit().putString("background_theme", newTheme).apply()
    }

    fun setBackgroundTheme(newTheme: String) {
        backgroundTheme.value = newTheme
        selectedThemeGlobal = newTheme
        isSystemDarkThemeGlobal = (newTheme == "Night")
        isDarkMode.value = (newTheme == "Night")
        sharedPrefs.edit().putString("background_theme", newTheme).apply()
        sharedPrefs.edit().putBoolean("dark_mode", newTheme == "Night").apply()
    }

    fun setPremium(premium: Boolean) {
        isPremium.value = true
        sharedPrefs.edit().putBoolean("premium_unlocked", true).apply()
    }

    fun setFirebaseDatabaseUrl(url: String) {
        firebaseDatabaseUrl.value = url
        repository.firebaseDatabaseUrl = url
        sharedPrefs.edit().putString("firebase_db_url", url).apply()
        // Refresh rates with new database endpoint
        refreshRates(force = false)
    }

    // Alerts operations
    fun addAlert(metal: String, targetPrice: Double, condition: String) {
        viewModelScope.launch {
            val alert = AlertEntity(
                metal = metal,
                targetPrice = targetPrice,
                condition = condition
            )
            repository.insertAlert(alert)
        }
    }

    fun removeAlert(alert: AlertEntity) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun removeAlertById(id: Int) {
        viewModelScope.launch {
            repository.deleteAlertById(id)
        }
    }

    // Hallmark operations
    fun verifyHallmark(huid: String) {
        viewModelScope.launch {
            val verification = VerificationEntity(
                huid = huid,
                status = "Verified" // Simulated BIS result status
            )
            repository.insertVerification(verification)
        }
    }

    fun clearHallmarkHistory() {
        viewModelScope.launch {
            repository.clearVerificationHistory()
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    private fun isInternetAvailable(): Boolean {
        return try {
            val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun isVersionOutdated(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (curr > lat) return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
