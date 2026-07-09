package com.goldsilver.livecalc.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleBackgroundWork(context)
        }
    }

    companion object {
        fun scheduleBackgroundWork(context: Context) {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            // Target 9:15 AM today
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 15)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                // If 9:15 AM has already passed today, target 9:15 AM tomorrow
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = calendar.timeInMillis - now

            val workRequest = PeriodicWorkRequestBuilder<RateUpdateWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "rate_update_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
