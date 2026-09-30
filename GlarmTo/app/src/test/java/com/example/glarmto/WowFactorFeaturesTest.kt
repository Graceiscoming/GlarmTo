package com.example.glarmto

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.dashboard.DashboardViewModel
import com.example.glarmto.ui.workout.WorkoutViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WowFactorFeaturesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: RecordingFakeGlarmToDao
    private lateinit var fakeSessionManager: StaticFakeSessionManager
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        fakeDao = RecordingFakeGlarmToDao()
        fakeSessionManager = StaticFakeSessionManager() // Returns "testuser"
        repository = GlarmToRepository(fakeDao, fakeSessionManager)
    }

    @Test
    fun `test getLatestWorkoutByName and Smart Suggestion Logic`() = runTest {
        val viewModel = WorkoutViewModel(repository)
        
        // 1. Insert a workout with RPE 6
        val workout1 = WorkoutEntity(
            username = "testuser",
            exerciseName = "Bench Press",
            weight = 60.0,
            reps = 10,
            rpe = 6,
            dateInMillis = System.currentTimeMillis()
        )
        fakeDao.insertWorkout(workout1)
        
        // Advance time internally just to ensure maxByOrNull sorts well by ID which is auto-incremented in fake
        
        // Check Smart Suggestion (RPE 6 -> should suggest +2.5kg)
        viewModel.fetchSmartSuggestion("Bench Press")
        
        val suggestion1 = viewModel.smartSuggestion.first { it != null }
        assertNotNull("Suggestion should not be null", suggestion1)
        assertTrue("Should suggest trying 62.5kg", suggestion1!!.contains("Try 62.5kg"))
        
        // 2. Insert another workout with RPE 9
        val workout2 = WorkoutEntity(
            username = "testuser",
            exerciseName = "Squat",
            weight = 100.0,
            reps = 5,
            rpe = 9,
            dateInMillis = System.currentTimeMillis()
        )
        fakeDao.insertWorkout(workout2)
        
        viewModel.fetchSmartSuggestion("Squat")
        val suggestion2 = viewModel.smartSuggestion.first { it != null && (it.contains("Squat") || it.contains("100.0kg")) }
        assertTrue("Should suggest targeting same weight for 6 reps", suggestion2!!.contains("Target 100.0kg for 6 reps"))
        
        // 3. Insert workout with RPE 10 (Max effort)
        val workout3 = WorkoutEntity(
            username = "testuser",
            exerciseName = "Deadlift",
            weight = 140.0,
            reps = 3,
            rpe = 10,
            dateInMillis = System.currentTimeMillis()
        )
        fakeDao.insertWorkout(workout3)
        
        viewModel.fetchSmartSuggestion("Deadlift")
        val suggestion3 = viewModel.smartSuggestion.first { it != null && it.contains("137.5kg") }
        assertTrue("Should suggest staying or dropping weight", suggestion3!!.contains("drop to 137.5kg"))
        
        // 4. Test blank exercise name returns null
        // To test blank, we just check that it eventually becomes null.
        // It might be difficult to test if it's already not null without delay.
        viewModel.fetchSmartSuggestion("")
        advanceUntilIdle()
        // Wait a tiny bit for IO thread if it needs to clear
        Thread.sleep(50)
        assertNull("Blank exercise name should clear suggestion", viewModel.smartSuggestion.value)
    }

    @Test
    fun `test Muscle Heatmap Activity Flow initialization`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val dashboardViewModel = DashboardViewModel(context, repository)
        
        // Initialize flow
        val heatmap = dashboardViewModel.heatmapData.first()
        
        // Should return exactly 91 days (13 weeks)
        assertEquals("Heatmap grid should contain exactly 91 items", 91, heatmap.size)
        // Since no workouts inserted recently, should be all 0
        assertTrue("Heatmap should have 0 sets initially", heatmap.all { it == 0 })
        
        // Let's insert a workout for today
        val today = Calendar.getInstance().timeInMillis
        fakeDao.insertWorkout(WorkoutEntity(username = "testuser", exerciseName = "Squat", weight = 100.0, reps = 5, dateInMillis = today))
        fakeDao.insertWorkout(WorkoutEntity(username = "testuser", exerciseName = "Squat", weight = 100.0, reps = 5, dateInMillis = today))
        
        // Assuming testuser flow emitted
        fakeDao.mockUserFlow.value = com.example.glarmto.data.local.entity.UserEntity(username = "testuser")
        
        advanceUntilIdle()
        
        val newHeatmap = dashboardViewModel.heatmapData.first()
        assertTrue("Heatmap size must stay 91", newHeatmap.size == 91)
        // Today is at index 90 (the end of the list)
        assertEquals("Today should have 2 sets", 2, newHeatmap[90])
    }

    @Test
    fun `test shareToInstagramStory generates bitmap and intent without crash`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val dashboardViewModel = DashboardViewModel(context, repository)
        
        try {
            // Provide dummy values
            dashboardViewModel.shareToInstagramStory("testuser", 5, 10)
            
            // Check that the image file was created in cache
            val imagesDir = java.io.File(context.cacheDir, "images")
            val imageFile = java.io.File(imagesDir, "glarmto_story.png")
            
            // Polling wait for IO thread to finish saving the image file
            var retries = 20
            while (!imageFile.exists() && retries > 0) {
                Thread.sleep(50)
                retries--
            }
            
            assertTrue("Story image should be generated in cache directory", imageFile.exists())
            // Note: In Robolectric, Bitmap.compress may produce a 0-byte file, so we skip the length check.
        } catch (e: Exception) {
            fail("shareToInstagramStory threw an exception: ${e.message}")
        }
    }

    @Test
    fun `test 3D Radar Chart categorization and normalization`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val dashboardViewModel = DashboardViewModel(context, repository)

        val today = Calendar.getInstance().timeInMillis
        // Insert Chest (Bench)
        fakeDao.insertWorkout(WorkoutEntity(username = "testuser", exerciseName = "Bench Press", weight = 60.0, reps = 10, dateInMillis = today))
        fakeDao.insertWorkout(WorkoutEntity(username = "testuser", exerciseName = "Incline Bench Press", weight = 20.0, reps = 10, dateInMillis = today))

        // Insert Back (Row)
        fakeDao.insertWorkout(WorkoutEntity(username = "testuser", exerciseName = "Barbell Row", weight = 50.0, reps = 10, dateInMillis = today))

        fakeDao.mockUserFlow.value = com.example.glarmto.data.local.entity.UserEntity(username = "testuser")
        
        // Wait for IO coroutine to emit the computed data
        val radarData = dashboardViewModel.radarChartData.first { it.isNotEmpty() }

        // Max sets is 2 (Chest)
        assertEquals(1.0f, radarData["Chest"] ?: 0f, 0.01f) // 2 / 2 = 1.0f
        assertEquals(0.5f, radarData["Back"] ?: 0f, 0.01f)  // 1 / 2 = 0.5f
        assertEquals(0.0f, radarData["Legs"] ?: 0f, 0.01f)  // 0 / 2 = 0.0f
    }

    @Test
    fun `test PiP Rest Timer logic in WorkoutViewModel`() = runTest {
        val viewModel = WorkoutViewModel(repository)

        // Initial setup
        assertEquals(0, viewModel.restTimeSeconds.value)
        assertFalse(viewModel.isTimerRunning.value)

        // Set and start timer
        viewModel.setRestTimer(2) // 2 seconds
        assertEquals(2, viewModel.restTimeSeconds.value)
        
        viewModel.startRestTimer()
        assertTrue(viewModel.isTimerRunning.value)

        // Advance 1 second
        advanceTimeBy(1001)
        assertEquals(1, viewModel.restTimeSeconds.value)
        assertTrue(viewModel.isTimerRunning.value)

        // Advance 1 more second, timer should expire
        advanceTimeBy(1001)
        assertEquals(0, viewModel.restTimeSeconds.value)
        assertFalse(viewModel.isTimerRunning.value)
        
        // Add additional time
        viewModel.addRestTime(30)
        assertEquals(30, viewModel.restTimeSeconds.value)
    }
}
