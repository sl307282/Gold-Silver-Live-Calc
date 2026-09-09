package com.goldsilver.livecalc.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goldsilver.livecalc.data.local.entities.RateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: RateEntity)

    @Query("SELECT * FROM metal_rates ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRateFlow(): Flow<RateEntity?>

    @Query("SELECT * FROM metal_rates WHERE currency = :currency ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRateForCurrencyFlow(currency: String): Flow<RateEntity?>

    @Query("SELECT * FROM metal_rates ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRate(): RateEntity?

    @Query("SELECT * FROM metal_rates ORDER BY timestamp ASC")
    fun getHistoricalRatesFlow(): Flow<List<RateEntity>>

    @Query("SELECT * FROM metal_rates WHERE currency = :currency ORDER BY timestamp ASC")
    fun getHistoricalRatesForCurrencyFlow(currency: String): Flow<List<RateEntity>>

    @Query("SELECT * FROM metal_rates ORDER BY timestamp ASC")
    suspend fun getHistoricalRates(): List<RateEntity>

    @androidx.room.Update
    suspend fun updateRate(rate: RateEntity)

    @Query("SELECT * FROM metal_rates WHERE currency = :currency ORDER BY timestamp DESC")
    suspend fun getRatesByCurrency(currency: String): List<RateEntity>

    @Query("DELETE FROM metal_rates WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteRatesOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM metal_rates")
    suspend fun deleteAllRates()
}
