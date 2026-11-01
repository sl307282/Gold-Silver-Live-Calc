package com.goldsilver.livecalc.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.goldsilver.livecalc.R
import com.goldsilver.livecalc.ui.theme.*
import kotlin.math.roundToInt

/**
 * Price range fraction calculator.
 * fraction = (current − low) / (high − low), clamped to 0..1; if high == low, fraction = 0.5.
 */
fun calculatePriceRangeFraction(current: Double, low: Double, high: Double): Float {
    if (high <= low) return 0.5f
    val f = (current - low) / (high - low)
    return f.coerceIn(0.0, 1.0).toFloat()
}

/**
 * Status categorization based on fraction thresholds:
 * - < 0.05: AT_LOW
 * - > 0.95: AT_HIGH
 * - < 0.33: NEAR_LOW
 * - 0.33..0.66: MID_RANGE
 * - > 0.66: NEAR_HIGH
 */
enum class PriceRangeStatus {
    AT_LOW,
    NEAR_LOW,
    MID_RANGE,
    NEAR_HIGH,
    AT_HIGH
}

fun getPriceRangeStatus(fraction: Float): PriceRangeStatus {
    return when {
        fraction < 0.05f -> PriceRangeStatus.AT_LOW
        fraction > 0.95f -> PriceRangeStatus.AT_HIGH
        fraction < 0.33f -> PriceRangeStatus.NEAR_LOW
        fraction <= 0.66f -> PriceRangeStatus.MID_RANGE
        else -> PriceRangeStatus.NEAR_HIGH
    }
}

fun getPriceRangeCaption(
    status: PriceRangeStatus,
    periodLabel: String,
    atLowTemplate: String = "At its %s low",
    atHighTemplate: String = "At its %s high",
    nearLowText: String = "Nearer the low",
    midText: String = "Mid-range",
    nearHighText: String = "Nearer the high"
): String {
    return when (status) {
        PriceRangeStatus.AT_LOW -> String.format(atLowTemplate, periodLabel)
        PriceRangeStatus.NEAR_LOW -> nearLowText
        PriceRangeStatus.MID_RANGE -> midText
        PriceRangeStatus.NEAR_HIGH -> nearHighText
        PriceRangeStatus.AT_HIGH -> String.format(atHighTemplate, periodLabel)
    }
}

/**
 * Reusable Price Range indicator bar for Historical Trends.
 *
 * @param low Lowest rate in the selected period.
 * @param high Highest rate in the selected period.
 * @param current Current live spot rate.
 * @param periodLabel Period word following the tab (e.g. "7-day", "30-day", "1-year").
 * @param formatter Formatter converting raw price into currency string without "/ g" (e.g. "₹13,777.65").
 */
@Composable
fun PriceRangeBar(
    low: Double,
    high: Double,
    current: Double,
    periodLabel: String,
    formatter: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val targetFraction = calculatePriceRangeFraction(current, low, high)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "priceRangeFraction"
    )

    val percentInt = (targetFraction * 100f).roundToInt()
    val formattedLow = formatter(low)
    val formattedHigh = formatter(high)
    val formattedCurrent = formatter(current)

    val contentDesc = stringResource(
        R.string.range_content_desc,
        percentInt,
        periodLabel,
        formattedLow,
        formattedHigh
    )

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val todayText = stringResource(R.string.range_today_label, formattedCurrent)
    val todayStyle = MaterialTheme.typography.labelSmall
    val measuredText = remember(todayText, todayStyle) {
        textMeasurer.measure(
            text = AnnotatedString(todayText),
            style = todayStyle
        )
    }
    val textWidth = measuredText.size.width.toFloat()

    val status = getPriceRangeStatus(targetFraction)
    val (captionText, captionColor) = when (status) {
        PriceRangeStatus.AT_LOW -> stringResource(R.string.range_at_low, periodLabel) to AccentRed
        PriceRangeStatus.AT_HIGH -> stringResource(R.string.range_at_high, periodLabel) to AccentGreen
        PriceRangeStatus.NEAR_LOW -> stringResource(R.string.range_near_low) to TextMuted
        PriceRangeStatus.MID_RANGE -> stringResource(R.string.range_mid) to TextMuted
        PriceRangeStatus.NEAR_HIGH -> stringResource(R.string.range_near_high) to TextMuted
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDesc
            }
    ) {
        // Row 1: Low ... High (labelSmall, 70% alpha, space-between)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.range_low_label, formattedLow),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceColor.copy(alpha = 0.7f)
            )
            Text(
                text = stringResource(R.string.range_high_label, formattedHigh),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceColor.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2 & 3: Horizontal track (height 6 dp), gold dot (14 dp), and clamped today rate label
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val dotRadiusPx = with(density) { 7.dp.toPx() }
            val trackHeightPx = with(density) { 6.dp.toPx() }
            val trackCenterY = with(density) { 10.dp.toPx() }
            val trackTop = trackCenterY - trackHeightPx / 2f

            val usableWidth = (maxWidthPx - 2 * dotRadiusPx).coerceAtLeast(0f)
            val dotXPx = dotRadiusPx + animatedFraction * usableWidth

            // Canvas for track and dot
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background track: height 6 dp, fully rounded, background = onSurface at 12% alpha
                drawRoundRect(
                    color = onSurfaceColor.copy(alpha = 0.12f),
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width, trackHeightPx),
                    cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                )

                // Filled portion from 0 to the current-rate position, gradient from red to green via amber
                val fillWidth = dotXPx.coerceIn(0f, size.width)
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF5252), // Red (left/low)
                                Color(0xFFFFB300), // Amber (middle)
                                Color(0xFF4ADE80)  // Green (right/high)
                            ),
                            startX = 0f,
                            endX = size.width
                        ),
                        topLeft = Offset(0f, trackTop),
                        size = Size(fillWidth, trackHeightPx),
                        cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                    )
                }

                // Soft shadow under the dot
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = dotRadiusPx + with(density) { 1.5.dp.toPx() },
                    center = Offset(dotXPx, trackCenterY + with(density) { 1.dp.toPx() })
                )

                // 14 dp gold dot (primary color)
                drawCircle(
                    color = GoldPrimary,
                    radius = dotRadiusPx,
                    center = Offset(dotXPx, trackCenterY)
                )

                // 2 dp dark border
                drawCircle(
                    color = DarkBackground,
                    radius = dotRadiusPx,
                    center = Offset(dotXPx, trackCenterY),
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )
            }

            // Row 3 (labelSmall, centered under the dot, clamped so it never clips at either edge)
            val rawTextX = dotXPx - (textWidth / 2f)
            val clampedTextX = rawTextX.coerceIn(0f, (maxWidthPx - textWidth).coerceAtLeast(0f))

            Text(
                text = todayText,
                style = todayStyle,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = clampedTextX
                        translationY = with(density) { 22.dp.toPx() }
                    }
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // State caption: "At its 7-day low", "Nearer the low", "Mid-range", "Nearer the high", "At its 7-day high"
        Text(
            text = captionText,
            style = MaterialTheme.typography.labelSmall,
            color = captionColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
