package com.goldsilver.livecalc.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldsilver.livecalc.ui.theme.*
import com.goldsilver.livecalc.ui.viewmodel.GoldSilverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    viewModel: GoldSilverViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestRate by viewModel.latestRate.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val focusManager = LocalFocusManager.current

    // Input states
    var weightInput by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("Gram") }
    var selectedPurity by remember { mutableStateOf("24K") }
    var isPercentageCharge by remember { mutableStateOf(true) }
    var makingChargeInput by remember { mutableStateOf("") }
    var gstInput by remember { mutableStateOf("3") }
    var customRateInput by remember { mutableStateOf("") }
    var isEditingRate by remember { mutableStateOf(false) }
    var showGstInfo by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // Trigger calculation manually
    var triggerCalc by remember { mutableStateOf(0) }
    var calculatedValues by remember { mutableStateOf(CalculatedBreakdown(0.0, 0.0, 0.0, 0.0)) }
    var hasCalculated by remember { mutableStateOf(false) }

    val rate24k = latestRate?.goldPrice24k ?: 76.20
    val rate22k = latestRate?.goldPrice22k ?: (rate24k * 0.9167)
    val rate18k = latestRate?.goldPrice18k ?: (rate24k * 0.75)
    val rate14k = latestRate?.goldPrice14k ?: (rate24k * 0.5833)

    val currencySymbol = when (currency) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> currency
    }

    val currentRatePerGram = when (selectedPurity) {
        "24K" -> rate24k; "22K" -> rate22k; "18K" -> rate18k; "14K" -> rate14k; else -> rate24k
    }

    LaunchedEffect(selectedPurity, latestRate) {
        if (!isEditingRate) {
            customRateInput = String.format(java.util.Locale.US, "%.2f", currentRatePerGram)
        }
    }

    val finalRatePerGram = customRateInput.toDoubleOrNull() ?: currentRatePerGram

    // Trigger recalculation when button is pressed
    LaunchedEffect(triggerCalc) {
        if (triggerCalc > 0) {
            val weight = weightInput.toDoubleOrNull() ?: 0.0
            val makingCharge = makingChargeInput.toDoubleOrNull() ?: 0.0
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
            hasCalculated = true
        }
    }

    if (showGstInfo) {
        AlertDialog(
            onDismissRequest = { showGstInfo = false },
            title = { Text("About GST", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("GST (Goods & Services Tax) of 3% is applicable on gold jewelry in India. It is calculated on the total of Gold Value + Making Charges.", color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { showGstInfo = false }) {
                    Text("Got it", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gold Calculator", color = GoldPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
                    }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── 1. Live Rate Card ──────────────────────────────────────────
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
                                onValueChange = { customRateInput = it },
                                label = { Text("Rate ($currency/Gram)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary, focusedLabelColor = GoldPrimary,
                                    unfocusedBorderColor = Color.Gray, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            IconButton(onClick = {
                                isEditingRate = false; focusManager.clearFocus()
                                if (customRateInput.toDoubleOrNull() == null)
                                    customRateInput = String.format(java.util.Locale.US, "%.2f", currentRatePerGram)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = GoldPrimary)
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
                                    // Green live dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
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
                                        color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                isEditingRate = false; focusManager.clearFocus()
                                                customRateInput = String.format(java.util.Locale.US, "%.2f", currentRatePerGram)
                                            }
                                            .padding(top = 2.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.2f", finalRatePerGram),
                                        color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp
                                    )
                                    Text(
                                        text = "$currency / gram",
                                        color = TextSecondary, fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { isEditingRate = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Rate", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Input Fields ────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Weight + Unit row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = { Text("Weight") },
                                placeholder = { Text("Enter weight", color = TextSecondary.copy(alpha = 0.5f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary, focusedLabelColor = GoldPrimary,
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

                        // ── 3. Purity Buttons ──────────────────────────────
                        Column {
                            Text("Purity (Karat)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("24K", "22K", "18K", "14K").forEach { purity ->
                                    val isSelected = selectedPurity == purity
                                    val bgColor by animateColorAsState(
                                        targetValue = if (isSelected) GoldPrimary else (if (isSystemDarkThemeGlobal) Color(0xFF2A2B38) else Color(0xFFE0E0E0)),
                                        animationSpec = tween(250), label = "purityBg"
                                    )
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) DarkBackground else TextPrimary,
                                        animationSpec = tween(250), label = "purityText"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bgColor)
                                            .clickable {
                                                selectedPurity = purity; isEditingRate = false; focusManager.clearFocus()
                                                customRateInput = String.format(java.util.Locale.US, "%.2f", when (purity) { "24K" -> rate24k; "22K" -> rate22k; "18K" -> rate18k; else -> rate14k })
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(purity, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // ── 4. Making Charge (Segmented + Input in one row) ─
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = makingChargeInput,
                                onValueChange = { newValue ->
                                    if (isPercentageCharge) {
                                        val num = newValue.toDoubleOrNull()
                                        if (newValue.isEmpty() || (num != null && num <= 100.0)) makingChargeInput = newValue
                                    } else {
                                        val digits = newValue.filter { it.isDigit() }
                                        if (digits.length <= 12) makingChargeInput = newValue
                                    }
                                },
                                label = { Text(if (isPercentageCharge) "Making Charge (%)" else "Making Charge ($currency)", fontSize = 11.sp) },
                                placeholder = { Text("0", color = TextSecondary.copy(alpha = 0.4f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary, focusedLabelColor = GoldPrimary,
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
                                            .background(if (isPercentageCharge) GoldPrimary else Color.Transparent)
                                            .clickable { isPercentageCharge = true },
                                        contentAlignment = Alignment.Center
                                    ) { Text("%", color = if (isPercentageCharge) DarkBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                            .background(if (!isPercentageCharge) GoldPrimary else Color.Transparent)
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
                                    val num = newValue.toDoubleOrNull()
                                    if (newValue.isEmpty() || (num != null && num <= 50.0)) gstInput = newValue
                                },
                                label = { Text("GST (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary, focusedLabelColor = GoldPrimary,
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
                        modifier = Modifier.fillMaxWidth().border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("ESTIMATED BREAKDOWN", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                            HorizontalDivider(color = GoldPrimary.copy(alpha = 0.15f))

                            BreakdownRow(icon = "🪙", label = "Gold Value", value = String.format("%.2f %s", calculatedValues.baseGoldValue, currency))
                            BreakdownRow(icon = "🛠", label = "Making Charges", value = String.format("%.2f %s", calculatedValues.makingCharges, currency))
                            BreakdownRow(icon = "🧾", label = "GST Amount", value = String.format("%.2f %s", calculatedValues.gstAmount, currency))
                        }
                    }
                }

                // ── 7. Final Total Highlighted Card ────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().let { androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.5f)) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💰 Final Payable", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = String.format("%.2f %s", calculatedValues.finalTotal, currency),
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                textAlign = TextAlign.Center
                            )
                            Text("Including GST", color = TextSecondary, fontSize = 11.sp)
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
                            selectedPurity = "24K"; isPercentageCharge = true; hasCalculated = false
                            customRateInput = String.format(java.util.Locale.US, "%.2f", rate24k)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                    ) {
                        Text("Reset", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick = { focusManager.clearFocus(); triggerCalc++ },
                        modifier = Modifier.weight(1.6f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Calculate", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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

data class CalculatedBreakdown(
    val baseGoldValue: Double,
    val makingCharges: Double,
    val gstAmount: Double,
    val finalTotal: Double
)
