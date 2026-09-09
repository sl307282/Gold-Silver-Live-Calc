package com.goldsilver.livecalc.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilverCalculatorScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.goldsilver.livecalc.util.LocalAppStrings.current
    val latestRate by viewModel.latestRate.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Input states
    var weightInput by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("Gram") }
    var selectedPurity by remember { mutableStateOf("999") }
    var isPercentageCharge by remember { mutableStateOf(true) }
    var makingChargeInput by remember { mutableStateOf("") }
    var gstInput by remember { mutableStateOf("3") }
    var customRateInput by remember { mutableStateOf("") }
    var isEditingRate by remember { mutableStateOf(false) }
    var showGstInfo by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // Calculation states
    var calculatedValues by remember { mutableStateOf(CalculatedBreakdown(0.0, 0.0, 0.0, 0.0)) }
    var hasCalculated by remember { mutableStateOf(false) }

    val baseSilverPrice = latestRate?.silverPrice ?: 0.96
    val price999 = baseSilverPrice
    val price925 = baseSilverPrice * 0.925
    val price900 = baseSilverPrice * 0.90

    val currencySymbol = when (currency) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> currency
    }

    val currentRatePerGram = when (selectedPurity) {
        "999" -> price999; "925" -> price925; "900" -> price900; else -> price999
    }

    LaunchedEffect(selectedPurity, latestRate) {
        if (!isEditingRate) {
            customRateInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(currentRatePerGram)
        }
    }

    val finalRatePerGram = com.goldsilver.livecalc.util.IndianCurrencyFormatter.parseAmount(customRateInput).let {
        if (it <= 0.0) currentRatePerGram else it
    }

    // Trigger recalculation when inputs change (if already calculated)
    LaunchedEffect(
        hasCalculated,
        weightInput,
        selectedUnit,
        selectedPurity,
        finalRatePerGram,
        isPercentageCharge,
        makingChargeInput,
        gstInput
    ) {
        if (hasCalculated) {
            val weight = weightInput.toDoubleOrNull() ?: 0.0
            val makingCharge = com.goldsilver.livecalc.util.IndianCurrencyFormatter.parseAmount(makingChargeInput)
            val gstPercent = gstInput.toDoubleOrNull() ?: 0.0
            val unitMultiplier = when (selectedUnit) {
                "Kilogram" -> 1000.0; "Tola" -> 11.6638; "Ounce" -> 31.1035; "Milligram" -> 0.001; else -> 1.0
            }
            val weightGrams = weight * unitMultiplier
            val baseValue = weightGrams * finalRatePerGram
            val makingChargeAmount = if (isPercentageCharge) baseValue * (makingCharge / 100.0) else makingCharge
            val subtotal = baseValue + makingChargeAmount
            val gstAmount = subtotal * (gstPercent / 100.0)
            calculatedValues = CalculatedBreakdown(baseValue, makingChargeAmount, gstAmount, subtotal + gstAmount)
        }
    }

    if (showGstInfo) {
        AlertDialog(
            onDismissRequest = { showGstInfo = false },
            title = { Text(strings.gstTax, color = SilverPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("GST of 3% is calculated on the total of Silver Value + Making Charges.", color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { showGstInfo = false }) {
                    Text(strings.done, color = SilverPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.silverCalculator, color = SilverPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = SilverPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── 1. Purity (Silver) Selector at TOP OF PAGE (Optimum Compact) ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Purity (Silver)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("999" to "999\n(Fine)", "925" to "925\n(Sterling)", "900" to "900\n(Coin)").forEach { (purity, label) ->
                                val isSelected = selectedPurity == purity
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        SilverPrimary
                                    } else {
                                        if (isSystemDarkThemeGlobal) Color(0xFF1C1E2A) else Color(0xFFE5E7EB)
                                    },
                                    animationSpec = tween(200),
                                    label = "silverPurityBg"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        Color(0xFF0D0E15)
                                    } else {
                                        if (isSystemDarkThemeGlobal) Color(0xFF7E8299) else Color(0xFF6B7280)
                                    },
                                    animationSpec = tween(200),
                                    label = "silverPurityText"
                                )
                                val borderColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        SilverPrimary
                                    } else {
                                        if (isSystemDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                                    },
                                    animationSpec = tween(200),
                                    label = "silverPurityBorder"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPurity = purity
                                            isEditingRate = false
                                            focusManager.clearFocus()
                                            customRateInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(
                                                when (purity) { "999" -> price999; "925" -> price925; else -> price900 }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = textColor,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Live Rate Card (Below Purity) ───────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    if (isEditingRate) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customRateInput,
                                onValueChange = { customRateInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatInput(it) },
                                label = { Text("Rate ($currency/Gram)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SilverPrimary, focusedLabelColor = SilverPrimary,
                                    unfocusedBorderColor = Color.Gray, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            IconButton(onClick = {
                                isEditingRate = false; focusManager.clearFocus()
                                if (com.goldsilver.livecalc.util.IndianCurrencyFormatter.parseAmount(customRateInput) <= 0.0)
                                    customRateInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(currentRatePerGram)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = SilverPrimary)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                    val isCustom = customRateInput.toDoubleOrNull() != null &&
                                            Math.abs((customRateInput.toDoubleOrNull() ?: 0.0) - currentRatePerGram) > 0.01
                                    Text(
                                        text = if (isCustom) "Custom Rate ($selectedPurity)" else "Live Rate ($selectedPurity)",
                                        color = TextSecondary, fontSize = 12.sp
                                    )
                                }
                                if ((customRateInput.toDoubleOrNull() ?: 0.0).let { Math.abs(it - currentRatePerGram) > 0.01 }) {
                                    Text(
                                        text = "Reset to Live",
                                        color = SilverPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            isEditingRate = false; focusManager.clearFocus()
                                            customRateInput = String.format(java.util.Locale.US, "%.2f", currentRatePerGram)
                                        }.padding(top = 2.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatIndianStyle(finalRatePerGram),
                                        color = SilverPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp
                                    )
                                    Text(
                                        text = "$currency / gram",
                                        color = TextSecondary, fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { isEditingRate = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Rate", tint = SilverPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Input Fields (Below Live Rate: Weight, Unit, Making Charge, GST) ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {


                        // ── 2. Weight + Unit row ────────────────────────────
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { newValue -> 
                                    if (newValue.length <= 10 && newValue.count { it == '.' } <= 1) {
                                        weightInput = newValue 
                                    }
                                },
                                label = { Text("Weight") },
                                placeholder = { Text("Enter weight", color = TextSecondary.copy(alpha = 0.5f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SilverPrimary, focusedLabelColor = SilverPrimary,
                                    unfocusedBorderColor = Color.Gray, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.5f).height(58.dp),
                                singleLine = true
                            )
                            Box(modifier = Modifier.weight(1f).height(58.dp)) {
                                OutlinedTextField(
                                    value = selectedUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unit") },
                                    trailingIcon = { Text("▼", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.Gray, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { unitDropdownExpanded = true })
                                DropdownMenu(expanded = unitDropdownExpanded, onDismissRequest = { unitDropdownExpanded = false }) {
                                    listOf("Gram" to "Gram (g) ⭐", "Kilogram" to "Kilogram (kg)", "Tola" to "Tola (11.6638 g)", "Ounce" to "Ounce (oz)", "Milligram" to "Milligram (mg)")
                                        .forEach { (key, label) ->
                                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedUnit = key; unitDropdownExpanded = false })
                                        }
                                }
                            }
                        }

                        // ── 4. Making Charge ───────────────────────────────
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = makingChargeInput,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 12 && newValue.count { it == '.' } <= 1) {
                                        if (isPercentageCharge) {
                                            val num = newValue.toDoubleOrNull()
                                            if (newValue.isEmpty() || (num != null && num <= 100.0)) makingChargeInput = newValue
                                        } else {
                                            makingChargeInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatInput(newValue)
                                        }
                                    }
                                },
                                label = { Text(if (isPercentageCharge) "Making Charge (%)" else "Making Charge ($currency)", fontSize = 11.sp) },
                                placeholder = { Text("0", color = TextSecondary.copy(alpha = 0.4f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SilverPrimary, focusedLabelColor = SilverPrimary,
                                    unfocusedBorderColor = Color.Gray, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.4f),
                                singleLine = true
                            )
                            Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                                Text("Charge Type", color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                            .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                            .background(if (isPercentageCharge) SilverPrimary else Color.Transparent)
                                            .clickable { isPercentageCharge = true },
                                        contentAlignment = Alignment.Center
                                    ) { Text("%", color = if (isPercentageCharge) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                            .background(if (!isPercentageCharge) SilverPrimary else Color.Transparent)
                                            .clickable { isPercentageCharge = false },
                                        contentAlignment = Alignment.Center
                                    ) { Text(currencySymbol, color = if (!isPercentageCharge) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                }
                            }
                        }

                        // ── 5. GST ─────────────────────────────────────────
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = gstInput,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 6 && newValue.count { it == '.' } <= 1) {
                                        val num = newValue.toDoubleOrNull()
                                        if (newValue.isEmpty() || (num != null && num <= 50.0)) gstInput = newValue
                                    }
                                },
                                label = { Text("GST (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SilverPrimary, focusedLabelColor = SilverPrimary,
                                    unfocusedBorderColor = Color.Gray, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { showGstInfo = true }) {
                                Icon(Icons.Default.Info, contentDescription = "GST Info", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // ── 6. Estimated Breakdown ─────────────────────────────────────
            if (hasCalculated) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, SilverPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(strings.priceBreakdown.uppercase(), color = SilverPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                            HorizontalDivider(color = SilverPrimary.copy(alpha = 0.15f))
                            BreakdownRow(icon = "🪙", label = strings.baseMetalPrice, value = "${formatIndianStyle(calculatedValues.baseGoldValue)} $currency")
                            BreakdownRow(icon = "🛠", label = strings.totalMakingCharges, value = "${formatIndianStyle(calculatedValues.makingCharges)} $currency")
                            BreakdownRow(icon = "🧾", label = strings.gstAmount, value = "${formatIndianStyle(calculatedValues.gstAmount)} $currency")
                        }
                    }
                }

                // ── 7. Final Total Highlighted Card (Light Theme) ─────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        SilverPrimary,
                                        Color(0xFFE2E8F0),
                                        SilverPrimary
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFF8FAFC),
                                            Color(0xFFE2E8F0)
                                        )
                                    )
                                )
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = SilverPrimary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "💰",
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = strings.totalPayable.uppercase(),
                                            color = Color(0xFF334155),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "${formatIndianStyle(calculatedValues.finalTotal)} $currency",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = strings.totalPayable,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── 8. Action Buttons ──────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            weightInput = ""; makingChargeInput = ""; gstInput = "3"
                            selectedPurity = "999"; isPercentageCharge = true; hasCalculated = false
                            customRateInput = com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(price999)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SilverPrimary)
                    ) {
                        Text(strings.reset, color = SilverPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            hasCalculated = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(100)
                                listState.animateScrollToItem(index = listState.layoutInfo.totalItemsCount - 1)
                            }
                        },
                        modifier = Modifier.weight(1.6f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SilverPrimary)
                    ) {
                        Text(strings.calculate, color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun BreakdownRow(icon: String, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon, fontSize = 16.sp)
            Text(label, color = TextSecondary, fontSize = 14.sp)
        }
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun formatIndianStyle(value: Double): String {
    return com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(value)
}
