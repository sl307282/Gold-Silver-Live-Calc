package com.goldsilver.livecalc.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldsilver.livecalc.ui.theme.*

const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.goldsilver.livecalc"
const val PLAY_STORE_MARKET_URI = "market://details?id=com.goldsilver.livecalc"

/**
 * Safely redirects the user to the app's Google Play Store update page.
 * Highly compatible across Android 7.0 through Android 16 (API 24–36).
 *
 * Tries in order:
 *  1. Native Google Play Store app (com.android.vending)
 *  2. Generic market:// intent handler
 *  3. Default Web Browser fallback
 *  4. Graceful user-facing notification if no handler exists
 */
fun openPlayStoreUpdatePage(context: Context) {
    val packageName = context.packageName.ifBlank { "com.goldsilver.livecalc" }
    val marketUri = Uri.parse("market://details?id=$packageName")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

    // Attempt 1: Direct Play Store App target
    try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.startActivity(playStoreIntent)
        return
    } catch (_: Exception) {
        // Play store app not installed or restricted, proceed to generic handler
    }

    // Attempt 2: Generic market:// URI
    try {
        val genericMarketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.startActivity(genericMarketIntent)
        return
    } catch (_: Exception) {
        // No market handler, proceed to web browser
    }

    // Attempt 3: Web Browser fallback
    try {
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {
        // Attempt 4: Graceful error handling
        Toast.makeText(
            context,
            "Please open Google Play Store to update Gold & Silver Live Calc.",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * AppUpdateDialog — Professional, minimal, backward-compatible App Update Dialog
 * designed for Android 7–16 (API 24–36) and all screen sizes.
 *
 * Components & Behavior:
 *  - Title: Update Available
 *  - Message: A new version is ready with the latest rates and improvements.
 *  - UPDATE NOW: Launches Google Play Store page
 *  - LATER: Closes dialog and continues app usage smoothly
 */
@Composable
fun AppUpdateDialog(
    versionName: String = "",
    updateMessage: String = "",
    onDismiss: () -> Unit,
    onUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = com.goldsilver.livecalc.util.LocalAppStrings.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = GoldPrimary.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemDarkThemeGlobal) DarkSurface else Color(0xFF1E202E)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Update Icon Badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(GoldPrimary.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = strings.updateAvailable,
                            tint = GoldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Title
                    Text(
                        text = strings.updateAvailable,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    // Message
                    Text(
                        text = updateMessage.ifBlank {
                            strings.newVersionReady
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0B3C7),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons: UPDATE NOW (Primary) & LATER (Secondary)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // UPDATE NOW Button
                        Button(
                            onClick = {
                                onDismiss()
                                openPlayStoreUpdatePage(context)
                                onUpdate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF0D0E15)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color(0xFF0D0E15),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.updateNow.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0D0E15),
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // LATER Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.18f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFB0B3C7)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = strings.later.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFB0B3C7),
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
