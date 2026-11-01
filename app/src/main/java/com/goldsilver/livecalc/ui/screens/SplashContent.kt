package com.goldsilver.livecalc.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goldsilver.livecalc.BuildConfig
import com.goldsilver.livecalc.R
import com.goldsilver.livecalc.ui.theme.GoldPrimary
import com.goldsilver.livecalc.ui.theme.TextPrimary
import com.goldsilver.livecalc.ui.theme.TextSecondary

/**
 * Returns true if system settings specify reduced motion (ANIMATOR_DURATION_SCALE == 0).
 */
fun isReducedMotion(context: Context): Boolean {
    return try {
        val durationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        durationScale == 0f
    } catch (_: Exception) {
        false
    }
}

/**
 * Branded Compose splash continuation screen.
 *
 * Appears as a short continuation after the system splash screen.
 * Displays centered icon (fade+scale 300 ms), app title "Gold Silver Live Calc",
 * tagline "Calculate with Confidence", thin indeterminate progress indicator,
 * "Fetching latest rates…", and the app version.
 */
@Composable
fun SplashContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reducedMotion = remember { isReducedMotion(context) }
    var startAnimation by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            startAnimation = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 300,
            easing = FastOutSlowInEasing
        ),
        label = "splash_fade"
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 300,
            easing = FastOutSlowInEasing
        ),
        label = "splash_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Centered launcher icon with fade+scale animation
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // App title in titleLarge
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline in bodyMedium
            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Thin indeterminate LinearProgressIndicator (primary color, 40% alpha track)
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = GoldPrimary,
                trackColor = GoldPrimary.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status indicator in labelSmall 60% alpha
            Text(
                text = stringResource(id = R.string.splash_fetching_rates),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary.copy(alpha = 0.6f)
            )
        }

        // Version label at the bottom (labelSmall 40% alpha)
        Text(
            text = stringResource(id = R.string.splash_version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}
