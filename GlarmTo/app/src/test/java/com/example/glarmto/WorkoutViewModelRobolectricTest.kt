package com.example.glarmto

import android.app.Application
import com.example.glarmto.testsupport.MainDispatcherRule
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.workout.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WorkoutViewModelRobolectricTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    @Test
    fun `startWorkout creates session and enables stopwatch`() = runBlocking {
        val vm = WorkoutViewModel(repository)
        delay(30)
        vm.startWorkout()
        delay(200)

        assertTrue(vm.isWorkingOut.value)
        assertNotNull(vm.currentSessionId.value)
        assertEquals(1, dao.insertedSessions.size)
    }

    @Test
    fun `addWorkout links current session id`() = runBlocking {
        val vm = WorkoutViewModel(repository)
        delay(30)
        vm.startWorkout()
        delay(150)
        val sid = vm.currentSessionId.value!!
        vm.addWorkout("Squat", 100.0, 5)
        delay(200)

        val w = dao.insertedWorkouts.last()
        assertEquals(sid, w.sessionId)
        assertEquals("Squat", w.exerciseName)
    }
}
