package com.goldsilver.livecalc.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPrefs = context.getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)
            val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)
            if (isNotificationsEnabled) {
                scheduleBackgroundWork(context)
            }
        }
    }

    companion object {
        fun scheduleBackgroundWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RateUpdateWorker>(24, TimeUnit.HOURS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "rate_update_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelBackgroundWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("rate_update_work")
        }
    }
}
