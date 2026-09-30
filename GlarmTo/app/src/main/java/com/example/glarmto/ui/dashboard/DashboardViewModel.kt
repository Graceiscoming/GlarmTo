package com.example.glarmto.ui.dashboard

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.repository.PeriodTrainingStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.glarmto.data.util.InstagramShareHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel for the Dashboard Screen.
 * 
 * Responsible for gathering and formatting daily statistics (workouts, nutrition, water),
 * calculating XP, streaks, and formatting chart data. Uses StateFlow to emit state
 * to the UI in a lifecycle-aware manner.
 */
class DashboardViewModel(
    private val application: Application,
    private val repository: GlarmToRepository
) : AndroidViewModel(application) {

    val user: StateFlow<UserEntity?> = repository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyGoal: StateFlow<Int> = user
        .map { user -> user?.dailyGoal ?: 2500 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2500)

    fun refreshGoal() {
        // Goal is now refreshed automatically via Room flow
    }

    private fun getTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayWorkouts: StateFlow<List<WorkoutEntity>> = repository.getWorkoutsForDay(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions: StateFlow<List<com.example.glarmto.data.local.entity.WorkoutSessionEntity>> = repository.getWorkoutSessionsForDay(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayNutrition: StateFlow<List<NutritionEntity>> = repository.getNutritionForDay(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWaterMl: StateFlow<Int> = repository.getWaterForDay(getTodayStartMillis())
        .map { list -> list.sumOf { it.amountMl } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val waterGoalMl: StateFlow<Int> = user
        .map { it?.dailyWaterGoalMl ?: 2000 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val trainingStreakDays: StateFlow<Int> = repository.getTodayWorkouts()
        .flatMapLatest {
            flow { emit(repository.getTrainingStreakDays()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _statsPeriodDays = MutableStateFlow(7)
    val statsPeriodDays: StateFlow<Int> = _statsPeriodDays.asStateFlow()

    fun setStatsPeriodDays(days: Int) {
        _statsPeriodDays.value = if (days >= 20) 30 else 7
    }

    val periodTrainingStats: StateFlow<PeriodTrainingStats> = combine(
        repository.getUserFlow(),
        repository.getTodayWorkouts(),
        _statsPeriodDays
    ) { _, _, days -> days }
        .flatMapLatest { days ->
            flow { emit(repository.getPeriodTrainingStats(days)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodTrainingStats(0.0, 0))

    fun shareExportJson() {
        viewModelScope.launch {
            val text = repository.exportUserDataJson()
            if (text.isBlank()) return@launch
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "GlarmTo backup (JSON)")
            }
            val chooser = Intent.createChooser(send, "Export JSON")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(chooser)
        }
    }

    fun shareExportCsv() {
        viewModelScope.launch {
            val text = repository.exportUserDataCsv()
            if (text.isBlank()) return@launch
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "GlarmTo backup (CSV)")
            }
            val chooser = Intent.createChooser(send, "Export CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(chooser)
        }
    }

    /**
     * Delegates Instagram Story sharing to the utility class to maintain SRP.
     */
    fun shareToInstagramStory(
        username: String, 
        level: Int, 
        streak: Int,
        showProfile: Boolean = true,
        showTime: Boolean = false,
        timeText: String = "",
        showCalories: Boolean = false,
        caloriesText: String = "",
        showExercises: Boolean = false,
        exercisesText: String = ""
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            InstagramShareHelper.shareToInstagramStory(
                context = application,
                username = username,
                level = level,
                streak = streak,
                showProfile = showProfile,
                showTime = showTime,
                timeText = timeText,
                showCalories = showCalories,
                caloriesText = caloriesText,
                showExercises = showExercises,
                exercisesText = exercisesText
            )
        }
    }

    val weeklyVolume: StateFlow<List<Pair<String, Double>>> = repository.getUserFlow()
        .flatMapLatest { user ->
            val cal = Calendar.getInstance()
            val endMillis = repository.getDayRange(cal).second
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val startMillis = repository.getDayRange(cal).first
            
            repository.getWorkoutsForRange(startMillis, endMillis).map { workouts ->
                val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                val volumeMap = mutableMapOf<String, Double>()
                
                // Initialize last 7 days with 0
                for (i in 0..6) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    volumeMap[sdf.format(c.time)] = 0.0
                }
                
                // Fill with actual data
                workouts.forEach { w ->
                    val dayName = sdf.format(java.util.Date(w.dateInMillis))
                    volumeMap[dayName] = (volumeMap[dayName] ?: 0.0) + (w.weight * w.reps)
                }
                
                // Return in chronological order (oldest to newest)
                volumeMap.toList().reversed()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapData: StateFlow<List<Int>> = repository.getUserFlow()
        .flatMapLatest { user ->
            val cal = Calendar.getInstance()
            val endMillis = repository.getDayRange(cal).second
            cal.add(Calendar.DAY_OF_YEAR, -90)
            val startMillis = repository.getDayRange(cal).first
            
            repository.getWorkoutsForRange(startMillis, endMillis).map { workouts ->
                val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val setsPerDay = mutableMapOf<String, Int>()
                
                for (i in 0..90) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    setsPerDay[dayFormat.format(c.time)] = 0
                }
                
                workouts.forEach { w ->
                    val dayStr = dayFormat.format(java.util.Date(w.dateInMillis))
                    setsPerDay[dayStr] = (setsPerDay[dayStr] ?: 0) + 1
                }
                
                setsPerDay.toList().reversed().map { it.second }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val radarChartData: StateFlow<Map<String, Float>> = repository.getUserFlow()
        .flatMapLatest { user ->
            val cal = Calendar.getInstance()
            val endMillis = repository.getDayRange(cal).second
            cal.add(Calendar.DAY_OF_YEAR, -30) // Last 30 days for Radar
            val startMillis = repository.getDayRange(cal).first
            
            repository.getWorkoutsForRange(startMillis, endMillis).map { workouts ->
                val counts = mutableMapOf(
                    "Chest" to 0, "Back" to 0, "Legs" to 0, "Arms" to 0, "Shoulders" to 0
                )
                
                workouts.forEach { w ->
                    val name = w.exerciseName.lowercase()
                    when {
                        name.contains("bench") || name.contains("chest") || name.contains("push") || name.contains("pec") -> counts["Chest"] = counts["Chest"]!! + 1
                        name.contains("pull") || name.contains("row") || name.contains("back") || name.contains("lat") -> counts["Back"] = counts["Back"]!! + 1
                        name.contains("squat") || name.contains("leg") || name.contains("calf") || name.contains("press") -> counts["Legs"] = counts["Legs"]!! + 1
                        name.contains("curl") || name.contains("tri") || name.contains("bi") || name.contains("arm") -> counts["Arms"] = counts["Arms"]!! + 1
                        name.contains("shoulder") || name.contains("overhead") || name.contains("raise") || name.contains("delt") -> counts["Shoulders"] = counts["Shoulders"]!! + 1
                        else -> {} // Uncat
                    }
                }
                
                val maxSets = counts.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                counts.mapValues { it.value.toFloat() / maxSets } // Normalize 0f to 1f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val muscleRecoveryState: StateFlow<Map<String, Int>> = repository.getUserFlow()
        .flatMapLatest { user ->
            val cal = Calendar.getInstance()
            val endMillis = repository.getDayRange(cal).second
            cal.add(Calendar.HOUR_OF_DAY, -48) // Last 48 hours for Fatigue
            val startMillis = cal.timeInMillis
            
            repository.getWorkoutsForRange(startMillis, endMillis).map { workouts ->
                val counts = mutableMapOf(
                    "Chest" to 0, "Back" to 0, "Legs" to 0, "Arms" to 0, "Shoulders" to 0
                )
                
                workouts.forEach { w ->
                    val name = w.exerciseName.lowercase()
                    when {
                        name.contains("bench") || name.contains("chest") || name.contains("push") || name.contains("pec") -> counts["Chest"] = counts["Chest"]!! + 1
                        name.contains("pull") || name.contains("row") || name.contains("back") || name.contains("lat") -> counts["Back"] = counts["Back"]!! + 1
                        name.contains("squat") || name.contains("leg") || name.contains("calf") || name.contains("press") -> counts["Legs"] = counts["Legs"]!! + 1
                        name.contains("curl") || name.contains("tri") || name.contains("bi") || name.contains("arm") -> counts["Arms"] = counts["Arms"]!! + 1
                        name.contains("shoulder") || name.contains("overhead") || name.contains("raise") || name.contains("delt") -> counts["Shoulders"] = counts["Shoulders"]!! + 1
                        else -> {} 
                    }
                }
                
                // Recovery formula: 100% - (sets * 10). Coerce 0 to 100.
                counts.mapValues { (100 - (it.value * 10)).coerceIn(0, 100) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mapOf(
            "Chest" to 100, "Back" to 100, "Legs" to 100, "Arms" to 100, "Shoulders" to 100
        ))
}

class DashboardViewModelFactory(
    private val application: Application,
    private val repository: GlarmToRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
