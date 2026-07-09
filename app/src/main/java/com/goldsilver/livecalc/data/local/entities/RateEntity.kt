package com.goldsilver.livecalc.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "metal_rates")
data class RateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val goldPrice24k: Double, // Per Gram base price
    val goldPrice22k: Double, // Per Gram
    val goldPrice18k: Double, // Per Gram
    val goldPrice14k: Double, // Per Gram
    val silverPrice: Double,   // Per Gram
    val currency: String = "USD"
)
