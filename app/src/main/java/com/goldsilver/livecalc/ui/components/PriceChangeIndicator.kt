package com.goldsilver.livecalc.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.AccentGreen
import com.goldsilver.livecalc.ui.theme.AccentRed
import com.goldsilver.livecalc.ui.theme.TextMuted
import java.util.Locale

@Composable
fun PriceChangeIndicator(
    changePercent: Double?,
    modifier: Modifier = Modifier
) {
    if (changePercent == null || changePercent.isNaN() || changePercent.isInfinite()) {
        Text(
            text = "—",
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
        return
    }

    // Format raw percentage to 2 decimals
    val formattedPercent = String.format(Locale.US, "%.2f", Math.abs(changePercent))
    val isZero = formattedPercent == "0.00"

    if (isZero) {
        Text(
            text = "0.00%",
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
    } else if (changePercent > 0) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▲",
                color = AccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "+$formattedPercent%",
                color = AccentGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▼",
                color = AccentRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$formattedPercent%",
                color = AccentRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

