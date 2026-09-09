package com.goldsilver.livecalc.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val metal: String,        // "GOLD" or "SILVER"
    val targetPrice: Double,   // Price per gram in selected currency
    val condition: String,     // "ABOVE" or "BELOW"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null
)
