package com.goldsilver.livecalc.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldsilver.livecalc.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the splash screen hold condition for [androidx.core.splashscreen.SplashScreen].
 *
 * Keeps the system splash on screen until cached rates (Room) and preferences
 * are loaded into memory, with a hard cap of 1500 ms.
 *
 * IMPORTANT: This ViewModel NEVER waits for the network.
 */
class SplashViewModel @JvmOverloads constructor(
    application: Application,
    private val cacheLoader: (suspend () -> Unit)? = null
) : AndroidViewModel(application) {

    companion object {
        const val MAX_SPLASH_HOLD_MS = 1500L
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            // Hard cap of 1500 ms: guarantee dismissal even on slow disk I/O
            val timeoutJob = launch {
                delay(MAX_SPLASH_HOLD_MS)
                _isReady.value = true
            }

            // Load local Room cached rates and preferences asynchronously without touching network
            launch(Dispatchers.IO) {
                try {
                    if (cacheLoader != null) {
                        cacheLoader.invoke()
                    } else {
                        // 1. Warm up preferences cache
                        val prefs = getApplication<Application>().getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)
                        prefs.all

                        // 2. Query Room for any cached rates
                        val database = AppDatabase.getDatabase(getApplication())
                        database.rateDao().getLatestRate()
                    }
                } catch (_: Exception) {
                    // Fallback silently if disk read fails
                } finally {
                    _isReady.value = true
                    timeoutJob.cancel()
                }
            }
        }
    }
}
