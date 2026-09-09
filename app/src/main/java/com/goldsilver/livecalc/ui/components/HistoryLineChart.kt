package com.goldsilver.livecalc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTextApi::class)
@Composable
fun HistoryLineChart(
    points: List<Pair<Long, Double>>, // timestamp, price
    isGold: Boolean,
    currency: String,
    modifier: Modifier = Modifier,
    livePrice: Double? = null   // today's live rate override for the header
) {
    val strings = com.goldsilver.livecalc.util.LocalAppStrings.current

    if (points.isEmpty()) {
        Box(
            modifier = modifier.background(DarkSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(strings.noHistoricalData, color = TextSecondary)
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
    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
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
        // High/Low/Current header indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("LOW", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(minPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT RATE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(displayPrice)} $currency",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = lineColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("HIGH", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(maxPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(points) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                val position = event.changes.first().position
                                val width = size.width
                                val numPoints = points.size
                                if (numPoints > 1) {
                                    val spacing = width / (numPoints - 1)
                                    selectedIndex = (position.x / spacing).roundToInt().coerceIn(0, numPoints - 1)
                                } else if (numPoints == 1) {
                                    selectedIndex = 0
                                }
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val numPoints = points.size

            val labelHeight = 24.dp.toPx()
            val chartHeight = height - labelHeight

            // Draw horizontal grid lines (3 lines)
            val gridColor = if (isSystemDarkThemeGlobal) Color(0xFF2C2C2C) else Color(0xFFE5E5EA)
            val gridStroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            for (i in 0..2) {
                val yGrid = chartHeight * i / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, yGrid),
                    end = Offset(width, yGrid),
                    strokeWidth = 1f,
                    pathEffect = gridStroke.pathEffect
                )
            }

            if (numPoints == 1) {
                // Single point rendering
                val point = points.first()
                val centerX = width / 2f
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
                val x = index * (width / (numPoints - 1))
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
                        gradientColor.copy(alpha = 0.3f),
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
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw date labels on X axis
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val numLabels = when {
                numPoints <= 7 -> minOf(numPoints, 3)
                numPoints <= 30 -> 4
                else -> 5
            }
            val labelIndices = if (numPoints >= 2) {
                (0 until numLabels).map { i ->
                    (i * (numPoints - 1) / (numLabels - 1))
                }.distinct()
            } else {
                listOf(0)
            }

            labelIndices.forEach { index ->
                if (index in 0 until numPoints) {
                    val x = index * (width / (numPoints - 1))
                    val dateText = dateFormat.format(Date(points[index].first))
                    val dateTextLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(dateText),
                        style = TextStyle(
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    val labelWidth = dateTextLayoutResult.size.width
                    val labelX = (x - labelWidth / 2f).coerceIn(0f, width - labelWidth)
                    val labelY = chartHeight + (labelHeight - dateTextLayoutResult.size.height) / 2f
                    drawText(
                        textLayoutResult = dateTextLayoutResult,
                        topLeft = Offset(labelX, labelY)
                    )
                }
            }

            // Highlight High & Low points
            if (animationProgress.value == 1f && maxIndex != minIndex) {
                // High Point Dot
                val highOffset = screenPoints[maxIndex]
                drawCircle(
                    color = AccentGreen,
                    radius = 10f,
                    center = highOffset
                )
                drawCircle(
                    color = TextPrimary,
                    radius = 5f,
                    center = highOffset
                )

                // High Point Label
                val highText = strings.high
                val highTextResult = textMeasurer.measure(
                    text = AnnotatedString(highText),
                    style = TextStyle(color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = highTextResult,
                    topLeft = Offset(
                        x = (highOffset.x - highTextResult.size.width / 2f).coerceIn(0f, width - highTextResult.size.width),
                        y = (highOffset.y - 40f).coerceIn(0f, chartHeight)
                    )
                )

                // Low Point Dot
                val lowOffset = screenPoints[minIndex]
                drawCircle(
                    color = AccentRed,
                    radius = 10f,
                    center = lowOffset
                )
                drawCircle(
                    color = TextPrimary,
                    radius = 5f,
                    center = lowOffset
                )

                // Low Point Label
                val lowText = strings.low
                val lowTextResult = textMeasurer.measure(
                    text = AnnotatedString(lowText),
                    style = TextStyle(color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = lowTextResult,
                    topLeft = Offset(
                        x = (lowOffset.x - lowTextResult.size.width / 2f).coerceIn(0f, width - lowTextResult.size.width),
                        y = (lowOffset.y + 12f).coerceIn(0f, chartHeight - lowTextResult.size.height)
                    )
                )
            }

            // Selection indicator & Enhanced Tooltip
            selectedIndex?.let { index ->
                if (index in screenPoints.indices) {
                    val selectedOffset = screenPoints[index]
                    val selectedPoint = points[index]

                    // Vertical guide line
                    drawLine(
                        color = TextMuted.copy(alpha = 0.5f),
                        start = Offset(selectedOffset.x, 0f),
                        end = Offset(selectedOffset.x, chartHeight),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Selection dot
                    drawCircle(
                        color = lineColor,
                        radius = 14f,
                        center = selectedOffset
                    )
                    drawCircle(
                        color = DarkBackground,
                        radius = 7f,
                        center = selectedOffset
                    )

                    // Compute difference and percentage vs immediately previous available point
                    val prevPoint = if (index > 0) points[index - 1] else null
                    val diff = prevPoint?.let { selectedPoint.second - it.second }
                    val diffPercent = if (prevPoint != null && prevPoint.second > 0) {
                        ((selectedPoint.second - prevPoint.second) / prevPoint.second) * 100.0
                    } else null

                    val tooltipDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedPoint.first))
                    val tooltipRate = "${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(selectedPoint.second)} $currency${strings.perGram}"
                    
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
                        val formattedDiff = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(Math.abs(diff))
                        val formattedPercent = String.format(Locale.US, "%.2f", Math.abs(diffPercent))
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

                    val tooltipPadding = 20f
                    val boxWidth = maxOf(dateResult.size.width, rateResult.size.width, changeResult.size.width) + tooltipPadding * 2
                    val boxHeight = dateResult.size.height + rateResult.size.height + changeResult.size.height + tooltipPadding * 2

                    // Calculate Box Position (Prevent clipping)
                    var boxX = selectedOffset.x - boxWidth / 2f
                    boxX = boxX.coerceIn(0f, width - boxWidth)
                    
                    var boxY = selectedOffset.y - boxHeight - 24f
                    if (boxY < 0f) {
                        boxY = selectedOffset.y + 24f // Display below if cuts off at top
                    }

                    // Draw Tooltip Box
                    drawRoundRect(
                        color = DarkSurface,
                        topLeft = Offset(boxX, boxY),
                        size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = lineColor.copy(alpha = 0.5f),
                        topLeft = Offset(boxX, boxY),
                        size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
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
    }
}
