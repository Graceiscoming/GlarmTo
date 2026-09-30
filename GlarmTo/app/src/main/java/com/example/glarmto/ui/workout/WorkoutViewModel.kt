package com.example.glarmto.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.local.entity.RoutineEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.local.entity.WorkoutSessionEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.AiGeneratorContext
import com.example.glarmto.data.util.CalendarDayUtils
import com.example.glarmto.data.util.ExerciseHistoryStats
import com.example.glarmto.data.util.ExerciseLibrary
import com.example.glarmto.data.util.RecoveryCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkoutViewModel(private val repository: GlarmToRepository) : ViewModel() {

    private var stopwatchJob: Job? = null

    private val _isWorkingOut = MutableStateFlow(false)
    val isWorkingOut: StateFlow<Boolean> = _isWorkingOut.asStateFlow()

    private val _workoutStartTime = MutableStateFlow(0L)
    val workoutStartTime: StateFlow<Long> = _workoutStartTime.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _selectedDate = MutableStateFlow(CalendarDayUtils.localTodayStartMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _smartSuggestion = MutableStateFlow<String?>(null)
    val smartSuggestion: StateFlow<String?> = _smartSuggestion.asStateFlow()

    fun fetchSmartSuggestion(exerciseName: String) {
        if (exerciseName.isBlank()) {
            _smartSuggestion.value = null
            return
        }
        viewModelScope.launch {
            val lastWorkout = repository.getSmartSuggestion(exerciseName.trim())
            if (lastWorkout == null) {
                _smartSuggestion.value = null
            } else {
                val weight = lastWorkout.weight
                val rpe = lastWorkout.rpe ?: 0
                val reps = lastWorkout.reps
                if (rpe in 1..7) {
                    _smartSuggestion.value = "Suggestion: Try ${(weight + 2.5)}kg (Last time: ${weight}kg, RPE $rpe)"
                } else if (rpe in 8..9) {
                    _smartSuggestion.value = "Suggestion: Target ${weight}kg for ${reps + 1} reps (Last time: RPE $rpe)"
                } else if (rpe == 10) {
                    _smartSuggestion.value = "Suggestion: Stay at ${weight}kg or drop to ${(weight - 2.5).coerceAtLeast(0.0)}kg (Last time: RPE 10)"
                } else {
                    _smartSuggestion.value = "Suggestion: Last time you did ${weight}kg x $reps reps. Try to beat it!"
                }
            }
        }
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val workouts: StateFlow<List<WorkoutEntity>> = combine(_selectedDate, _currentSessionId) { date, sessionId -> 
            Pair(date, sessionId) 
        }
        .flatMapLatest { (date, sessionId) ->
            if (sessionId != null) {
                repository.getWorkoutsForSession(sessionId)
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                val (start, end) = repository.getDayRange(cal)
                repository.getWorkoutsForRange(start, end)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val sessionsForDate: StateFlow<List<WorkoutSessionEntity>> = _selectedDate
        .flatMapLatest { date ->
            repository.getWorkoutSessionsForDay(date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customRoutines: StateFlow<List<RoutineEntity>> = repository.getRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultRestTime: StateFlow<Int> = repository.getUserFlow()
        .map { it?.defaultRestSeconds ?: 60 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val userFlow: StateFlow<UserEntity?> = repository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayNutrition: StateFlow<List<NutritionEntity>> = repository.getTodayNutrition()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** From [androidx.compose.material3.DatePicker] (UTC day start). */
    fun setSelectedDateFromMaterialPicker(utcPickerMillis: Long) {
        _selectedDate.value = CalendarDayUtils.localDayStartFromMaterialPickerUtc(utcPickerMillis)
    }

    /** From "today" button or any local wall-clock instant. */
    fun setSelectedDateFromLocalInstant(wallMillis: Long) {
        _selectedDate.value = CalendarDayUtils.normalizeToLocalDayStart(wallMillis)
    }

    private val _restTimeSeconds = MutableStateFlow(0)
    val restTimeSeconds: StateFlow<Int> = _restTimeSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _initialRestTime = MutableStateFlow(60)
    val initialRestTime: StateFlow<Int> = _initialRestTime.asStateFlow()

    private var restTimerJob: Job? = null

    fun setRestTimer(seconds: Int) {
        _restTimeSeconds.value = seconds
        _initialRestTime.value = seconds
    }

    fun startRestTimer() {
        if (_restTimeSeconds.value > 0) {
            _isTimerRunning.value = true
            restTimerJob?.cancel()
            restTimerJob = viewModelScope.launch {
                while (isActive && _isTimerRunning.value && _restTimeSeconds.value > 0) {
                    delay(1000)
                    _restTimeSeconds.value -= 1
                    if (_restTimeSeconds.value <= 0) {
                        _isTimerRunning.value = false
                    }
                }
            }
        }
    }

    fun stopRestTimer() {
        _isTimerRunning.value = false
        restTimerJob?.cancel()
    }

    fun addRestTime(seconds: Int) {
        val next = _restTimeSeconds.value + seconds
        if (next > 0) {
            _restTimeSeconds.value = next
        } else {
            _restTimeSeconds.value = 0
            stopRestTimer()
        }
    }

    private fun startStopwatchTickerIfNeeded() {
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (isActive && _isWorkingOut.value && _workoutStartTime.value > 0) {
                _elapsedSeconds.value = (System.currentTimeMillis() - _workoutStartTime.value) / 1000
                delay(1000)
            }
        }
    }

    fun startWorkout() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = WorkoutSessionEntity(
                startTimeInMillis = now,
                dateInMillis = _selectedDate.value
            )
            val id = repository.insertWorkoutSession(session).toInt()
            _currentSessionId.value = id
            _workoutStartTime.value = now
            _elapsedSeconds.value = 0L
            _isWorkingOut.value = true
            startStopwatchTickerIfNeeded()
        }
    }

    fun endWorkout(
        name: String = "",
        notes: String = "",
        exhaustion: Int = 0,
        satisfaction: Int = 0
    ) {
        viewModelScope.launch {
            val sId = _currentSessionId.value ?: return@launch
            val now = System.currentTimeMillis()
            val duration = (now - _workoutStartTime.value) / 1000
            
            val finalName = if (name.isBlank()) {
                val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                "${sdf.format(java.util.Date(_selectedDate.value))} Workout"
            } else name

            val session = WorkoutSessionEntity(
                sessionId = sId,
                startTimeInMillis = _workoutStartTime.value,
                endTimeInMillis = now,
                durationSeconds = duration,
                dateInMillis = _selectedDate.value,
                sessionName = finalName,
                notes = notes,
                exhaustionLevel = exhaustion,
                satisfactionLevel = satisfaction
            )
            repository.updateWorkoutSession(session)

            stopwatchJob?.cancel()
            stopwatchJob = null
            _isWorkingOut.value = false
            _currentSessionId.value = null
            _workoutStartTime.value = 0L
            _elapsedSeconds.value = 0L
        }
    }

    override fun onCleared() {
        stopwatchJob?.cancel()
        super.onCleared()
    }

    fun addWorkout(exerciseName: String, weight: Double, reps: Int, rpe: Int? = null) {
        viewModelScope.launch {
            val workout = WorkoutEntity(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                dateInMillis = _selectedDate.value,
                sessionId = _currentSessionId.value,
                rpe = rpe?.coerceIn(1, 10)
            )
            repository.insertWorkout(workout)
        }
    }

    fun copyWorkoutsFromYesterday() {
        viewModelScope.launch {
            repository.copyWorkoutsFromPreviousDay(_selectedDate.value, _currentSessionId.value)
        }
    }

    fun deleteWorkout(id: Int) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }

    /**
     * Recent history for the AI Workout Generator: muscle recovery, weekly muscle balance,
     * per-exercise progression/trend, and the user's actual pace (seconds/set, sets/exercise).
     */
    suspend fun getAiGeneratorContext(): AiGeneratorContext {
        val now = System.currentTimeMillis()
        val historyWindowStart = now - 90L * 24 * 60 * 60 * 1000 // 90 days covers recovery, weekly balance, and PR lookups
        val history = repository.getWorkoutsBetweenRange(historyWindowStart, now)

        val recoveryWindowStart = now - 72L * 60 * 60 * 1000
        val muscleRecovery = RecoveryCalculator.calculate(
            history.filter { it.dateInMillis >= recoveryWindowStart }, now
        )

        val weekWindowStart = now - 7L * 24 * 60 * 60 * 1000
        val weeklyMuscleSets = history
            .filter { it.dateInMillis >= weekWindowStart }
            .mapNotNull { ExerciseLibrary.getMuscleFor(it.exerciseName) }
            .groupingBy { it }
            .eachCount()

        val exerciseHistory = history
            .groupBy { it.exerciseName }
            .mapValues { (_, sets) ->
                val sorted = sets.sortedBy { it.dateInMillis }
                val last = sorted.last()
                ExerciseHistoryStats(
                    lastWeight = last.weight,
                    lastReps = last.reps,
                    maxWeight = sets.maxOf { it.weight },
                    lastPerformedMillis = last.dateInMillis,
                    recentSets = sorted.takeLast(6).map { it.weight to it.reps }
                )
            }

        // Learn actual pace from completed sessions in the same window, so time budgeting isn't
        // a one-size-fits-all guess.
        val sessions = repository.getWorkoutSessionsForRange(historyWindowStart, now).first()
        val setsPerSession = history.filter { it.sessionId != null }.groupBy { it.sessionId }
        val completedSessions = sessions.filter { it.durationSeconds > 0 }
        val totalSeconds = completedSessions.sumOf { it.durationSeconds }
        val totalSets = completedSessions.sumOf { setsPerSession[it.sessionId]?.size ?: 0 }
        val avgSecondsPerSet = if (totalSets > 0) totalSeconds.toDouble() / totalSets else null

        val perSessionSetsPerExercise = setsPerSession.values.mapNotNull { sets ->
            val distinctExercises = sets.map { it.exerciseName }.distinct().size
            if (distinctExercises == 0) null else sets.size.toDouble() / distinctExercises
        }
        val avgSetsPerExercise = if (perSessionSetsPerExercise.isNotEmpty()) perSessionSetsPerExercise.average() else null

        return AiGeneratorContext(
            muscleRecovery = muscleRecovery,
            exerciseHistory = exerciseHistory,
            weeklyMuscleSets = weeklyMuscleSets,
            avgSecondsPerSet = avgSecondsPerSet,
            avgSetsPerExercise = avgSetsPerExercise
        )
    }
}

class WorkoutViewModelFactory(private val repository: GlarmToRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
