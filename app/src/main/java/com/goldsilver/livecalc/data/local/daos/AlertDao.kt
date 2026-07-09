package com.goldsilver.livecalc.data.local.daos

import androidx.room.*
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    suspend fun getActiveAlerts(): List<AlertEntity>

    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlertById(alertId: Int)
}
