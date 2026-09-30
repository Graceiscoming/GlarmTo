package com.example.glarmto

import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Tests for export, period stats, streak, water, and copy-yesterday helpers on [GlarmToRepository].
 */
class GlarmToRepositoryFeaturesTest {

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    @Test
    fun `getPeriodTrainingStats sums volume and sets in window`() = runBlocking {
        val todayStart = repository.getDayRange(Calendar.getInstance()).first
        dao.insertedWorkouts.add(
            WorkoutEntity(1, "A", 10.0, 5, todayStart, "testuser", null, null)
        )
        dao.insertedWorkouts.add(
            WorkoutEntity(2, "B", 2.0, 3, todayStart + 1, "testuser", null, null)
        )
        val stats = repository.getPeriodTrainingStats(7)
        assertEquals(10.0 * 5 + 2.0 * 3, stats.totalVolume, 0.01)
        assertEquals(2, stats.totalSets)
    }

    @Test
    fun `getTrainingStreakDays is one when only today has sets`() = runBlocking {
        val todayStart = repository.getDayRange(Calendar.getInstance()).first
        dao.insertedWorkouts.add(
            WorkoutEntity(1, "A", 1.0, 1, todayStart, "testuser", null, null)
        )
        assertEquals(1, repository.getTrainingStreakDays())
    }

    @Test
    fun `exportUserDataJson includes exportVersion and username`() = runBlocking {
        val json = repository.exportUserDataJson()
        assertTrue(json.contains("\"exportVersion\""))
        assertTrue(json.contains("testuser"))
    }

    @Test
    fun `exportUserDataCsv contains GlarmTo header`() = runBlocking {
        val csv = repository.exportUserDataCsv()
        assertTrue(csv.contains("# GlarmTo export"))
    }

    @Test
    fun `insertWater stores entry for current user`() = runBlocking {
        val day = repository.getDayRange(Calendar.getInstance()).first
        repository.insertWater(250, day)
        assertEquals(1, dao.insertedWater.size)
        assertEquals(250, dao.insertedWater.single().amountMl)
        assertEquals("testuser", dao.insertedWater.single().username)
    }

    @Test
    fun `getWaterForDay returns flow for user`() = runBlocking {
        val day = repository.getDayRange(Calendar.getInstance()).first
        dao.insertedWater.add(WaterEntity(1, "testuser", day, 100))
        val list = repository.getWaterForDay(day).first()
        assertEquals(1, list.size)
        assertEquals(100, list.single().amountMl)
    }

    @Test
    fun `copyWorkoutsFromPreviousDay duplicates into target day`() = runBlocking {
        val todayStart = repository.getDayRange(Calendar.getInstance()).first
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayStart = repository.getDayRange(prevCal).first
        dao.insertedWorkouts.add(
            WorkoutEntity(1, "Squat", 100.0, 5, yesterdayStart, "testuser", null, 9)
        )
        repository.copyWorkoutsFromPreviousDay(todayStart, sessionId = null)
        assertEquals(2, dao.insertedWorkouts.size)
        val copy = dao.insertedWorkouts.last()
        assertEquals("Squat", copy.exerciseName)
        assertEquals(todayStart, copy.dateInMillis)
        assertEquals(null, copy.sessionId)
    }

    @Test
    fun `copyNutritionFromPreviousDay duplicates meals`() = runBlocking {
        val todayStart = repository.getDayRange(Calendar.getInstance()).first
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayStart = repository.getDayRange(prevCal).first
        dao.insertedNutrition.add(
            NutritionEntity(1, "Oats", 320, yesterdayStart, "testuser")
        )
        repository.copyNutritionFromPreviousDay(todayStart)
        assertEquals(2, dao.insertedNutrition.size)
        assertEquals("Oats", dao.insertedNutrition.last().foodName)
        assertEquals(todayStart, dao.insertedNutrition.last().dateInMillis)
    }
}
