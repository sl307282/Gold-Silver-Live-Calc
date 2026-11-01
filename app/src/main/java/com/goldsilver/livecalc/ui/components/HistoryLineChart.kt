package com.goldsilver.livecalc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.util.LocalAppStrings
import com.goldsilver.livecalc.util.PriceConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HistoryChartPoint(
    val timestamp: Long,
    val price: Double,
    val dateKey: String = "",
    val isToday: Boolean = false
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun HistoryLineChart(
    points: List<HistoryChartPoint>,
    isGold: Boolean,
    currency: String,
    modifier: Modifier = Modifier,
    livePrice: Double? = null,
    isConvertedAtTodayRate: Boolean = false,
    selectedRange: String = "7D"
) {
    val strings = LocalAppStrings.current

    if (points.isEmpty()) {
        Box(
            modifier = modifier.background(DarkSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(strings.noHistoricalData, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val prices = points.map { it.price }
    val displayPrice = livePrice ?: prices.lastOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val minPrice = prices.minOrNull() ?: 0.0
    val maxIndex = prices.indexOf(maxPrice)
    val minIndex = prices.indexOf(minPrice)

    val priceRange = maxPrice - minPrice
    val padding = if (priceRange == 0.0) maxPrice * 0.05 else priceRange * 0.15
    val graphMin = (minPrice - padding).coerceAtLeast(0.0)
    val graphMax = maxPrice + padding

    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points, selectedRange) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650)
        )
    }

    val lineColor = if (isGold) GoldPrimary else SilverPrimary
    val gradientColor = if (isGold) GoldLight else SilverLight

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // High/Low/Current header indicator formatted via PriceConverter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(strings.low.uppercase(Locale.US), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = PriceConverter.format(minPrice, currency, perGram = true),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(strings.currentRate.uppercase(Locale.US), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = PriceConverter.format(displayPrice, currency, perGram = true),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = lineColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(strings.high.uppercase(Locale.US), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = PriceConverter.format(maxPrice, currency, perGram = true),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        var selectedIndex by remember(points, selectedRange) { mutableStateOf<Int?>(null) }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(points, selectedRange) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val position = change.position
                                val width = size.width
                                val numPoints = points.size
                                val plotLeft = 8.dp.toPx()
                                val approxRightPadding = 72.dp.toPx()
                                val plotWidth = (width - plotLeft - approxRightPadding).coerceAtLeast(40f)
                                if (numPoints > 1 && plotWidth > 0f) {
                                    val touchX = (position.x - plotLeft).coerceIn(0f, plotWidth)
                                    val spacing = plotWidth / (numPoints - 1)
                                    selectedIndex = (touchX / spacing).roundToInt().coerceIn(0, numPoints - 1)
                                } else if (numPoints == 1) {
                                    selectedIndex = 0
                                }
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val numPoints = points.size

            val labelHeight = 24.dp.toPx()
            val chartHeight = (height - labelHeight).coerceAtLeast(10f)

            // Dynamic Y-axis amount labels
            val midPrice = (maxPrice + minPrice) / 2.0
            val yAxisStyle = TextStyle(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)

            val maxTextResult = textMeasurer.measure(
                text = AnnotatedString(PriceConverter.formatAmount(maxPrice, currency)),
                style = yAxisStyle
            )
            val midTextResult = textMeasurer.measure(
                text = AnnotatedString(PriceConverter.formatAmount(midPrice, currency)),
                style = yAxisStyle
            )
            val minTextResult = textMeasurer.measure(
                text = AnnotatedString(PriceConverter.formatAmount(minPrice, currency)),
                style = yAxisStyle
            )

            val maxLabelWidth = maxOf(maxTextResult.size.width, midTextResult.size.width, minTextResult.size.width)
            val yAxisWidth = maxLabelWidth + 6.dp.toPx()

            // Inset plot so line and HIGH/LOW pills never overlap right-axis labels
            val plotLeft = 10.dp.toPx()
            val plotGap = 16.dp.toPx() // generous buffer between plot line/pills and y-axis text
            val plotRight = (width - yAxisWidth - plotGap).coerceAtLeast(plotLeft + 40f)
            val plotWidth = plotRight - plotLeft

            val yAxisLabelX = (plotRight + plotGap).coerceAtMost(width - maxLabelWidth)

            // Draw 3 horizontal gridlines with amount labels in selected currency
            val gridColor = if (isSystemDarkThemeGlobal) Color(0xFF2C2C2C) else Color(0xFFE5E5EA)
            val gridStroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))

            val yGridMax = if (graphMax > graphMin) {
                (chartHeight - ((maxPrice - graphMin) / (graphMax - graphMin) * chartHeight)).toFloat()
            } else 0f
            val yGridMin = if (graphMax > graphMin) {
                (chartHeight - ((minPrice - graphMin) / (graphMax - graphMin) * chartHeight)).toFloat()
            } else chartHeight
            val yGridMid = (yGridMax + yGridMin) / 2f

            // Gridline 1: Max
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, yGridMax),
                end = Offset(plotRight + 8.dp.toPx(), yGridMax),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )
            drawText(
                textLayoutResult = maxTextResult,
                topLeft = Offset(yAxisLabelX, (yGridMax - maxTextResult.size.height / 2f).coerceIn(0f, chartHeight - maxTextResult.size.height))
            )

            // Gridline 2: Mid
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, yGridMid),
                end = Offset(plotRight + 8.dp.toPx(), yGridMid),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )
            drawText(
                textLayoutResult = midTextResult,
                topLeft = Offset(yAxisLabelX, (yGridMid - midTextResult.size.height / 2f).coerceIn(0f, chartHeight - midTextResult.size.height))
            )

            // Gridline 3: Min
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, yGridMin),
                end = Offset(plotRight + 8.dp.toPx(), yGridMin),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )
            drawText(
                textLayoutResult = minTextResult,
                topLeft = Offset(yAxisLabelX, (yGridMin - minTextResult.size.height / 2f).coerceIn(0f, chartHeight - minTextResult.size.height))
            )

            if (numPoints == 1) {
                val point = points.first()
                val centerX = plotLeft + plotWidth / 2f
                val centerY = chartHeight / 2f

                drawCircle(
                    color = lineColor,
                    radius = 12f * animationProgress.value,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = TextPrimary,
                    radius = 6f * animationProgress.value,
                    center = Offset(centerX, centerY)
                )

                val dateText = if (point.isToday) strings.today else SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(point.timestamp))
                val dateTextResult = textMeasurer.measure(
                    text = AnnotatedString(dateText),
                    style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
                drawText(
                    textLayoutResult = dateTextResult,
                    topLeft = Offset(centerX - dateTextResult.size.width / 2f, chartHeight + 4.dp.toPx())
                )
                return@Canvas
            }

            // Map points to screen coordinates within [plotLeft, plotRight]
            val screenPoints = points.mapIndexed { index, point ->
                val x = plotLeft + index * (plotWidth / (numPoints - 1))
                val rawY = if (graphMax > graphMin) {
                    (chartHeight - ((point.price - graphMin) / (graphMax - graphMin) * chartHeight)).toFloat()
                } else {
                    chartHeight / 2f
                }
                val animatedY = chartHeight - ((chartHeight - rawY) * animationProgress.value)
                Offset(x, animatedY)
            }

            // Draw fill area under the line
            val fillPath = Path().apply {
                moveTo(screenPoints.first().x, chartHeight)
                lineTo(screenPoints.first().x, screenPoints.first().y)
                for (i in 1 until numPoints) {
                    val pPrev = screenPoints[i - 1]
                    val pCurr = screenPoints[i]
                    val controlX = (pPrev.x + pCurr.x) / 2f
                    cubicTo(controlX, pPrev.y, controlX, pCurr.y, pCurr.x, pCurr.y)
                }
                lineTo(screenPoints.last().x, chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        gradientColor.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Draw connecting price line path
            val linePath = Path().apply {
                moveTo(screenPoints.first().x, screenPoints.first().y)
                for (i in 1 until numPoints) {
                    val pPrev = screenPoints[i - 1]
                    val pCurr = screenPoints[i]
                    val controlX = (pPrev.x + pCurr.x) / 2f
                    cubicTo(controlX, pPrev.y, controlX, pCurr.y, pCurr.x, pCurr.y)
                }
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw date labels on X-axis: deduplicated, no repeats, final point labelled "Today" if current rate
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val labelIndices = if (numPoints <= 7) {
                // All points for 7D
                points.indices.toList()
            } else {
                // At least 4 evenly spaced
                val count = 4
                (0 until count).map { i -> (i * (numPoints - 1) / (count - 1)) }.distinct()
            }

            labelIndices.forEach { index ->
                if (index in 0 until numPoints) {
                    val x = screenPoints[index].x
                    val point = points[index]
                    val dateText = if (point.isToday) {
                        strings.today
                    } else {
                        dateFormat.format(Date(point.timestamp))
                    }
                    val dateTextLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(dateText),
                        style = TextStyle(
                            color = if (point.isToday) lineColor else TextMuted,
                            fontSize = if (numPoints <= 7) 9.sp else 10.sp,
                            fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                    val labelW = dateTextLayoutResult.size.width
                    val labelX = (x - labelW / 2f).coerceIn(0f, plotRight - labelW / 2f)
                    val labelY = chartHeight + (labelHeight - dateTextLayoutResult.size.height) / 2f
                    drawText(
                        textLayoutResult = dateTextLayoutResult,
                        topLeft = Offset(labelX, labelY)
                    )
                }
            }

            // Highlight High & Low points with offset capsule badges strictly inset to never overlap labels
            if (animationProgress.value == 1f && maxIndex != minIndex) {
                // High Point
                val highOffset = screenPoints[maxIndex]
                drawCircle(color = AccentGreen, radius = 9f, center = highOffset)
                drawCircle(color = TextPrimary, radius = 4.5f, center = highOffset)

                val highText = strings.high
                val highTextResult = textMeasurer.measure(
                    text = AnnotatedString(highText),
                    style = TextStyle(color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                val padH = 6.dp.toPx()
                val padV = 2.dp.toPx()
                val highBadgeW = highTextResult.size.width + padH * 2f
                val highBadgeH = highTextResult.size.height + padV * 2f
                val highBadgeX = (highOffset.x - highBadgeW / 2f).coerceIn(plotLeft, plotRight - highBadgeW)
                val highBadgeY = (highOffset.y - highBadgeH - 8.dp.toPx()).let {
                    if (it < 0f) highOffset.y + 10.dp.toPx() else it
                }

                drawRoundRect(
                    color = DarkSurface,
                    topLeft = Offset(highBadgeX, highBadgeY),
                    size = Size(highBadgeW, highBadgeH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = AccentGreen,
                    topLeft = Offset(highBadgeX, highBadgeY),
                    size = Size(highBadgeW, highBadgeH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1f)
                )
                drawText(
                    textLayoutResult = highTextResult,
                    topLeft = Offset(highBadgeX + padH, highBadgeY + padV)
                )

                // Low Point
                val lowOffset = screenPoints[minIndex]
                drawCircle(color = AccentRed, radius = 9f, center = lowOffset)
                drawCircle(color = TextPrimary, radius = 4.5f, center = lowOffset)

                val lowText = strings.low
                val lowTextResult = textMeasurer.measure(
                    text = AnnotatedString(lowText),
                    style = TextStyle(color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                val lowBadgeW = lowTextResult.size.width + padH * 2f
                val lowBadgeH = lowTextResult.size.height + padV * 2f
                val lowBadgeX = (lowOffset.x - lowBadgeW / 2f).coerceIn(plotLeft, plotRight - lowBadgeW)
                val lowBadgeY = (lowOffset.y + 10.dp.toPx()).let {
                    if (it + lowBadgeH > chartHeight) lowOffset.y - lowBadgeH - 8.dp.toPx() else it
                }

                drawRoundRect(
                    color = DarkSurface,
                    topLeft = Offset(lowBadgeX, lowBadgeY),
                    size = Size(lowBadgeW, lowBadgeH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = AccentRed,
                    topLeft = Offset(lowBadgeX, lowBadgeY),
                    size = Size(lowBadgeW, lowBadgeH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1f)
                )
                drawText(
                    textLayoutResult = lowTextResult,
                    topLeft = Offset(lowBadgeX + padH, lowBadgeY + padV)
                )
            }

            // Touch crosshair & Drag-to-scrub tooltip
            selectedIndex?.let { index ->
                if (index in screenPoints.indices) {
                    val selectedOffset = screenPoints[index]
                    val selectedPoint = points[index]

                    // Vertical crosshair
                    drawLine(
                        color = TextMuted.copy(alpha = 0.6f),
                        start = Offset(selectedOffset.x, 0f),
                        end = Offset(selectedOffset.x, chartHeight),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // Horizontal crosshair within plot bounds
                    drawLine(
                        color = TextMuted.copy(alpha = 0.35f),
                        start = Offset(plotLeft, selectedOffset.y),
                        end = Offset(plotRight, selectedOffset.y),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // Selection intersection dot
                    drawCircle(
                        color = lineColor,
                        radius = 12f,
                        center = selectedOffset
                    )
                    drawCircle(
                        color = DarkBackground,
                        radius = 6f,
                        center = selectedOffset
                    )

                    // Difference vs previous day in selected period
                    val prevPoint = if (index > 0) points[index - 1] else null
                    val diff = prevPoint?.let { selectedPoint.price - it.price }
                    val diffPercent = if (prevPoint != null && prevPoint.price > 0) {
                        ((selectedPoint.price - prevPoint.price) / prevPoint.price) * 100.0
                    } else null

                    val dateBaseStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedPoint.timestamp))
                    val tooltipDate = if (selectedPoint.isToday) "${strings.today} • $dateBaseStr" else dateBaseStr
                    val tooltipRate = PriceConverter.format(selectedPoint.price, currency, perGram = true)

                    val dateResult = textMeasurer.measure(
                        text = AnnotatedString(tooltipDate),
                        style = TextStyle(color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                    val rateResult = textMeasurer.measure(
                        text = AnnotatedString(tooltipRate),
                        style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )

                    val changeString: String
                    val changeColor: Color
                    if (diff != null && diffPercent != null) {
                        val formattedDiff = PriceConverter.formatAmount(kotlin.math.abs(diff), currency)
                        val formattedPercent = String.format(Locale.US, "%.2f", kotlin.math.abs(diffPercent))
                        if (diff > 0.001) {
                            changeString = "▲ $formattedDiff ($formattedPercent%)"
                            changeColor = AccentGreen
                        } else if (diff < -0.001) {
                            changeString = "▼ $formattedDiff ($formattedPercent%)"
                            changeColor = AccentRed
                        } else {
                            changeString = "0.00 (0.00%)"
                            changeColor = TextMuted
                        }
                    } else {
                        changeString = strings.initialRecord
                        changeColor = TextMuted
                    }

                    val changeResult = textMeasurer.measure(
                        text = AnnotatedString(changeString),
                        style = TextStyle(color = changeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )

                    val tooltipPadding = 16f
                    val boxWidth = maxOf(dateResult.size.width, rateResult.size.width, changeResult.size.width) + tooltipPadding * 2f
                    val boxHeight = dateResult.size.height + rateResult.size.height + changeResult.size.height + tooltipPadding * 2f

                    // Calculate Tooltip Position strictly within plot bounds
                    var boxX = selectedOffset.x - boxWidth / 2f
                    boxX = boxX.coerceIn(plotLeft, plotRight - boxWidth)

                    var boxY = selectedOffset.y - boxHeight - 20f
                    if (boxY < 0f) {
                        boxY = selectedOffset.y + 20f
                    }

                    // Draw Tooltip Box
                    drawRoundRect(
                        color = DarkSurface,
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = lineColor.copy(alpha = 0.5f),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw Tooltip Text
                    val textX = boxX + tooltipPadding
                    val dateY = boxY + tooltipPadding * 0.5f
                    val rateY = dateY + dateResult.size.height + 2f
                    val changeY = rateY + rateResult.size.height + 2f

                    drawText(
                        textLayoutResult = dateResult,
                        topLeft = Offset(textX, dateY)
                    )
                    drawText(
                        textLayoutResult = rateResult,
                        topLeft = Offset(textX, rateY)
                    )
                    drawText(
                        textLayoutResult = changeResult,
                        topLeft = Offset(textX, changeY)
                    )
                }
            }
        }

        // Caption "Converted at today's rate" for EUR / AED / GBP
        if (isConvertedAtTodayRate) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.convertedAtTodayRate,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
