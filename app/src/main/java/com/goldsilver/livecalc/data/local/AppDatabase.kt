package com.goldsilver.livecalc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.goldsilver.livecalc.data.local.daos.AlertDao
import com.goldsilver.livecalc.data.local.daos.RateDao
import com.goldsilver.livecalc.data.local.daos.VerificationDao
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import com.goldsilver.livecalc.data.local.entities.RateEntity
import com.goldsilver.livecalc.data.local.entities.VerificationEntity

@Database(
    entities = [RateEntity::class, AlertEntity::class, VerificationEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rateDao(): RateDao
    abstract fun alertDao(): AlertDao
    abstract fun verificationDao(): VerificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gold_silver_calc_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
