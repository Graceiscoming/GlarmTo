package com.example.glarmto

import android.app.Application
import com.example.glarmto.testsupport.MainDispatcherRule
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.calculator.CalculatorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CalculatorViewModelRobolectricTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var app: Application
    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    @Test
    fun `updateProfile writes TDEE and rest seconds`() = runBlocking {
        val vm = CalculatorViewModel(app, repository)
        val job = launch { vm.currentUser.collect {} }
        delay(120)
        vm.updateProfile(
            30,
            80.0,
            180.0,
            isMale = true,
            restSeconds = 120,
            workoutDays = 4,
            goal = "Maintain",
            macroProteinPct = 30,
            macroCarbPct = 40,
            macroFatPct = 30,
            dailyWaterGoalMl = 2000
        )
        delay(350)

        val u = dao.lastUpdatedUser
        assertTrue(u != null && u.dailyGoal > 0)
        assertEquals(120, u!!.defaultRestSeconds)
        assertEquals(4, u.workoutDays)
        assertEquals("Maintain", u.goal)
        job.cancel()
    }
}
