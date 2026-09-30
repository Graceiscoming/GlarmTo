package com.example.glarmto.ui.dashboard

import com.example.glarmto.data.util.ResourceTexts
import com.example.glarmto.ui.util.LanguageToggleButton
import com.example.glarmto.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glarmto.GlarmToApplication
import com.example.glarmto.ui.util.OnResume
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import com.example.glarmto.ui.workout.RecoveryViewModel
import com.example.glarmto.ui.workout.RecoveryViewModelFactory
import com.example.glarmto.ui.workout.RecoveryBarItem
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Composable
fun DashboardScreen(onLogout: () -> Unit = {}, onNavigateToHistory: (isMonthly: Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(application, application.repository)
    )

    OnResume { viewModel.refreshToday() }

    val recoveryViewModel: RecoveryViewModel = viewModel(
        factory = RecoveryViewModelFactory(application.repository, ResourceTexts(application))
    )

    val recoveryStatus by recoveryViewModel.recoveryStatus.collectAsState()
    val smartRecommendation by recoveryViewModel.smartRecommendation.collectAsState()
    var showRecovery by remember { mutableStateOf(false) }

    LaunchedEffect(showRecovery) {
        if (showRecovery) {
            recoveryViewModel.fetchAndCalculateRecovery()
        }
    }
    // Sets logged while the app was in the background should show up in an already open card.
    OnResume { if (showRecovery) recoveryViewModel.fetchAndCalculateRecovery() }

    val workouts by viewModel.todayWorkouts.collectAsState()
    val nutritions by viewModel.todayNutrition.collectAsState()
    val user by viewModel.user.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val weeklyVolume by viewModel.weeklyVolume.collectAsState()
    val trainingStreak by viewModel.trainingStreakDays.collectAsState()
    val periodStats by viewModel.periodTrainingStats.collectAsState()
    val statsPeriodDays by viewModel.statsPeriodDays.collectAsState()
    val todayWater by viewModel.todayWaterMl.collectAsState()
    val waterGoal by viewModel.waterGoalMl.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val radarData by viewModel.radarChartData.collectAsState()
    val sessions by viewModel.todaySessions.collectAsState()

    var showIgCustomizer by remember { mutableStateOf(false) }
    var igShowProfile by remember { mutableStateOf(true) }
    var igShowTime by remember { mutableStateOf(false) }
    var igShowCalories by remember { mutableStateOf(false) }
    var igShowExercises by remember { mutableStateOf(false) }

    val level = user?.level ?: 1
    val xp = user?.xp ?: 0
    
    val threshold = application.repository.getTotalXPThreshold(level)
    val required = application.repository.getXPRequiredForNextLevel(level)
    val xpProgress = if (required > 0) (xp - threshold).toFloat() / required.toFloat() else 0f

    val totalConsumed = nutritions.sumOf { it.calories }
    val progress = if (dailyGoal > 0) (totalConsumed.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    val remaining = (dailyGoal - totalConsumed).coerceAtLeast(0)

    val totalSets = workouts.size
    val totalVolume = workouts.sumOf { it.weight * it.reps }

    if (showIgCustomizer) {
        AlertDialog(
            onDismissRequest = { showIgCustomizer = false },
            title = { Text(stringResource(R.string.customize_ig_story), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = igShowProfile, onCheckedChange = { igShowProfile = it })
                        Text(stringResource(R.string.show_profile_level_streak))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = igShowTime, onCheckedChange = { igShowTime = it })
                        Text(stringResource(R.string.show_workout_time))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = igShowCalories, onCheckedChange = { igShowCalories = it })
                        Text(stringResource(R.string.show_calories_burned_estimated))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = igShowExercises, onCheckedChange = { igShowExercises = it })
                        Text(stringResource(R.string.show_exercises_summary))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val durationMinutes = sessions.sumOf { it.durationSeconds } / 60
                    val timeStr = if (durationMinutes > 0) context.getString(R.string.f_minutes_short, durationMinutes.toString()) else context.getString(R.string.less_than_1_min)
                    val caloriesStr = context.getString(R.string.f_kcal, (durationMinutes * 5).toString())
                    val exercisesStr = context.getString(R.string.f_ig_sets_volume, workouts.size.toString(), totalVolume.toString())

                    val username = application.repository.getCurrentUser() ?: context.getString(R.string.guest)
                    viewModel.shareToInstagramStory(
                        username = username,
                        level = level,
                        streak = trainingStreak,
                        showProfile = igShowProfile,
                        showTime = igShowTime,
                        timeText = context.getString(R.string.f_ig_duration, timeStr),
                        showCalories = igShowCalories,
                        caloriesText = context.getString(R.string.f_ig_burned, caloriesStr),
                        showExercises = igShowExercises,
                        exercisesText = exercisesStr
                    )
                    showIgCustomizer = false
                }) {
                    Text(stringResource(R.string.share_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIgCustomizer = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val username = application.repository.getCurrentUser() ?: context.getString(R.string.guest)
                    Text(stringResource(R.string.f_hello, username), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    
                    // Level & XP Bar
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                stringResource(R.string.f_lvl, level), 
                                color = Color.White, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(xpProgress)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.f_xp, (xp - threshold), required), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    var showHistoryMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showHistoryMenu = true }) {
                            Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history))
                        }
                        DropdownMenu(
                            expanded = showHistoryMenu,
                            onDismissRequest = { showHistoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.daily_view)) },
                                onClick = { 
                                    showHistoryMenu = false
                                    onNavigateToHistory(false) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.monthly_view)) },
                                onClick = { 
                                    showHistoryMenu = false
                                    onNavigateToHistory(true) 
                                }
                            )
                        }
                    }
                    LanguageToggleButton(application.languageManager)
                    var showThemeMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Filled.Palette, contentDescription = stringResource(R.string.themes))
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            com.example.glarmto.data.preferences.ThemeManager.availableThemes.forEach { themeName ->
                                DropdownMenuItem(
                                    text = { Text(themeName) },
                                    onClick = { 
                                        showThemeMenu = false
                                        application.themeManager.setTheme(themeName)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.logout), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        showIgCustomizer = true
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.share_to_ig_story), fontSize = 12.sp)
                }
                OutlinedButton(onClick = { viewModel.shareExportJson() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.export_json), fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.training_insights), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(stringResource(R.string.f_current_streak_day_s_with_at_least_one_set, trainingStreak), fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = statsPeriodDays == 7,
                            onClick = { viewModel.setStatsPeriodDays(7) },
                            label = { Text(stringResource(R.string.n_7_days)) }
                        )
                        FilterChip(
                            selected = statsPeriodDays == 30,
                            onClick = { viewModel.setStatsPeriodDays(30) },
                            label = { Text(stringResource(R.string.n_30_days)) }
                        )
                    }
                    Text(
                        "Volume: ${"%.0f".format(periodStats.totalVolume)} kg · Sets: ${periodStats.totalSets}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (heatmapData.size == 91) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.contribution_activity), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            for (col in 0 until 13) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (row in 0 until 7) {
                                        val index = col * 7 + row
                                        val sets = heatmapData[index]
                                        val color = when {
                                            sets == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                            sets in 1..2 -> MaterialTheme.colorScheme.primaryContainer
                                            sets in 3..5 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            sets in 6..9 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(color, RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.water_today), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.f_ml, todayWater, waterGoal), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (waterGoal > 0) (todayWater.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f
                        },
                        modifier = Modifier
                            .width(120.dp)
                            .height(10.dp)
                    )
                }
            }
        }

        // Workout Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = stringResource(R.string.workout), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.workout_summary), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.total_sets), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalSets", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.total_volume), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.f_kg, totalVolume), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Nutrition Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.nutrition), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.nutrition_summary), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    Text(stringResource(R.string.f_consumed_kcal_remaining_kcal, totalConsumed, remaining), fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                    )
                    Text(stringResource(R.string.f_daily_goal_kcal, dailyGoal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Weekly Volume Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.weekly_volume_progress), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    
                    if (weeklyVolume.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        val maxVolume = weeklyVolume.maxOfOrNull { it.second } ?: 1.0
                        val safeMaxVolume = if (maxVolume == 0.0) 1.0 else maxVolume
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklyVolume.forEach { (day, volume) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    val barHeight = (volume / safeMaxVolume).toFloat().coerceIn(0.05f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .fillMaxHeight(barHeight)
                                            .background(
                                                color = if (volume == safeMaxVolume && volume > 0) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(day, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showRecovery = !showRecovery },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.muscle_recovery), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = if (showRecovery) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showRecovery) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.ai_recommendation), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(smartRecommendation, fontSize = 14.sp)
                                }
                            }

                            if (recoveryStatus.isEmpty()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                recoveryStatus.forEach { status ->
                                    RecoveryBarItem(status = status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
