package com.example.glarmto.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glarmto.data.local.entity.RoutineEntity
import com.example.glarmto.data.repository.GlarmToRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineViewModel(private val repository: GlarmToRepository) : ViewModel() {

    val routines: StateFlow<List<RoutineEntity>> = repository.getRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRoutine(name: String, exercisesList: List<String>) {
        if (name.isBlank() || exercisesList.isEmpty()) return
        viewModelScope.launch {
            val routine = RoutineEntity(
                username = repository.getCurrentUser() ?: "admin",
                routineName = name,
                exercises = exercisesList.joinToString(separator = "|")
            )
            repository.insertRoutine(routine)
        }
    }

    fun deleteRoutine(id: Int) {
        viewModelScope.launch {
            repository.deleteRoutine(id)
        }
    }
}

class RoutineViewModelFactory(private val repository: GlarmToRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
