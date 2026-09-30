package com.example.glarmto.ui.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.HealthCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(
    application: Application,
    private val repository: GlarmToRepository
) : AndroidViewModel(application) {

    val currentUser: StateFlow<UserEntity?> = repository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(
        age: Int,
        weight: Double,
        height: Double,
        isMale: Boolean,
        restSeconds: Int,
        workoutDays: Int,
        goal: String,
        macroProteinPct: Int,
        macroCarbPct: Int,
        macroFatPct: Int,
        dailyWaterGoalMl: Int
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                val tdeeResult = HealthCalculator.calculateTdee(age, weight, height, isMale, workoutDays, goal)

                repository.updateUser(
                    user.copy(
                        age = age,
                        weight = weight,
                        height = height,
                        isMale = isMale,
                        dailyGoal = tdeeResult,
                        defaultRestSeconds = restSeconds,
                        workoutDays = workoutDays,
                        goal = goal,
                        macroProteinPct = macroProteinPct.coerceIn(0, 100),
                        macroCarbPct = macroCarbPct.coerceIn(0, 100),
                        macroFatPct = macroFatPct.coerceIn(0, 100),
                        dailyWaterGoalMl = dailyWaterGoalMl.coerceIn(500, 10000)
                    )
                )
            }
        }
    }
}

class CalculatorViewModelFactory(
    private val application: Application,
    private val repository: GlarmToRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
