package com.goldsilver.livecalc.util

import java.util.Locale

object CurrencyFormatter {

    /**
     * Returns the appropriate currency symbol for display.
     */
    fun getCurrencySymbol(currency: String): String {
        return when (currency.trim().uppercase(Locale.US)) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "AED" -> "AED "
            else -> "$currency "
        }
    }

    /**
     * Formats an amount with the correct digit grouping:
     * - INR uses Indian numbering grouping (3-2-2) e.g. "1,37,776.50"
     * - Other currencies use standard international grouping (3-3) e.g. "137,776.50"
     */
    fun formatAmount(amount: Double, currency: String = "INR", decimalPlaces: Int = 2): String {
        if (amount.isNaN() || amount.isInfinite()) {
            return if (decimalPlaces > 0) "0." + "0".repeat(decimalPlaces) else "0"
        }
        val cleanCurrency = currency.trim().uppercase(Locale.US)
        return if (cleanCurrency == "INR") {
            IndianCurrencyFormatter.formatAmount(amount, decimalPlaces)
        } else {
            val isNegative = amount < 0
            val absAmount = kotlin.math.abs(amount)
            val formatted = String.format(Locale.US, "%,.${decimalPlaces}f", absAmount)
            if (isNegative) "-$formatted" else formatted
        }
    }

    /**
     * Formats an amount with currency symbol and localized digit grouping.
     * e.g., formatCurrency(13777.65, "INR") -> "₹13,777.65"
     * e.g., formatCurrency(124.61, "USD") -> "$124.61"
     * e.g., formatCurrency(111.59, "EUR") -> "€111.59"
     * e.g., formatCurrency(457.61, "AED") -> "AED 457.61"
     * e.g., formatCurrency(93.09, "GBP") -> "£93.09"
     */
    fun formatCurrency(amount: Double, currency: String, decimalPlaces: Int = 2): String {
        val cleanCurrency = currency.trim().uppercase(Locale.US)
        val symbol = getCurrencySymbol(cleanCurrency)
        val formattedNumber = formatAmount(amount, cleanCurrency, decimalPlaces)
        return if (cleanCurrency == "AED") {
            "AED $formattedNumber"
        } else {
            "$symbol$formattedNumber"
        }
    }

    /**
     * Formats user input dynamically in real-time as they type into a TextField.
     * Preserves single decimal points and trailing dots during typing.
     */
    fun formatInput(input: String, currency: String = "INR"): String {
        val clean = cleanInput(input)
        if (clean.isEmpty()) return ""
        val cleanCurrency = currency.trim().uppercase(Locale.US)
        if (cleanCurrency == "INR") {
            return IndianCurrencyFormatter.formatInput(input)
        }

        val isNegative = clean.startsWith("-")
        val numberPart = if (isNegative) clean.substring(1) else clean

        val parts = numberPart.split(".", limit = 2)
        val intPartString = parts[0]
        val hasDecimal = parts.size > 1 || numberPart.endsWith(".")
        val decimalPartString = if (parts.size > 1) parts[1] else ""

        val formattedInt = if (intPartString.isNotEmpty()) {
            val cleanDigits = intPartString.trimStart('0')
            if (cleanDigits.isEmpty()) "0" else {
                val sb = StringBuilder()
                var count = 0
                for (i in cleanDigits.length - 1 downTo 0) {
                    sb.append(cleanDigits[i])
                    count++
                    if (count % 3 == 0 && i > 0) {
                        sb.append(',')
                    }
                }
                sb.reverse().toString()
            }
        } else {
            "0"
        }

        val result = StringBuilder()
        if (isNegative) result.append("-")
        result.append(formattedInt)
        if (hasDecimal) {
            result.append(".")
            result.append(decimalPartString)
        }
        return result.toString()
    }

    /**
     * Strips commas and spaces from user input.
     */
    fun cleanInput(input: String): String {
        return IndianCurrencyFormatter.cleanInput(input)
    }

    /**
     * Parses a string input into a Double safely.
     */
    fun parseAmount(input: String): Double {
        return IndianCurrencyFormatter.parseAmount(input)
    }
}
