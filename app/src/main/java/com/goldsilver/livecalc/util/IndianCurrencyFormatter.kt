package com.goldsilver.livecalc.util

import java.util.Locale

object IndianCurrencyFormatter {

    /**
     * Formats an integer digit string into Indian numbering system (3-2-2 grouping).
     * e.g., "1000" -> "1,000"
     * e.g., "10000" -> "10,000"
     * e.g., "100000" -> "1,00,000"
     * e.g., "1000000" -> "10,00,000"
     * e.g., "10000000" -> "1,00,00,000"
     */
    fun formatIndianIntegerString(digits: String): String {
        if (digits.isEmpty()) return ""
        val cleanDigits = digits.trimStart('0')
        if (cleanDigits.isEmpty()) return "0"
        if (cleanDigits.length <= 3) return cleanDigits

        val last3 = cleanDigits.takeLast(3)
        val remaining = cleanDigits.dropLast(3)
        val sb = StringBuilder()

        var i = remaining.length
        while (i > 0) {
            val start = (i - 2).coerceAtLeast(0)
            if (sb.isNotEmpty()) {
                sb.insert(0, ",")
            }
            sb.insert(0, remaining.substring(start, i))
            i -= 2
        }

        return "$sb,$last3"
    }

    /**
     * Formats a Double value into Indian numbering format with specified decimal places.
     * e.g., 14748.96 -> "14,748.96"
     * e.g., 237.32 -> "237.32"
     * e.g., 100000.0 -> "1,00,000.00"
     */
    fun formatAmount(amount: Double, decimalPlaces: Int = 2): String {
        if (amount.isNaN() || amount.isInfinite()) return if (decimalPlaces > 0) "0." + "0".repeat(decimalPlaces) else "0"
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)

        val formattedString = if (decimalPlaces > 0) {
            String.format(Locale.US, "%.${decimalPlaces}f", absAmount)
        } else {
            kotlin.math.round(absAmount).toLong().toString()
        }

        val parts = formattedString.split(".", limit = 2)
        val intFormatted = formatIndianIntegerString(parts[0])
        val prefix = if (isNegative) "-" else ""

        return if (parts.size > 1) {
            "$prefix$intFormatted.${parts[1]}"
        } else {
            "$prefix$intFormatted"
        }
    }

    /**
     * Formats user input dynamically in real-time as they type into a TextField.
     * Preserves single decimal points and trailing dots during typing.
     * e.g., "1000" -> "1,000"
     * e.g., "100000" -> "1,00,000"
     * e.g., "14748." -> "14,748."
     * e.g., "14748.96" -> "14,748.96"
     */
    fun formatInput(input: String): String {
        val clean = cleanInput(input)
        if (clean.isEmpty()) return ""
        
        val isNegative = clean.startsWith("-")
        val numberPart = if (isNegative) clean.substring(1) else clean

        val parts = numberPart.split(".", limit = 2)
        val intPartString = parts[0]
        val hasDecimal = parts.size > 1 || numberPart.endsWith(".")
        val decimalPartString = if (parts.size > 1) parts[1] else ""

        val formattedInt = if (intPartString.isNotEmpty()) {
            formatIndianIntegerString(intPartString)
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
     * Strips all commas and whitespace from an input string for calculations / API calls.
     */
    fun cleanInput(input: String): String {
        return input.replace(",", "").replace(" ", "").trim()
    }

    /**
     * Parses a formatted string to a Double safely.
     */
    fun parseAmount(input: String): Double {
        val clean = cleanInput(input)
        return clean.toDoubleOrNull() ?: 0.0
    }
}
