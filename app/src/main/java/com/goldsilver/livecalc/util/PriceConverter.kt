package com.goldsilver.livecalc.util

import com.goldsilver.livecalc.data.local.entities.RateEntity
import java.util.Locale

object PriceConverter {

    /**
     * Formats an amount with currency symbol/code and optional "/ g" suffix.
     * e.g. "₹13,777.65 / g" for INR, "$165.60 / g" for USD, "AED 608.20 / g" for AED.
     */
    fun format(amount: Double, currency: String, perGram: Boolean = true): String {
        val formattedCurrency = CurrencyFormatter.formatCurrency(amount, currency)
        return if (perGram) "$formattedCurrency / g" else formattedCurrency
    }

    /**
     * Formats an amount with currency symbol and localized digit grouping.
     * e.g. "₹423.10", "$5.20", "AED 18.50"
     */
    fun formatAmount(amount: Double, currency: String): String {
        return CurrencyFormatter.formatCurrency(amount, currency)
    }

    /**
     * Formats the absolute value of an amount with currency symbol.
     * e.g. formatAbsolute(-423.10, "INR") -> "₹423.10"
     */
    fun formatAbsolute(amount: Double, currency: String): String {
        return CurrencyFormatter.formatCurrency(kotlin.math.abs(amount), currency)
    }

    /**
     * Converts a USD rate entity to targetCurrency at today's FX rate.
     * FX Ratio = todayTargetRate / todayUsdRate
     */
    fun convertRateEntityAtTodayFx(
        rate: RateEntity,
        fromCurrency: String = "USD",
        toCurrency: String
    ): RateEntity {
        val from = fromCurrency.trim().uppercase(Locale.US)
        val to = toCurrency.trim().uppercase(Locale.US)
        if (from == to) return rate

        val goldRatio = CurrencyConverter.getGoldRate(to) / CurrencyConverter.getGoldRate(from)
        val silverRatio = CurrencyConverter.getSilverRate(to) / CurrencyConverter.getSilverRate(from)

        return rate.copy(
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
     * Converts a single price amount between currencies at today's rate.
     */
    fun convertAmountAtTodayFx(
        amount: Double,
        fromCurrency: String = "USD",
        toCurrency: String,
        isGold: Boolean = true
    ): Double {
        val from = fromCurrency.trim().uppercase(Locale.US)
        val to = toCurrency.trim().uppercase(Locale.US)
        if (from == to || amount <= 0.0) return amount
        val ratio = if (isGold) {
            CurrencyConverter.getGoldRate(to) / CurrencyConverter.getGoldRate(from)
        } else {
            CurrencyConverter.getSilverRate(to) / CurrencyConverter.getSilverRate(from)
        }
        return amount * ratio
    }
}
