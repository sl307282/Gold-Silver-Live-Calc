package com.goldsilver.livecalc.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hallmark_verifications")
data class VerificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val huid: String,
    val verificationTime: Long = System.currentTimeMillis(),
    val status: String = "Checked"
)
