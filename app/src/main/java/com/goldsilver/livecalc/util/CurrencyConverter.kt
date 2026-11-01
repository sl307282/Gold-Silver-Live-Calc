package com.goldsilver.livecalc.util

import com.goldsilver.livecalc.data.local.entities.RateEntity
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object CurrencyConverter {

    // Base market calibration per gram (fallback when live cache is cold)
    private val BASE_GOLD_PER_GRAM = mapOf(
        "USD" to 124.61,
        "INR" to 13777.65,
        "EUR" to 111.59,
        "AED" to 457.61,
        "GBP" to 93.09
    )

    private val BASE_SILVER_PER_GRAM = mapOf(
        "USD" to 1.58,
        "INR" to 190.29,
        "EUR" to 1.41,
        "AED" to 5.80,
        "GBP" to 1.18
    )

    // Dynamic cache updated whenever live rates are fetched from Firebase
    private val liveGoldPerGram = ConcurrentHashMap<String, Double>(BASE_GOLD_PER_GRAM)
    private val liveSilverPerGram = ConcurrentHashMap<String, Double>(BASE_SILVER_PER_GRAM)

    /**
     * Registers a live rate into the conversion memory.
     */
    fun registerRate(rate: RateEntity) {
        val curr = rate.currency.trim().uppercase(Locale.US)
        if (curr.isNotBlank()) {
            if (rate.goldPrice24k > 0) liveGoldPerGram[curr] = rate.goldPrice24k
            if (rate.silverPrice > 0) liveSilverPerGram[curr] = rate.silverPrice
        }
    }

    fun getGoldRate(currency: String): Double {
        val curr = currency.trim().uppercase(Locale.US)
        return liveGoldPerGram[curr] ?: BASE_GOLD_PER_GRAM[curr] ?: 124.61
    }

    fun getSilverRate(currency: String): Double {
        val curr = currency.trim().uppercase(Locale.US)
        return liveSilverPerGram[curr] ?: BASE_SILVER_PER_GRAM[curr] ?: 1.58
    }

    /**
     * Converts a numerical gold amount from one currency to another.
     */
    fun convertGoldAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val from = fromCurrency.trim().uppercase(Locale.US)
        val to = toCurrency.trim().uppercase(Locale.US)
        if (from == to || amount <= 0) return amount

        val fromRate = getGoldRate(from)
        val toRate = getGoldRate(to)
        if (fromRate <= 0) return amount
        return (amount / fromRate) * toRate
    }

    /**
     * Converts a numerical silver amount from one currency to another.
     */
    fun convertSilverAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val from = fromCurrency.trim().uppercase(Locale.US)
        val to = toCurrency.trim().uppercase(Locale.US)
        if (from == to || amount <= 0) return amount

        val fromRate = getSilverRate(from)
        val toRate = getSilverRate(to)
        if (fromRate <= 0) return amount
        return (amount / fromRate) * toRate
    }

    /**
     * Converts an entire RateEntity from its stored currency to targetCurrency.
     * Guaranteed to never modify the source entity or double-convert.
     */
    fun convertRateEntity(rate: RateEntity, targetCurrency: String): RateEntity {
        val from = rate.currency.trim().uppercase(Locale.US)
        val to = targetCurrency.trim().uppercase(Locale.US)
        if (from == to) return rate

        registerRate(rate)

        val goldRatio = getGoldRate(to) / getGoldRate(from)
        val silverRatio = getSilverRate(to) / getSilverRate(from)

        return rate.copy(
            id = rate.id,
            timestamp = rate.timestamp,
            date = rate.date,
            unit = rate.unit,
            currency = to,
            goldPrice24k = rate.goldPrice24k * goldRatio,
            goldPrice22k = rate.goldPrice22k * goldRatio,
            goldPrice18k = rate.goldPrice18k * goldRatio,
            goldPrice14k = rate.goldPrice14k * goldRatio,
            silverPrice = rate.silverPrice * silverRatio,
            prevTradingDayGoldPrice = rate.prevTradingDayGoldPrice?.times(goldRatio),
            prevTradingDaySilverPrice = rate.prevTradingDaySilverPrice?.times(silverRatio)
        )
    }

    /**
     * Returns a valid baseline RateEntity for the given currency when no rates are in DB.
     */
    fun getDefaultRateEntity(currency: String): RateEntity {
        val to = currency.trim().uppercase(Locale.US)
        val gold24k = getGoldRate(to)
        val silver = getSilverRate(to)
        return RateEntity(
            timestamp = System.currentTimeMillis(),
            currency = to,
            goldPrice24k = gold24k,
            goldPrice22k = gold24k * 0.9167,
            goldPrice18k = gold24k * 0.75,
            goldPrice14k = gold24k * 0.5833,
            silverPrice = silver,
            prevTradingDayGoldPrice = gold24k * 0.995,
            prevTradingDaySilverPrice = silver * 0.995,
            unit = "gram"
        )
    }
}
