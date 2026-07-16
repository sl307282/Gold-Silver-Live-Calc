package com.goldsilver.livecalc.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goldsilver.livecalc.data.local.AppDatabase
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import com.goldsilver.livecalc.data.repository.MetalRepository

class RateUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val sharedPrefs = applicationContext.getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)

        if (!isNotificationsEnabled) {
            BootReceiver.cancelBackgroundWork(applicationContext)
            return Result.success()
        }

        val activeAlerts = database.alertDao().getActiveAlerts()
        if (activeAlerts.isEmpty()) {
            // Notifications are sent only if at least one active price alert exists
            return Result.success()
        }

        val repository = MetalRepository(
            applicationContext,
            database.rateDao(),
            database.alertDao(),
            database.verificationDao()
        )

        val currency = sharedPrefs.getString("currency", "INR") ?: "INR"
        val firebaseDbUrl = sharedPrefs.getString("firebase_db_url", "https://gold-silver-live-calc-default-rtdb.firebaseio.com/") ?: "https://gold-silver-live-calc-default-rtdb.firebaseio.com/"
        repository.firebaseDatabaseUrl = firebaseDbUrl

        // Fetch rates (updates database and automatically triggers alert checks)
        val fetchResult = repository.fetchRates(currency)

        return if (fetchResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
