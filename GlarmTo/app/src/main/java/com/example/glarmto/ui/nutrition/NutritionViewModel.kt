package com.example.glarmto.ui.nutrition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.CalendarDayUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class NutritionViewModel(
    application: Application,
    private val repository: GlarmToRepository
) : AndroidViewModel(application) {

    val dailyGoal: StateFlow<Int> = repository.getUserFlow()
        .map { it?.dailyGoal ?: 2500 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2500)

    val userFlow: StateFlow<com.example.glarmto.data.local.entity.UserEntity?> = repository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(CalendarDayUtils.localTodayStartMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val waterEntries: StateFlow<List<WaterEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getWaterForDay(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val nutritionList: StateFlow<List<NutritionEntity>> = _selectedDate
        .flatMapLatest { date ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val (start, end) = repository.getDayRange(cal)
            repository.getNutritionForRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDateFromMaterialPicker(utcPickerMillis: Long) {
        _selectedDate.value = CalendarDayUtils.localDayStartFromMaterialPickerUtc(utcPickerMillis)
    }

    fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            val user = repository.getUserFlow().firstOrNull()
            if (user != null) {
                repository.updateUser(user.copy(dailyGoal = goal))
            }
        }
    }

    fun addNutrition(foodName: String, calories: Int) {
        viewModelScope.launch {
            val nutrition = NutritionEntity(
                foodName = foodName,
                calories = calories,
                dateInMillis = _selectedDate.value // Save to selected date
            )
            repository.insertNutrition(nutrition)
        }
    }

    fun deleteNutrition(id: Int) {
        viewModelScope.launch {
            repository.deleteNutrition(id)
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.insertWater(amountMl, _selectedDate.value)
        }
    }

    fun deleteWater(id: Int) {
        viewModelScope.launch {
            repository.deleteWater(id)
        }
    }

    fun copyMealsFromYesterday() {
        viewModelScope.launch {
            repository.copyNutritionFromPreviousDay(_selectedDate.value)
        }
    }
}

class NutritionViewModelFactory(
    private val application: Application,
    private val repository: GlarmToRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NutritionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NutritionViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
