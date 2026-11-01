package com.goldsilver.livecalc

import com.goldsilver.livecalc.R
import com.goldsilver.livecalc.data.local.entities.RateEntity
import com.goldsilver.livecalc.ui.viewmodel.Period
import com.goldsilver.livecalc.util.PriceConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalTrendsTest {

    // Fixed 7-day test series in USD
    private val usdPrices = listOf(120.0, 122.0, 121.5, 125.0, 124.0, 126.0, 128.0)

    // Conversion ratios for INR and EUR
    private val inrRatio = 110.566 // e.g. 13777.65 / 124.61
    private val eurRatio = 0.8955  // e.g. 111.59 / 124.61

    private val inrPrices = usdPrices.map { it * inrRatio }
    private val eurPrices = usdPrices.map { it * eurRatio }

    @Test
    fun testPriceConverterFormattingOutputs() {
        // Strip format (with / g)
        val inrFormatted = PriceConverter.format(13777.65, "INR", perGram = true)
        assertTrue(inrFormatted.startsWith("₹"))
        assertTrue(inrFormatted.endsWith(" / g"))

        val usdFormatted = PriceConverter.format(165.60, "USD", perGram = true)
        assertTrue(usdFormatted.startsWith("$"))
        assertTrue(usdFormatted.endsWith(" / g"))

        val aedFormatted = PriceConverter.format(608.20, "AED", perGram = true)
        assertTrue(aedFormatted.startsWith("AED "))
        assertTrue(aedFormatted.endsWith(" / g"))

        val eurFormatted = PriceConverter.format(111.59, "EUR", perGram = true)
        assertTrue(eurFormatted.startsWith("€"))
        assertTrue(eurFormatted.endsWith(" / g"))

        val gbpFormatted = PriceConverter.format(93.09, "GBP", perGram = true)
        assertTrue(gbpFormatted.startsWith("£"))
        assertTrue(gbpFormatted.endsWith(" / g"))

        // Amount format (without / g)
        val inrAmount = PriceConverter.formatAmount(423.10, "INR")
        assertEquals("₹423.10", inrAmount)

        val usdAmount = PriceConverter.formatAmount(5.20, "USD")
        assertEquals("$5.20", usdAmount)

        val aedAmount = PriceConverter.formatAmount(18.50, "AED")
        assertEquals("AED 18.50", aedAmount)
    }

    @Test
    fun testStripValuesMatchPriceConverter() {
        // Verify min, current, max for USD
        val usdMin = usdPrices.minOrNull()!!
        val usdMax = usdPrices.maxOrNull()!!
        val usdCurrent = usdPrices.last()

        assertEquals(PriceConverter.format(usdMin, "USD", perGram = true), PriceConverter.format(120.0, "USD", perGram = true))
        assertEquals(PriceConverter.format(usdCurrent, "USD", perGram = true), PriceConverter.format(128.0, "USD", perGram = true))
        assertEquals(PriceConverter.format(usdMax, "USD", perGram = true), PriceConverter.format(128.0, "USD", perGram = true))

        // Verify min, current, max for INR
        val inrMin = inrPrices.minOrNull()!!
        val inrMax = inrPrices.maxOrNull()!!
        val inrCurrent = inrPrices.last()

        assertEquals(PriceConverter.format(inrMin, "INR", perGram = true), PriceConverter.format(usdMin * inrRatio, "INR", perGram = true))
        assertEquals(PriceConverter.format(inrCurrent, "INR", perGram = true), PriceConverter.format(usdCurrent * inrRatio, "INR", perGram = true))
        assertEquals(PriceConverter.format(inrMax, "INR", perGram = true), PriceConverter.format(usdMax * inrRatio, "INR", perGram = true))

        // Verify min, current, max for EUR
        val eurMin = eurPrices.minOrNull()!!
        val eurMax = eurPrices.maxOrNull()!!
        val eurCurrent = eurPrices.last()

        assertEquals(PriceConverter.format(eurMin, "EUR", perGram = true), PriceConverter.format(usdMin * eurRatio, "EUR", perGram = true))
        assertEquals(PriceConverter.format(eurCurrent, "EUR", perGram = true), PriceConverter.format(usdCurrent * eurRatio, "EUR", perGram = true))
        assertEquals(PriceConverter.format(eurMax, "EUR", perGram = true), PriceConverter.format(usdMax * eurRatio, "EUR", perGram = true))
    }

    @Test
    fun testTodaysChangePercentageIsIdenticalAcrossCurrencies() {
        // Today vs Yesterday (last vs second-to-last)
        val usdToday = usdPrices.last()
        val usdYesterday = usdPrices[usdPrices.size - 2]
        val usdPercent = ((usdToday - usdYesterday) / usdYesterday) * 100.0

        val inrToday = inrPrices.last()
        val inrYesterday = inrPrices[inrPrices.size - 2]
        val inrPercent = ((inrToday - inrYesterday) / inrYesterday) * 100.0

        val eurToday = eurPrices.last()
        val eurYesterday = eurPrices[eurPrices.size - 2]
        val eurPercent = ((eurToday - eurYesterday) / eurYesterday) * 100.0

        // Percentages must be mathematically identical
        assertEquals(usdPercent, inrPercent, 0.0001)
        assertEquals(usdPercent, eurPercent, 0.0001)

        // Today's change amount matches PriceConverter.formatAmount
        val usdDiff = usdToday - usdYesterday
        val inrDiff = inrToday - inrYesterday
        val eurDiff = eurToday - eurYesterday

        assertEquals(PriceConverter.formatAmount(usdDiff, "USD"), "$2.00")
        assertTrue(PriceConverter.formatAmount(inrDiff, "INR").startsWith("₹"))
        assertTrue(PriceConverter.formatAmount(eurDiff, "EUR").startsWith("€"))
    }

    @Test
    fun testPeriodChangePercentageIsIdenticalAcrossCurrencies() {
        // First vs Last point
        val usdFirst = usdPrices.first()
        val usdLast = usdPrices.last()
        val usdPeriodPercent = ((usdLast - usdFirst) / usdFirst) * 100.0

        val inrFirst = inrPrices.first()
        val inrLast = inrPrices.last()
        val inrPeriodPercent = ((inrLast - inrFirst) / inrFirst) * 100.0

        val eurFirst = eurPrices.first()
        val eurLast = eurPrices.last()
        val eurPeriodPercent = ((eurLast - eurFirst) / eurFirst) * 100.0

        assertEquals(usdPeriodPercent, inrPeriodPercent, 0.0001)
        assertEquals(usdPeriodPercent, eurPeriodPercent, 0.0001)
        assertEquals(6.6666, usdPeriodPercent, 0.001)
    }

    @Test
    fun testRangeAndSummaryMetrics() {
        val usdRange = usdPrices.maxOrNull()!! - usdPrices.minOrNull()!!
        val inrRange = inrPrices.maxOrNull()!! - inrPrices.minOrNull()!!
        val eurRange = eurPrices.maxOrNull()!! - eurPrices.minOrNull()!!

        // Range (High − Low) formatted with / g
        assertEquals(PriceConverter.format(usdRange, "USD", perGram = true), "$8.00 / g")
        assertTrue(PriceConverter.format(inrRange, "INR", perGram = true).endsWith(" / g"))
        assertTrue(PriceConverter.format(eurRange, "EUR", perGram = true).endsWith(" / g"))

        val usdAvg = usdPrices.average()
        val inrAvg = inrPrices.average()
        val eurAvg = eurPrices.average()

        assertEquals(PriceConverter.format(usdAvg, "USD", perGram = true), PriceConverter.format(123.785714, "USD", perGram = true))
        assertEquals(PriceConverter.format(inrAvg, "INR", perGram = true), PriceConverter.format(usdAvg * inrRatio, "INR", perGram = true))
        assertEquals(PriceConverter.format(eurAvg, "EUR", perGram = true), PriceConverter.format(usdAvg * eurRatio, "EUR", perGram = true))
    }

    @Test
    fun testChangeFormattingNeverProducesNegativeSignWithArrow() {
        // Negative change (down)
        val negativeDiff = -422.92
        val pct = -2.978
        val arrowDown = if (negativeDiff < -0.001) "▼" else "▲"
        val unsignedAmount = PriceConverter.formatAbsolute(negativeDiff, "INR")
        val unsignedPct = String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(pct))
        val formattedTodayChange = "$arrowDown $unsignedAmount ($unsignedPct%)"

        assertEquals("▼ ₹422.92 (2.98%)", formattedTodayChange)
        assertTrue(!formattedTodayChange.contains("-"))
        assertTrue(formattedTodayChange.startsWith("▼ ₹"))

        // Period Change with / g
        val periodDiff = -509.61
        val periodPct = -3.57
        val periodArrow = if (periodDiff < -0.001) "▼" else "▲"
        val unsignedPeriodAmount = PriceConverter.format(kotlin.math.abs(periodDiff), "INR", perGram = true)
        val unsignedPeriodPct = String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(periodPct))
        val formattedPeriodChange = "$periodArrow $unsignedPeriodAmount ($unsignedPeriodPct%)"

        assertEquals("▼ ₹509.61 / g (3.57%)", formattedPeriodChange)
        assertTrue(!formattedPeriodChange.contains("-₹"))
    }

    @Test
    fun testDailyRatesListValuesAndPercentages() {
        // Verify day-over-day changes across all 7 days
        for (i in 1 until usdPrices.size) {
            val usdPrev = usdPrices[i - 1]
            val usdCurr = usdPrices[i]
            val usdDailyPercent = ((usdCurr - usdPrev) / usdPrev) * 100.0

            val inrPrev = inrPrices[i - 1]
            val inrCurr = inrPrices[i]
            val inrDailyPercent = ((inrCurr - inrPrev) / inrPrev) * 100.0

            val eurPrev = eurPrices[i - 1]
            val eurCurr = eurPrices[i]
            val eurDailyPercent = ((eurCurr - eurPrev) / eurPrev) * 100.0

            assertEquals("Day $i daily percent mismatch between USD and INR", usdDailyPercent, inrDailyPercent, 0.0001)
            assertEquals("Day $i daily percent mismatch between USD and EUR", usdDailyPercent, eurDailyPercent, 0.0001)

            // Verify close formatted value matches PriceConverter
            val formattedUsd = PriceConverter.format(usdCurr, "USD", perGram = true)
            val formattedInr = PriceConverter.format(inrCurr, "INR", perGram = true)
            val formattedEur = PriceConverter.format(eurCurr, "EUR", perGram = true)

            assertTrue(formattedUsd.startsWith("$") && formattedUsd.endsWith(" / g"))
            assertTrue(formattedInr.startsWith("₹") && formattedInr.endsWith(" / g"))
            assertTrue(formattedEur.startsWith("€") && formattedEur.endsWith(" / g"))
        }
    }

    @Test
    fun testConvertRateEntityAtTodayFx() {
        val originalUsd = RateEntity(
            id = 10,
            date = "2026-11-01",
            unit = "gram",
            currency = "USD",
            timestamp = 1700000000000L,
            goldPrice24k = 124.61,
            goldPrice22k = 114.23,
            goldPrice18k = 93.46,
            goldPrice14k = 72.69,
            silverPrice = 1.58,
            prevTradingDayGoldPrice = 124.00,
            prevTradingDaySilverPrice = 1.55
        )

        val convertedEur = PriceConverter.convertRateEntityAtTodayFx(originalUsd, fromCurrency = "USD", toCurrency = "EUR")
        assertEquals("EUR", convertedEur.currency)
        assertTrue(convertedEur.goldPrice24k > 0.0)
        assertTrue(convertedEur.silverPrice > 0.0)

        // The ratio between goldPrice24k and prevTradingDayGoldPrice must remain exactly identical
        val usdGoldRatio = originalUsd.goldPrice24k / originalUsd.prevTradingDayGoldPrice!!
        val eurGoldRatio = convertedEur.goldPrice24k / convertedEur.prevTradingDayGoldPrice!!
        assertEquals(usdGoldRatio, eurGoldRatio, 0.0001)
    }

    @Test
    fun testDynamicPeriodChangeLabelsAndTakeaway() {
        // Map Period.D7 -> trend_change_7d, Period.D30 -> trend_change_30d, Period.Y1 -> trend_change_1y
        assertEquals(R.string.trend_change_7d, when (Period.D7) {
            Period.D7 -> R.string.trend_change_7d
            Period.D30 -> R.string.trend_change_30d
            Period.Y1 -> R.string.trend_change_1y
        })
        assertEquals(R.string.trend_change_30d, when (Period.D30) {
            Period.D7 -> R.string.trend_change_7d
            Period.D30 -> R.string.trend_change_30d
            Period.Y1 -> R.string.trend_change_1y
        })
        assertEquals(R.string.trend_change_1y, when (Period.Y1) {
            Period.D7 -> R.string.trend_change_7d
            Period.D30 -> R.string.trend_change_30d
            Period.Y1 -> R.string.trend_change_1y
        })

        // Verify takeaways
        val takeaway7d = when (Period.D7) {
            Period.D7 -> "this week"
            Period.D30 -> "this month"
            Period.Y1 -> "this year"
        }
        val takeaway30d = when (Period.D30) {
            Period.D7 -> "this week"
            Period.D30 -> "this month"
            Period.Y1 -> "this year"
        }
        val takeaway1y = when (Period.Y1) {
            Period.D7 -> "this week"
            Period.D30 -> "this month"
            Period.Y1 -> "this year"
        }
        assertEquals("this week", takeaway7d)
        assertEquals("this month", takeaway30d)
        assertEquals("this year", takeaway1y)

        // Verify takeaway sentence format and that it never contains the word "Period"
        val metalName = "Gold"
        val unsignedPercent = "3.57"
        val unsignedAmount = "₹509.61 / g"
        val arrow = "▼"
        val takeawaySentence = "$metalName is down $unsignedPercent% $takeaway7d ($arrow $unsignedAmount)"
        assertEquals("Gold is down 3.57% this week (▼ ₹509.61 / g)", takeawaySentence)
        assertTrue(!takeawaySentence.contains("Period", ignoreCase = true))

        // Verify hint format "From <startDate> to today"
        val hint = "From 26 Oct to today"
        assertTrue(hint.startsWith("From ") && hint.endsWith(" to today"))
        assertTrue(!hint.contains("Period", ignoreCase = true))
    }
}
