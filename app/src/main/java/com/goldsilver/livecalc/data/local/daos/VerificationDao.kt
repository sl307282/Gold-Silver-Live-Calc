package com.goldsilver.livecalc.data.local.daos

import androidx.room.*
import com.goldsilver.livecalc.data.local.entities.VerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: VerificationEntity)

    @Query("SELECT * FROM hallmark_verifications ORDER BY verificationTime DESC")
    fun getAllVerificationsFlow(): Flow<List<VerificationEntity>>

    @Query("DELETE FROM hallmark_verifications")
    suspend fun clearHistory()
}
