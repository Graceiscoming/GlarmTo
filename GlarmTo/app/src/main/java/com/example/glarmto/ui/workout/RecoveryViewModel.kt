package com.example.glarmto.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.RecoveryCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MuscleRecovery(
    val muscleGroup: MuscleGroup,
    val recoveryPercentage: Float // 0.0 to 1.0 (1.0 = 100% recovered)
)

class RecoveryViewModel(private val repository: GlarmToRepository) : ViewModel() {
    private val _recoveryStatus = MutableStateFlow<List<MuscleRecovery>>(emptyList())
    val recoveryStatus: StateFlow<List<MuscleRecovery>> = _recoveryStatus

    private val _smartRecommendation = MutableStateFlow<String>("Analyzing your history...")
    val smartRecommendation: StateFlow<String> = _smartRecommendation

    init {
        fetchAndCalculateRecovery()
    }

    fun fetchAndCalculateRecovery() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val windowMs = 72 * 60 * 60 * 1000L // 72 hours
                val startWindow = now - windowMs

                // 1. Fetch Workouts
                val recentWorkouts = repository.getWorkoutsBetweenRange(startWindow, now)

                // 2. Calculate current recovery (shared with WorkoutGenerator's recovery-aware picks)
                val recoveryMap = RecoveryCalculator.calculate(recentWorkouts, now)
                val recoveryList = MuscleGroup.values().map { muscle ->
                    MuscleRecovery(muscle, recoveryMap.getValue(muscle))
                }

                _recoveryStatus.value = recoveryList

                // 4. Smart Recommendation
                val fullyRecovered = recoveryList.filter { it.recoveryPercentage > 0.9f }.map { it.muscleGroup }
                val exhausted = recoveryList.filter { it.recoveryPercentage < 0.5f }.map { it.muscleGroup }

                if (exhausted.isNotEmpty()) {
                    val recStr = if (fullyRecovered.isNotEmpty()) {
                        "Your ${exhausted.joinToString(", ") { it.name }} are exhausted. Focus on ${fullyRecovered.random().name} today!"
                    } else {
                        "You've been working hard! Everything needs a rest. Take a rest day 🧘‍♂️"
                    }
                    _smartRecommendation.value = recStr
                } else {
                    _smartRecommendation.value = "You are fully recovered! Go crush any workout today 💪"
                }
            }
        }
    }
}

class RecoveryViewModelFactory(private val repository: GlarmToRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecoveryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecoveryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
