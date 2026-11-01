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

@OptIn(ExperimentalTextApi::class)
@Composable
fun HistoryLineChart(
    points: List<Pair<Long, Double>>, // timestamp, price
    isGold: Boolean,
    currency: String,
    modifier: Modifier = Modifier,
    livePrice: Double? = null,   // today's live rate override for the header
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

    val prices = points.map { it.second }
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
                                if (numPoints > 1 && width > 0f) {
                                    // Reserve margin for Y-axis amount labels (approx 60dp)
                                    val approxYMargin = 60.dp.toPx()
                                    val usableChartWidth = (width - approxYMargin).coerceAtLeast(40f)
                                    val spacing = usableChartWidth / (numPoints - 1)
                                    selectedIndex = (position.x / spacing).roundToInt().coerceIn(0, numPoints - 1)
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
            val yAxisWidth = (maxLabelWidth + 12.dp.toPx()).coerceAtLeast(48.dp.toPx())
            val chartWidth = (width - yAxisWidth).coerceAtLeast(50f)

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
                start = Offset(0f, yGridMax),
                end = Offset(chartWidth, yGridMax),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )
            drawText(
                textLayoutResult = maxTextResult,
                topLeft = Offset(chartWidth + 6.dp.toPx(), (yGridMax - maxTextResult.size.height / 2f).coerceIn(0f, chartHeight - maxTextResult.size.height))
            )

            // Gridline 2: Mid
            drawLine(
                color = gridColor,
                start = Offset(0f, yGridMid),
                end = Offset(chartWidth, yGridMid),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )
            drawText(
                textLayoutResult = midTextResult,
                topLeft = Offset(chartWidth + 6.dp.toPx(), (yGridMid - midTextResult.size.height / 2f).coerceIn(0f, chartHeight - midTextResult.size.height))
            )

            // Gridline 3: Min
            drawLine(
                color = gridColor,
                start = Offset(0f, yGridMin),
                end = Offset(chartWidth, yGridMin),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )
            drawText(
                textLayoutResult = minTextResult,
                topLeft = Offset(chartWidth + 6.dp.toPx(), (yGridMin - minTextResult.size.height / 2f).coerceIn(0f, chartHeight - minTextResult.size.height))
            )

            if (numPoints == 1) {
                val point = points.first()
                val centerX = chartWidth / 2f
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

                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val dateText = dateFormat.format(Date(point.first))
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

            // Map points to screen coordinates
            val screenPoints = points.mapIndexed { index, pair ->
                val x = index * (chartWidth / (numPoints - 1))
                val rawY = if (graphMax > graphMin) {
                    (chartHeight - ((pair.second - graphMin) / (graphMax - graphMin) * chartHeight)).toFloat()
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

            // Draw date labels on X-axis: all 7 for 7D, at least 4 evenly spaced for 30D and 1Y
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
                    val x = index * (chartWidth / (numPoints - 1))
                    val dateText = dateFormat.format(Date(points[index].first))
                    val dateTextLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(dateText),
                        style = TextStyle(
                            color = TextMuted,
                            fontSize = if (numPoints <= 7) 9.sp else 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    val labelW = dateTextLayoutResult.size.width
                    val labelX = (x - labelW / 2f).coerceIn(0f, chartWidth - labelW)
                    val labelY = chartHeight + (labelHeight - dateTextLayoutResult.size.height) / 2f
                    drawText(
                        textLayoutResult = dateTextLayoutResult,
                        topLeft = Offset(labelX, labelY)
                    )
                }
            }

            // Highlight High & Low points with offset capsule badges so they never overlap the line
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
                val highBadgeX = (highOffset.x - highBadgeW / 2f).coerceIn(0f, chartWidth - highBadgeW)
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
                val lowBadgeX = (lowOffset.x - lowBadgeW / 2f).coerceIn(0f, chartWidth - lowBadgeW)
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

                    // Horizontal crosshair
                    drawLine(
                        color = TextMuted.copy(alpha = 0.35f),
                        start = Offset(0f, selectedOffset.y),
                        end = Offset(chartWidth, selectedOffset.y),
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
                    val diff = prevPoint?.let { selectedPoint.second - it.second }
                    val diffPercent = if (prevPoint != null && prevPoint.second > 0) {
                        ((selectedPoint.second - prevPoint.second) / prevPoint.second) * 100.0
                    } else null

                    val tooltipDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedPoint.first))
                    val tooltipRate = PriceConverter.format(selectedPoint.second, currency, perGram = true)

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
                            changeString = "+$formattedDiff (▲ +$formattedPercent%)"
                            changeColor = AccentGreen
                        } else if (diff < -0.001) {
                            changeString = "-$formattedDiff (▼ $formattedPercent%)"
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

                    // Calculate Tooltip Position
                    var boxX = selectedOffset.x - boxWidth / 2f
                    boxX = boxX.coerceIn(0f, chartWidth - boxWidth)

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
