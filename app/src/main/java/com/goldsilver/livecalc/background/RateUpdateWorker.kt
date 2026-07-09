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
        val repository = MetalRepository(
            applicationContext,
            database.rateDao(),
            database.alertDao(),
            database.verificationDao()
        )

        // Get saved currency (default INR) and firebase database url
        val sharedPrefs = applicationContext.getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)
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
