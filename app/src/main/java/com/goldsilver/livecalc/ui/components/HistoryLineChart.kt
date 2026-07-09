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
import androidx.compose.ui.Alignment
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
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier.background(DarkSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No historical data available", color = TextSecondary)
        }
        return
    }

    val prices = points.map { it.second }
    val maxPrice = prices.maxOrNull() ?: 0.0
    val minPrice = prices.minOrNull() ?: 0.0
    val maxIndex = prices.indexOf(maxPrice)
    val minIndex = prices.indexOf(minPrice)

    val priceRange = maxPrice - minPrice
    val padding = if (priceRange == 0.0) 1.0 else priceRange * 0.15
    val graphMin = minPrice - padding
    val graphMax = maxPrice + padding

    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
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
        // High/Low header indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("HIGH", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = String.format("%.2f %s", maxPrice, currency),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LOW", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = String.format("%.2f %s", minPrice, currency),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val numPoints = points.size

            if (numPoints < 2) return@Canvas

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

            // Map points to screen coordinates
            val screenPoints = points.mapIndexed { index, pair ->
                val x = index * (width / (numPoints - 1))
                val rawY = (chartHeight - ((pair.second - graphMin) / (graphMax - graphMin) * chartHeight)).toFloat()
                // Animate Y scale up
                val animatedY = chartHeight - ((chartHeight - rawY) * animationProgress.value)
                Offset(x, animatedY)
            }

            // Draw fill area under the line
            val fillPath = Path().apply {
                moveTo(screenPoints.first().x, chartHeight)
                for (i in 0 until numPoints) {
                    lineTo(screenPoints[i].x, screenPoints[i].y)
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
                    quadraticTo(controlX, pPrev.y, controlX, pCurr.y)
                    lineTo(pCurr.x, pCurr.y)
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
                numPoints <= 7 -> 3
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
            if (animationProgress.value == 1f) {
                // High Point Dot
                val highOffset = screenPoints[maxIndex]
                drawCircle(
                    color = AccentGreen,
                    radius = 12f,
                    center = highOffset
                )
                drawCircle(
                    color = TextPrimary,
                    radius = 6f,
                    center = highOffset
                )

                // High Point Label
                val highText = "HIGH"
                val highTextResult = textMeasurer.measure(
                    text = AnnotatedString(highText),
                    style = TextStyle(color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = highTextResult,
                    topLeft = Offset(
                        x = (highOffset.x - highTextResult.size.width / 2f).coerceIn(0f, width - highTextResult.size.width),
                        y = (highOffset.y - 45f).coerceIn(0f, chartHeight)
                    )
                )

                // Low Point Dot
                val lowOffset = screenPoints[minIndex]
                drawCircle(
                    color = AccentRed,
                    radius = 12f,
                    center = lowOffset
                )
                drawCircle(
                    color = TextPrimary,
                    radius = 6f,
                    center = lowOffset
                )

                // Low Point Label
                val lowText = "LOW"
                val lowTextResult = textMeasurer.measure(
                    text = AnnotatedString(lowText),
                    style = TextStyle(color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = lowTextResult,
                    topLeft = Offset(
                        x = (lowOffset.x - lowTextResult.size.width / 2f).coerceIn(0f, width - lowTextResult.size.width),
                        y = (lowOffset.y + 15f).coerceIn(0f, chartHeight - lowTextResult.size.height)
                    )
                )
            }
        }
    }
}
