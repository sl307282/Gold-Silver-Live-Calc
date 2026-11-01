package com.goldsilver.livecalc.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldsilver.livecalc.R
import com.goldsilver.livecalc.data.model.PurityTable
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import com.goldsilver.livecalc.ui.viewmodel.HallmarkViewModel
import kotlinx.coroutines.launch

private val HallmarkPrimary = Color(0xFF10B981) // Vibrant Emerald Green

/**
 * Main Hallmark Verification Screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallmarkScreen(
    viewModel: HallmarkViewModel = viewModel(),
    onNavigateToCalculator: (isGold: Boolean, purity: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val huidInput by viewModel.huidInput.collectAsStateWithLifecycle()
    val isHuidValid by viewModel.isHuidValid.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    var showHuidSheet by rememberSaveable { mutableStateOf(false) }
    var isHowToReadExpanded by rememberSaveable { mutableStateOf(true) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isHowToReadExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "chevronRotation"
    )

    // ── Bottom Sheet: Understanding HUID ──────────────────────────────────────
    if (showHuidSheet) {
        WhatIsHuidBottomSheet(
            onDismissRequest = { showHuidSheet = false },
            onVerifyClick = {
                viewModel.verifyInBisCare(context) { message, actionLabel, onAction ->
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onAction?.invoke()
                        }
                    }
                }
            },
            isVerifyEnabled = isHuidValid
        )
    }

    // ── Main Screen ───────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = DarkSurfaceElevated,
                        contentColor = TextPrimary,
                        actionColor = HallmarkPrimary,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hallmark_verification_title),
                        color = HallmarkPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // ── 2. Intro Card ─────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = HallmarkPrimary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HallmarkPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = stringResource(R.string.hallmark_intro_title),
                                    tint = HallmarkPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.hallmark_intro_title),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.hallmark_intro_subtitle),
                                    color = HallmarkPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                        )

                        // Official BIS Platform Disclaimer Info Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSystemDarkThemeGlobal)
                                        Color.White.copy(alpha = 0.04f)
                                    else
                                        Color.Black.copy(alpha = 0.04f)
                                )
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = HallmarkPrimary.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(top = 1.dp)
                            )
                            Text(
                                text = stringResource(R.string.hallmark_disclaimer),
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ── 3 & 4. HUID Input & Action Button ─────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = huidInput,
                            onValueChange = { viewModel.onHuidChange(it) },
                            label = { Text(stringResource(R.string.huid_input_label)) },
                            isError = isError,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        viewModel.pasteHuid(clipText)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = stringResource(R.string.huid_paste_cd),
                                        tint = HallmarkPrimary
                                    )
                                }
                            },
                            supportingText = {
                                if (isError) {
                                    Text(
                                        text = stringResource(R.string.huid_error_invalid),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.huid_helper_text),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HallmarkPrimary,
                                focusedLabelColor = HallmarkPrimary,
                                cursorColor = HallmarkPrimary,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                unfocusedBorderColor = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4. Verify in BIS CARE button
                        Button(
                            onClick = {
                                viewModel.verifyInBisCare(context) { message, actionLabel, onAction ->
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = message,
                                            actionLabel = actionLabel,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onAction?.invoke()
                                        }
                                    }
                                }
                            },
                            enabled = isHuidValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HallmarkPrimary,
                                disabledContainerColor = HallmarkPrimary.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = if (isHuidValid) DarkBackground else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_verify_bis_care),
                                color = if (isHuidValid) DarkBackground else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Caption under button
                        Text(
                            text = stringResource(R.string.caption_redirect_bis_care),
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── 5. "How to read a hallmark" Card ──────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.5.dp,
                            color = HallmarkPrimary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Card Header Row (Tappable to expand/collapse)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clickable { isHowToReadExpanded = !isHowToReadExpanded }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = HallmarkPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(R.string.hallmark_how_to_read_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { isHowToReadExpanded = !isHowToReadExpanded },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isHowToReadExpanded)
                                        stringResource(R.string.collapse_cd)
                                    else
                                        stringResource(R.string.expand_cd),
                                    tint = HallmarkPrimary,
                                    modifier = Modifier.rotate(chevronRotation)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isHowToReadExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HorizontalDivider(
                                    color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                                )

                                // Three marks
                                HallmarkMarkRow(
                                    icon = Icons.Default.Verified,
                                    title = stringResource(R.string.hallmark_mark_bis_logo),
                                    description = stringResource(R.string.hallmark_mark_bis_logo_desc)
                                )

                                HallmarkMarkRow(
                                    icon = Icons.Default.Diamond,
                                    title = stringResource(R.string.hallmark_mark_purity_grade),
                                    description = stringResource(R.string.hallmark_mark_purity_grade_desc)
                                )

                                HallmarkMarkRow(
                                    icon = Icons.Default.QrCode,
                                    title = stringResource(R.string.hallmark_mark_huid),
                                    description = stringResource(R.string.hallmark_mark_huid_desc)
                                )

                                // Mandatory caption
                                Text(
                                    text = stringResource(R.string.hallmark_mandatory_caption),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HallmarkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Compact Purity Reference Table
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(
                                            if (isSystemDarkThemeGlobal)
                                                Color.White.copy(alpha = 0.02f)
                                            else
                                                Color.Black.copy(alpha = 0.02f)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.purity_reference_title),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.purity_reference_hint),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    HorizontalDivider(
                                        color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
                                    )

                                    PurityTable.ALL_ITEMS.forEachIndexed { index, item ->
                                        val metalColor = if (item.isGold) GoldPrimary else SilverPrimary
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clickable { onNavigateToCalculator(item.isGold, item.carat) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(metalColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (item.isGold) stringResource(R.string.metal_gold_label) else stringResource(R.string.metal_silver_label),
                                                        color = metalColor,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Text(
                                                    text = "${item.carat} · ${item.fineness}",
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = item.purityPercent,
                                                    color = TextSecondary,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (index < PurityTable.ALL_ITEMS.lastIndex) {
                                            HorizontalDivider(
                                                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f),
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. Text Button "What is a HUID?" ──────────────────────────────
            item {
                OutlinedButton(
                    onClick = { showHuidSheet = true },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        HallmarkPrimary.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = HallmarkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_what_is_huid),
                        color = HallmarkPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Compatibility wrapper for callers expecting HallmarkVerificationScreen.
 */
@Composable
fun HallmarkVerificationScreen(
    viewModel: GoldSilverViewModel? = null,
    hallmarkViewModel: HallmarkViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) = HallmarkScreen(
    viewModel = hallmarkViewModel,
    onNavigateToCalculator = { isGold, purity ->
        val route = if (isGold) "gold_calc?purity=$purity" else "silver_calc?purity=$purity"
        onNavigate?.invoke(route)
    },
    modifier = modifier
)

/**
 * "What is a HUID?" Modal Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatIsHuidBottomSheet(
    onDismissRequest: () -> Unit,
    onVerifyClick: () -> Unit,
    isVerifyEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = DarkSurface,
        tonalElevation = 8.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sheet_understanding_huid_title),
                    color = HallmarkPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_cd),
                        tint = HallmarkPrimary
                    )
                }
            }

            HorizontalDivider(
                color = if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
            )

            // Section 1: Definition
            Text(
                text = stringResource(R.string.sheet_section_definition),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            // Section 2: Three Marks
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSystemDarkThemeGlobal)
                            Color.White.copy(alpha = 0.04f)
                        else
                            Color.Black.copy(alpha = 0.04f)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.sheet_section_three_marks_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                // 1. BIS Logo
                HallmarkMarkRow(
                    icon = Icons.Default.Verified,
                    title = stringResource(R.string.hallmark_mark_bis_logo),
                    description = stringResource(R.string.hallmark_mark_bis_logo_desc)
                )

                // 2. Purity Grade
                HallmarkMarkRow(
                    icon = Icons.Default.Diamond,
                    title = stringResource(R.string.hallmark_mark_purity_grade),
                    description = stringResource(R.string.hallmark_mark_purity_grade_desc)
                )

                // 3. HUID
                HallmarkMarkRow(
                    icon = Icons.Default.QrCode,
                    title = stringResource(R.string.hallmark_mark_huid),
                    description = stringResource(R.string.hallmark_mark_huid_desc)
                )

                Text(
                    text = stringResource(R.string.hallmark_mandatory_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = HallmarkPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Section 3: Where to Find It
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.sheet_section_where_to_find_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.sheet_section_where_to_find_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // Section 4: How to Verify (Numbered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSystemDarkThemeGlobal)
                            Color.White.copy(alpha = 0.04f)
                        else
                            Color.Black.copy(alpha = 0.04f)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.sheet_section_how_to_verify_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                NumberedStepRow(stepNumber = 1, text = stringResource(R.string.sheet_step_1))
                NumberedStepRow(stepNumber = 2, text = stringResource(R.string.sheet_step_2))
                NumberedStepRow(stepNumber = 3, text = stringResource(R.string.sheet_step_3))

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onVerifyClick,
                    enabled = isVerifyEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HallmarkPrimary,
                        disabledContainerColor = HallmarkPrimary.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = if (isVerifyEnabled) DarkBackground else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_verify_bis_care),
                        color = if (isVerifyEnabled) DarkBackground else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Section 5: Disclaimer
            Text(
                text = stringResource(R.string.sheet_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun HallmarkMarkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HallmarkPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HallmarkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun NumberedStepRow(
    stepNumber: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HallmarkPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                color = HallmarkPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
