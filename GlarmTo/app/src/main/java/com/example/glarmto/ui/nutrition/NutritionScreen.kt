package com.example.glarmto.ui.nutrition

import com.example.glarmto.data.util.BarcodeNutrition
import com.example.glarmto.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glarmto.GlarmToApplication
import com.example.glarmto.ui.util.OnResume
import com.example.glarmto.data.util.CalendarDayUtils
import com.example.glarmto.data.util.HealthCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.glarmto.ui.camera.CameraScannerScreen
import com.example.glarmto.ui.camera.ScannerMode
import com.example.glarmto.data.util.NetworkUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication
    val viewModel: NutritionViewModel = viewModel(
        factory = NutritionViewModelFactory(application, application.repository)
    )

    OnResume { viewModel.refreshToday() }

    val nutritions by viewModel.nutritionList.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val today by viewModel.today.collectAsState()
    val user by viewModel.userFlow.collectAsState()
    val waterEntries by viewModel.waterEntries.collectAsState()

    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var isEditingGoal by remember { mutableStateOf(false) }
    var tempGoal by remember { mutableStateOf(dailyGoal.toString()) }

    val focusManager = LocalFocusManager.current

    val totalConsumed = nutritions.sumOf { it.calories }
    val progress = if (dailyGoal > 0) (totalConsumed.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    val remaining = (dailyGoal - totalConsumed).coerceAtLeast(0)
    val totalWater = waterEntries.sumOf { it.amountMl }
    val waterGoal = user?.dailyWaterGoalMl ?: 2000
    val (pG, cG, fG) = user?.let { u ->
        HealthCalculator.macroGramsFromCalories(dailyGoal, u.macroProteinPct, u.macroCarbPct, u.macroFatPct)
    } ?: Triple(0, 0, 0)

    val isNutritionDateValid = remember(selectedDate, today) {
        val (start, end) = CalendarDayUtils.nutritionEditableLocalRange(today)
        val day = CalendarDayUtils.normalizeToLocalDayStart(selectedDate)
        day in start..end
    }

    var activeScannerMode by remember { mutableStateOf<ScannerMode?>(null) }
    // A scanned barcode whose nutrition the user is about to type in; saved with the entry so it is found next time.
    var barcodeToRemember by remember { mutableStateOf<String?>(null) }
    
    if (activeScannerMode != null) {
        CameraScannerScreen(
            mode = activeScannerMode!!,
            onResult = { macroData, scannedBarcode ->
                val named = macroData.productName.ifBlank {
                    context.getString(if (activeScannerMode == ScannerMode.OCR) R.string.scanned_label else R.string.scanned_product)
                }
                // Values for 100 g (not one serving) are marked so they aren't mistaken for a portion.
                foodName = if (macroData.per100g) named + " " + context.getString(R.string.per_100g_suffix) else named
                if (macroData.nutritionKnown) {
                    calories = macroData.calories.toString()
                    barcodeToRemember = null
                } else {
                    // Only the name is known: the user fills in the calories from the pack, and we remember them.
                    calories = ""
                    barcodeToRemember = scannedBarcode
                }
                activeScannerMode = null
            },
            onCancel = { activeScannerMode = null },
            onEnterManually = { barcodeToRemember = it }
        )
        return // Take over the entire screen while scanning
    }

    var showNoInternetDialog by remember { mutableStateOf(false) }

    if (showNoInternetDialog) {
        AlertDialog(
            onDismissRequest = { showNoInternetDialog = false },
            title = { Text(stringResource(R.string.no_internet_connection), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.please_connect_to_the_internet_to_use_this)) },
            confirmButton = {
                TextButton(onClick = { showNoInternetDialog = false }) { Text(stringResource(R.string.ok)) }
            }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val localDay = CalendarDayUtils.localDayStartFromMaterialPickerUtc(utcTimeMillis)
                return CalendarDayUtils.isMillisInNutritionEditableRange(localDay)
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.setSelectedDateFromMaterialPicker(it)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header Section with Date and Daily Goal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateStr = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(selectedDate))
            Text(stringResource(R.string.f_nutrition_on, dateStr), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.change_date), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isEditingGoal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tempGoal,
                        onValueChange = { tempGoal = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                    TextButton(onClick = {
                        val newGoal = tempGoal.toIntOrNull()
                        if (newGoal != null && newGoal > 0) {
                            viewModel.updateDailyGoal(newGoal)
                        }
                        isEditingGoal = false
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.f_goal_kcal, dailyGoal), fontWeight = FontWeight.Medium)
                    if (isNutritionDateValid) {
                        IconButton(onClick = {
                            tempGoal = dailyGoal.toString()
                            isEditingGoal = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_goal), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (isNutritionDateValid) {
                TextButton(onClick = { viewModel.copyMealsFromYesterday() }) {
                    Text(stringResource(R.string.copy_yesterday), maxLines = 1)
                }
            }
        }

        // Progress Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.f_consumed_kcal_remaining_kcal, totalConsumed, remaining), fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )
                user?.let { u ->
                    Text(
                        stringResource(R.string.f_macro_split_p_c_f_g_g_g_edit_in_profile, u.macroProteinPct, u.macroCarbPct, u.macroFatPct, pG, cG, fG),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.water_this_day), fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.glarmto.ui.nutrition.AnimatedWaterGlass(
                        fillPercentage = if (waterGoal > 0) (totalWater.toFloat() / waterGoal) else 0f,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalWater}", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.f_ml_2, waterGoal), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isNutritionDateValid) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.addWater(250) }) { Text(stringResource(R.string.n_250_ml)) }
                        Button(onClick = { viewModel.addWater(500) }) { Text(stringResource(R.string.n_500_ml)) }
                    }
                }
                waterEntries.forEach { w ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.f_ml_3, w.amountMl), style = MaterialTheme.typography.bodyMedium)
                        if (isNutritionDateValid) {
                            IconButton(onClick = { viewModel.deleteWater(w.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Manual Input Form (Only show if date is valid)
        if (isNutritionDateValid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text(stringResource(R.string.food_name_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            label = { Text(stringResource(R.string.calories_kcal)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val cal = calories.trim().toIntOrNull()
                                if (cal != null && cal > 0) {
                                    val name = if (foodName.isNotBlank()) foodName.trim() else context.getString(R.string.quick_add)
                                    viewModel.addNutrition(foodName = name, calories = cal)
                                    barcodeToRemember?.let {
                                        (context.applicationContext as GlarmToApplication).productLookup
                                            .remember(it, BarcodeNutrition(name, cal, 0, 0, 0))
                                    }
                                    barcodeToRemember = null
                                    foodName = ""
                                    calories = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.add))
                        }
                    }
                    
                    if (barcodeToRemember != null) {
                        Text(
                            stringResource(R.string.barcode_enter_calories_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            // Works without internet: the bundled product list covers common Thai products.
                            onClick = { activeScannerMode = ScannerMode.BARCODE },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.scan_barcode))
                        }
                        OutlinedButton(
                            onClick = { activeScannerMode = ScannerMode.OCR },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.scan_label))
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    stringResource(R.string.editing_past_data_is_disabled_you_can_only),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }

        Divider()
            }
        }

        // List of eaten foods today
        items(nutritions) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.foodName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(stringResource(R.string.f_kcal, item.calories), color = MaterialTheme.colorScheme.primary)
                        }
                        if (isNutritionDateValid) {
                            IconButton(onClick = { viewModel.deleteNutrition(item.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
}
