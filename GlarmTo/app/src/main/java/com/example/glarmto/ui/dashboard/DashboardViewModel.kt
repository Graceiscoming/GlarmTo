package com.example.glarmto.ui.dashboard

import com.example.glarmto.data.util.ResourceTexts
import com.example.glarmto.R
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
import com.example.glarmto.data.util.CalendarDayUtils
import com.example.glarmto.data.util.MuscleBalance
import com.example.glarmto.data.util.TodayTracker
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
    private val repository: GlarmToRepository,
    todayProvider: () -> Long = CalendarDayUtils::localTodayStartMillis
) : AndroidViewModel(application) {

    private val texts = ResourceTexts(application)

    // "Today" must follow the calendar, not the moment this ViewModel was created.
    private val dayTracker = TodayTracker(todayProvider)

    /** Call when the screen is shown again so a day change (e.g. after midnight) is picked up. */
    fun refreshToday() {
        dayTracker.refresh()
    }

    val user: StateFlow<UserEntity?> = repository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyGoal: StateFlow<Int> = user
        .map { user -> user?.dailyGoal ?: 2500 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2500)

    fun refreshGoal() {
        // Goal is now refreshed automatically via Room flow
    }

    val todayWorkouts: StateFlow<List<WorkoutEntity>> = dayTracker.today.flatMapLatest { repository.getWorkoutsForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions: StateFlow<List<com.example.glarmto.data.local.entity.WorkoutSessionEntity>> = dayTracker.today.flatMapLatest { repository.getWorkoutSessionsForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayNutrition: StateFlow<List<NutritionEntity>> = dayTracker.today.flatMapLatest { repository.getNutritionForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWaterMl: StateFlow<Int> = dayTracker.today.flatMapLatest { repository.getWaterForDay(it) }
        .map { list -> list.sumOf { it.amountMl } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val waterGoalMl: StateFlow<Int> = user
        .map { it?.dailyWaterGoalMl ?: 2000 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val trainingStreakDays: StateFlow<Int> = dayTracker.today.flatMapLatest { repository.getWorkoutsForDay(it) }
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
        dayTracker.today.flatMapLatest { repository.getWorkoutsForDay(it) },
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
                putExtra(Intent.EXTRA_SUBJECT, texts.get(R.string.backup_subject_json))
            }
            val chooser = Intent.createChooser(send, texts.get(R.string.export_json))
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
                putExtra(Intent.EXTRA_SUBJECT, texts.get(R.string.backup_subject_csv))
            }
            val chooser = Intent.createChooser(send, texts.get(R.string.export_csv))
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
                brandText = texts.get(R.string.ig_brand),
                levelText = texts.get(R.string.f_ig_level, level),
                streakText = texts.get(R.string.f_ig_streak, streak),
                chooserTitle = texts.get(R.string.share_to_story),
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

    val weeklyVolume: StateFlow<List<Pair<String, Double>>> = combine(repository.getUserFlow(), dayTracker.today) { _, _ -> Unit }
        .flatMapLatest {
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

    val heatmapData: StateFlow<List<Int>> = combine(repository.getUserFlow(), dayTracker.today) { _, _ -> Unit }
        .flatMapLatest {
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

    val radarChartData: StateFlow<Map<String, Float>> = combine(repository.getUserFlow(), dayTracker.today) { _, _ -> Unit }
        .flatMapLatest {
            val cal = Calendar.getInstance()
            val endMillis = repository.getDayRange(cal).second
            cal.add(Calendar.DAY_OF_YEAR, -30) // Last 30 days for Radar
            val startMillis = repository.getDayRange(cal).first

            repository.getWorkoutsForRange(startMillis, endMillis).map { workouts ->
                MuscleBalance.radarScores(workouts.map { it.exerciseName })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
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
