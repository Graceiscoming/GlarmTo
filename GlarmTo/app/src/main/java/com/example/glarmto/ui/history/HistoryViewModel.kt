package com.example.glarmto.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.CalendarDayUtils
import com.example.glarmto.data.util.TodayTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryViewModel(
    application: Application,
    private val repository: GlarmToRepository,
    todayProvider: () -> Long = CalendarDayUtils::localTodayStartMillis
) : AndroidViewModel(application) {

    // "Today" must follow the calendar, not the moment this ViewModel was created.
    private val dayTracker = TodayTracker(todayProvider)

    private val _selectedDate = MutableStateFlow(dayTracker.today.value)
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _isMonthlyView = MutableStateFlow(false)
    val isMonthlyView: StateFlow<Boolean> = _isMonthlyView.asStateFlow()

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val workouts: StateFlow<List<WorkoutEntity>> = combine(_selectedDate, _isMonthlyView) { date, isMonthly -> Pair(date, isMonthly) }
        .flatMapLatest { (date, isMonthly) ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val (start, end) = if (isMonthly) repository.getMonthRange(cal) else repository.getDayRange(cal)
            repository.getWorkoutsForRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val nutrition: StateFlow<List<NutritionEntity>> = combine(_selectedDate, _isMonthlyView) { date, isMonthly -> Pair(date, isMonthly) }
        .flatMapLatest { (date, isMonthly) ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val (start, end) = if (isMonthly) repository.getMonthRange(cal) else repository.getDayRange(cal)
            repository.getNutritionForRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val sessions: StateFlow<List<com.example.glarmto.data.local.entity.WorkoutSessionEntity>> = combine(_selectedDate, _isMonthlyView) { date, isMonthly -> Pair(date, isMonthly) }
        .flatMapLatest { (date, isMonthly) ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val (start, end) = if (isMonthly) repository.getMonthRange(cal) else repository.getDayRange(cal)
            repository.getWorkoutSessionsForRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Call when the screen is shown again. If the day rolled over and the user was looking at "today",
     * the selected date moves to the new day; a day they picked themselves is kept.
     */
    fun refreshToday() {
        dayTracker.refreshAndFollow(_selectedDate)
    }

    fun setViewMode(isMonthly: Boolean) {
        _isMonthlyView.value = isMonthly
    }

    fun deleteWorkout(id: Int) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = CalendarDayUtils.localDayStartFromMaterialPickerUtc(date)
    }
}

class HistoryViewModelFactory(
    private val application: Application,
    private val repository: GlarmToRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
