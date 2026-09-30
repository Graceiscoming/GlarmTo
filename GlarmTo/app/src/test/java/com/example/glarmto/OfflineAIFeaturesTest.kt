package com.example.glarmto

import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.Equipment
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.WorkoutGenerator
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.workout.RecoveryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OfflineAIFeaturesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testWorkoutGenerator_timeConstraints() {
        // Given 30 mins, constraint = Dumbbell, focus = Chest
        val result = WorkoutGenerator.generateWorkout(
            availableTimeMins = 30,
            equipmentConstraints = listOf(Equipment.Dumbbell),
            focusMuscles = listOf(MuscleGroup.Chest)
        )

        // Then expected number of exercises should be MIN of the requested target (5) and the available items in DB matching constraints.
        // There might only be 4 exercises matching Chest + Dumbbell/Bodyweight in the library!
        assertTrue("Should have 4 or 5 exercises based on constraints", result.exercises.size in 4..5)
        
        // Assert totalTimeMins field
        assertEquals(30, result.totalTimeMins)
        
        // Sets should be around 3
        assertTrue("Expected ~3 sets", result.exercises.first().targetSets in 2..4)
    }

    @Test
    fun testWorkoutGenerator_equipmentConstraints() {
        // Given only specific equipment
        val result = WorkoutGenerator.generateWorkout(
            availableTimeMins = 45,
            equipmentConstraints = listOf(Equipment.Barbell),
            focusMuscles = listOf(MuscleGroup.Legs)
        )
        
        // Ensure that selected exercises only require Barbell OR Bodyweight (which is allowed)
        val validEquipments = listOf(Equipment.Barbell, Equipment.Bodyweight)
        
        for (ex in result.exercises) {
            val generatedDef = com.example.glarmto.data.util.ExerciseLibrary.exercises.find { it.name == ex.name }
            assertTrue(
                "Exercise ${ex.name} requires ${generatedDef?.equipment} but you only have Barbell",
                validEquipments.contains(generatedDef?.equipment)
            )
        }
    }

    @Test
    fun testRecoveryViewModel_fatigueMath() = runTest {
        val dao = RecordingFakeGlarmToDao()
        val sessionManager = StaticFakeSessionManager()
        val repo = GlarmToRepository(dao, sessionManager)

        val now = System.currentTimeMillis()
        
        // Let's log some Sets for "Bench Press" (Chest). 1 set = 15% damage. 2 sets = 30% damage.
        // We log them RIGHT NOW, so recovery should be 1.0 - 0.3 = 0.7 (70% recovered)
        dao.insertWorkout(WorkoutEntity(1, "Bench Press", weight = 50.0, reps = 10, dateInMillis = now, username = "testuser"))
        dao.insertWorkout(WorkoutEntity(2, "Bench Press", weight = 50.0, reps = 10, dateInMillis = now, username = "testuser"))

        val viewModel = RecoveryViewModel(repo)
        
        // Wait for IO coroutine to emit by using flow.first()
        val recoveryList = viewModel.recoveryStatus.first { it.isNotEmpty() }
        val chestRecovery = recoveryList.find { it.muscleGroup == MuscleGroup.Chest }
        
        assertTrue("Chest recovery should not be null", chestRecovery != null)
        
        // Math check:
        // Damage = 2 sets * 0.15 = 0.3
        // Time elapsed = 0 -> Recovery Progress = 0
        // Expected Current Recovery = 1.0f - 0.3f = 0.7f
        val expectedRecovery = 0.7f
        
        // Allowing a tiny epsilon for float precision
        val isCloseEnough = abs(expectedRecovery - chestRecovery!!.recoveryPercentage) < 0.001f
        assertTrue("Expected ~0.7f recovery but got ${chestRecovery.recoveryPercentage}", isCloseEnough)
    }
    
    @Test
    fun testRecoveryViewModel_timeDecay() = runTest {
        val dao = RecordingFakeGlarmToDao()
        val sessionManager = StaticFakeSessionManager()
        val repo = GlarmToRepository(dao, sessionManager)

        val now = System.currentTimeMillis()
        // Log "Squat" (Legs) exactly 24 hours ago.
        val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000L)
        
        // 4 sets of Squat = 4 * 0.15 = 0.6 damage (60% damage) -> 40% immediately.
        for (i in 1..4) {
            dao.insertWorkout(WorkoutEntity(i, "Squat", weight = 100.0, reps = 8, dateInMillis = twentyFourHoursAgo, username = "testuser"))
        }

        val viewModel = RecoveryViewModel(repo)
        
        // Wait for IO coroutine to emit by using flow.first()
        val recoveryList = viewModel.recoveryStatus.first { it.isNotEmpty() }
        val legRecovery = recoveryList.find { it.muscleGroup == MuscleGroup.Legs }
        
        // Math check:
        // Initial Damage = 0.6f
        // Time elapsed = 24 hours = 50% of 48 hours -> Recovery Progress = 0.5f
        // Remaining Damage = max(0, 0.6 - 0.5) = 0.1f
        // Expected Recovery = 1.0 - 0.1 = 0.9f
        val expectedRecovery = 0.9f
        
        val isCloseEnough = abs(expectedRecovery - legRecovery!!.recoveryPercentage) < 0.001f
        assertTrue("Expected ~0.9f recovery after 24 hrs but got ${legRecovery.recoveryPercentage}", isCloseEnough)
    }
}
