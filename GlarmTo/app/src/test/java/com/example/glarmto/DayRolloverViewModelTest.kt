package com.example.glarmto

import com.example.glarmto.testsupport.TestTexts
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.local.entity.WorkoutSessionEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.dashboard.DashboardViewModel
import com.example.glarmto.ui.history.HistoryViewModel
import com.example.glarmto.ui.nutrition.NutritionViewModel
import com.example.glarmto.ui.workout.WorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * An app left open past midnight must move to the new day instead of showing and saving to the day
 * the ViewModel was created on. "Now" is injected so each test can roll the day over on demand.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DayRolloverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var app: Application
    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository

    private fun dayStart(dayOfMonth: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.MARCH, dayOfMonth)
    }.timeInMillis

    private val monday = dayStart(9)
    private val tuesday = dayStart(10)
    private val wednesday = dayStart(11)
    private val lastWeek = dayStart(2)
    private val noon = 12L * 60 * 60 * 1000

    /** The fake "current day" the ViewModels read. */
    private var now = monday

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
        now = monday
    }

    private fun workout(name: String, day: Long) = WorkoutEntity(
        exerciseName = name, weight = 50.0, reps = 5, dateInMillis = day + noon, username = "testuser"
    )

    private fun meal(name: String, day: Long) = NutritionEntity(
        foodName = name, calories = 100, dateInMillis = day + noon, username = "testuser"
    )

    // ---------------- Dashboard ----------------

    @Test
    fun `dashboard today workouts follow the day after refreshToday`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedWorkouts.add(workout("MondaySet", monday).copy(id = 1))
        dao.insertedWorkouts.add(workout("TuesdaySet", tuesday).copy(id = 2))
        val vm = DashboardViewModel(app, repository) { now }
        val job = launch { vm.todayWorkouts.collect { } }

        assertEquals(listOf("MondaySet"), vm.todayWorkouts.value.map { it.exerciseName })

        now = tuesday
        vm.refreshToday()

        assertEquals(listOf("TuesdaySet"), vm.todayWorkouts.value.map { it.exerciseName })
        job.cancel()
    }

    @Test
    fun `dashboard without refreshToday keeps showing the old day`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedWorkouts.add(workout("MondaySet", monday).copy(id = 1))
        dao.insertedWorkouts.add(workout("TuesdaySet", tuesday).copy(id = 2))
        val vm = DashboardViewModel(app, repository) { now }
        val job = launch { vm.todayWorkouts.collect { } }

        now = tuesday // no refresh: documents that the screen is what triggers the update

        assertEquals(listOf("MondaySet"), vm.todayWorkouts.value.map { it.exerciseName })
        job.cancel()
    }

    @Test
    fun `dashboard nutrition water and sessions all follow the day`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedNutrition.add(meal("Monday rice", monday).copy(id = 1))
        dao.insertedNutrition.add(meal("Tuesday egg", tuesday).copy(id = 2))
        dao.insertedWater.add(WaterEntity(id = 1, username = "testuser", dateInMillis = monday + noon, amountMl = 250))
        dao.insertedWater.add(WaterEntity(id = 2, username = "testuser", dateInMillis = tuesday + noon, amountMl = 500))
        dao.insertedSessions.add(WorkoutSessionEntity(sessionId = 1, startTimeInMillis = monday + noon, dateInMillis = monday + noon, username = "testuser", sessionName = "Mon"))
        dao.insertedSessions.add(WorkoutSessionEntity(sessionId = 2, startTimeInMillis = tuesday + noon, dateInMillis = tuesday + noon, username = "testuser", sessionName = "Tue"))
        val vm = DashboardViewModel(app, repository) { now }
        val jobs = listOf(
            launch { vm.todayNutrition.collect { } },
            launch { vm.todayWaterMl.collect { } },
            launch { vm.todaySessions.collect { } }
        )

        assertEquals(listOf("Monday rice"), vm.todayNutrition.value.map { it.foodName })
        assertEquals(250, vm.todayWaterMl.value)
        assertEquals(listOf("Mon"), vm.todaySessions.value.map { it.sessionName })

        now = tuesday
        vm.refreshToday()

        assertEquals(listOf("Tuesday egg"), vm.todayNutrition.value.map { it.foodName })
        assertEquals(500, vm.todayWaterMl.value)
        assertEquals(listOf("Tue"), vm.todaySessions.value.map { it.sessionName })
        jobs.forEach { it.cancel() }
    }

    @Test
    fun `dashboard refreshToday on the same day changes nothing`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedWorkouts.add(workout("MondaySet", monday).copy(id = 1))
        val vm = DashboardViewModel(app, repository) { now }
        val job = launch { vm.todayWorkouts.collect { } }

        vm.refreshToday()
        vm.refreshToday()

        assertEquals(listOf("MondaySet"), vm.todayWorkouts.value.map { it.exerciseName })
        job.cancel()
    }

    @Test
    fun `dashboard can roll over several days in one refresh`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedWorkouts.add(workout("WednesdaySet", wednesday).copy(id = 1))
        val vm = DashboardViewModel(app, repository) { now }
        val job = launch { vm.todayWorkouts.collect { } }

        now = wednesday
        vm.refreshToday()

        assertEquals(listOf("WednesdaySet"), vm.todayWorkouts.value.map { it.exerciseName })
        job.cancel()
    }

    // ---------------- Workout ----------------

    @Test
    fun `workout screen moves to the new day when it was on today`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedWorkouts.add(workout("MondaySet", monday).copy(id = 1))
        dao.insertedWorkouts.add(workout("TuesdaySet", tuesday).copy(id = 2))
        val vm = WorkoutViewModel(repository, TestTexts.english) { now }
        val job = launch { vm.workouts.collect { } }
        assertEquals(monday, vm.selectedDate.value)
        assertEquals(listOf("MondaySet"), vm.workouts.value.map { it.exerciseName })

        now = tuesday
        vm.refreshToday()

        assertEquals(tuesday, vm.selectedDate.value)
        assertEquals(listOf("TuesdaySet"), vm.workouts.value.map { it.exerciseName })
        job.cancel()
    }

    @Test
    fun `workout screen keeps a day the user picked`() = runTest(UnconfinedTestDispatcher()) {
        val vm = WorkoutViewModel(repository, TestTexts.english) { now }
        vm.setSelectedDateFromLocalInstant(lastWeek + noon)

        now = tuesday
        vm.refreshToday()

        assertEquals(lastWeek, vm.selectedDate.value)
    }

    @Test
    fun `sets added after midnight are saved to the new day`() = runTest(UnconfinedTestDispatcher()) {
        val vm = WorkoutViewModel(repository, TestTexts.english) { now }
        now = tuesday
        vm.refreshToday()

        vm.addWorkout("Squat", 100.0, 5)
        awaitReal { dao.insertedWorkouts.isNotEmpty() }

        assertEquals(tuesday, dao.insertedWorkouts.single().dateInMillis)
    }

    @Test
    fun `workout today nutrition follows the day`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedNutrition.add(meal("Monday rice", monday).copy(id = 1))
        dao.insertedNutrition.add(meal("Tuesday egg", tuesday).copy(id = 2))
        val vm = WorkoutViewModel(repository, TestTexts.english) { now }
        val job = launch { vm.todayNutrition.collect { } }
        assertEquals(listOf("Monday rice"), vm.todayNutrition.value.map { it.foodName })

        now = tuesday
        vm.refreshToday()

        assertEquals(listOf("Tuesday egg"), vm.todayNutrition.value.map { it.foodName })
        job.cancel()
    }

    // ---------------- Nutrition ----------------

    @Test
    fun `nutrition screen moves to the new day and exposes it`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedNutrition.add(meal("Monday rice", monday).copy(id = 1))
        dao.insertedNutrition.add(meal("Tuesday egg", tuesday).copy(id = 2))
        val vm = NutritionViewModel(app, repository) { now }
        val job = launch { vm.nutritionList.collect { } }
        assertEquals(monday, vm.today.value)
        assertEquals(listOf("Monday rice"), vm.nutritionList.value.map { it.foodName })

        now = tuesday
        vm.refreshToday()

        assertEquals(tuesday, vm.today.value)
        assertEquals(tuesday, vm.selectedDate.value)
        assertEquals(listOf("Tuesday egg"), vm.nutritionList.value.map { it.foodName })
        job.cancel()
    }

    @Test
    fun `nutrition screen keeps a day the user picked but still updates today`() = runTest(UnconfinedTestDispatcher()) {
        val vm = NutritionViewModel(app, repository) { now }
        vm.setSelectedDateFromMaterialPicker(utcMidnight(2026, Calendar.MARCH, 10))

        now = wednesday
        vm.refreshToday()

        assertEquals("picked Tuesday stays", tuesday, vm.selectedDate.value)
        assertEquals("but today moved on", wednesday, vm.today.value)
    }

    @Test
    fun `food logged after midnight is saved to the new day`() = runTest(UnconfinedTestDispatcher()) {
        val vm = NutritionViewModel(app, repository) { now }
        now = tuesday
        vm.refreshToday()

        vm.addNutrition("Toast", 120)
        awaitReal { dao.insertedNutrition.isNotEmpty() }

        assertEquals(tuesday, dao.insertedNutrition.single().dateInMillis)
    }

    // ---------------- History ----------------

    @Test
    fun `history moves to the new day when it was on today`() = runTest(UnconfinedTestDispatcher()) {
        dao.insertedWorkouts.add(workout("MondaySet", monday).copy(id = 1))
        dao.insertedWorkouts.add(workout("TuesdaySet", tuesday).copy(id = 2))
        val vm = HistoryViewModel(app, repository) { now }
        val job = launch { vm.workouts.collect { } }
        assertEquals(listOf("MondaySet"), vm.workouts.value.map { it.exerciseName })

        now = tuesday
        vm.refreshToday()

        assertEquals(tuesday, vm.selectedDate.value)
        assertEquals(listOf("TuesdaySet"), vm.workouts.value.map { it.exerciseName })
        job.cancel()
    }

    @Test
    fun `history keeps a day the user picked`() = runTest(UnconfinedTestDispatcher()) {
        val vm = HistoryViewModel(app, repository) { now }
        vm.setSelectedDate(utcMidnight(2026, Calendar.MARCH, 2))

        now = tuesday
        vm.refreshToday()

        assertEquals(lastWeek, vm.selectedDate.value)
    }

    /** The repository writes on Dispatchers.IO (a real thread), so wait in real time, not virtual test time. */
    private suspend fun awaitReal(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue("timed out waiting for the write", condition())
    }

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day)
        }.timeInMillis
}
