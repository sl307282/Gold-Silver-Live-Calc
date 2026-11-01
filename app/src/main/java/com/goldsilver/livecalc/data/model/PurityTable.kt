package com.goldsilver.livecalc.data.model

data class PurityItem(
    val metal: String,
    val carat: String,
    val fineness: String,
    val purityPercent: String,
    val factor: Double,
    val isGold: Boolean
)

object PurityTable {
    val GOLD_24K = PurityItem(
        metal = "Gold",
        carat = "24K",
        fineness = "999",
        purityPercent = "99.9%",
        factor = 1.0,
        isGold = true
    )
    val GOLD_22K = PurityItem(
        metal = "Gold",
        carat = "22K",
        fineness = "916",
        purityPercent = "91.6%",
        factor = 0.9167,
        isGold = true
    )
    val GOLD_18K = PurityItem(
        metal = "Gold",
        carat = "18K",
        fineness = "750",
        purityPercent = "75.0%",
        factor = 0.75,
        isGold = true
    )
    val GOLD_14K = PurityItem(
        metal = "Gold",
        carat = "14K",
        fineness = "585",
        purityPercent = "58.3%",
        factor = 0.5833,
        isGold = true
    )
    val SILVER_999 = PurityItem(
        metal = "Silver",
        carat = "999",
        fineness = "999",
        purityPercent = "99.9%",
        factor = 1.0,
        isGold = false
    )
    val SILVER_925 = PurityItem(
        metal = "Silver",
        carat = "925",
        fineness = "925",
        purityPercent = "92.5%",
        factor = 0.925,
        isGold = false
    )

    val ALL_ITEMS: List<PurityItem> = listOf(
        GOLD_24K,
        GOLD_22K,
        GOLD_18K,
        GOLD_14K,
        SILVER_999,
        SILVER_925
    )
}
