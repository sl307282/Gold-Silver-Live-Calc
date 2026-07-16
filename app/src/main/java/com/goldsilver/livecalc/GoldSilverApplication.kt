package com.goldsilver.livecalc

import android.app.Application
import com.goldsilver.livecalc.background.BootReceiver
import com.goldsilver.livecalc.data.local.AppDatabase
import com.goldsilver.livecalc.data.repository.MetalRepository

class GoldSilverApplication : Application() {

    lateinit var repository: MetalRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        val database = AppDatabase.getDatabase(this)
        repository = MetalRepository(
            this,
            database.rateDao(),
            database.alertDao(),
            database.verificationDao()
        )

        // Schedule or cancel WorkManager rate updates based on preference
        val sharedPrefs = getSharedPreferences("gold_silver_prefs", MODE_PRIVATE)
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)
        if (isNotificationsEnabled) {
            BootReceiver.scheduleBackgroundWork(this)
        } else {
            BootReceiver.cancelBackgroundWork(this)
        }
    }
}
