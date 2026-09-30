package com.example.glarmto

import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * XP is tracked per entry (`xpAwarded`), so deleting an entry takes back exactly what it earned.
 */
class XpLedgerTest {

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repo: GlarmToRepository

    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun setUser(xp: Long = 0, daily: Long = 0, lastXpDate: Long = today, level: Int = 1) {
        dao.mockUserFlow.value = UserEntity(
            username = "testuser", xp = xp, dailyXPEarned = daily, lastXPDate = lastXpDate, level = level
        )
    }

    private fun set(name: String = "Squat", id: Int = 0, xpAwarded: Int = 0) = WorkoutEntity(
        id = id, exerciseName = name, weight = 50.0, reps = 5, dateInMillis = today, xpAwarded = xpAwarded
    )

    private val user get() = dao.mockUserFlow.value!!

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        repo = GlarmToRepository(dao, StaticFakeSessionManager())
        setUser()
    }

    // ---- workouts ----

    @Test
    fun `a new set records the xp it earned`() = runBlocking {
        repo.insertWorkout(set())

        assertEquals(10, dao.insertedWorkouts.single().xpAwarded)
        assertEquals(10L, user.xp)
    }

    @Test
    fun `a set added without xp records zero and earns nothing`() = runBlocking {
        repo.insertWorkout(set(), awardXp = false)

        assertEquals(0, dao.insertedWorkouts.single().xpAwarded)
        assertEquals(0L, user.xp)
    }

    @Test
    fun `caller cannot forge xpAwarded on insert`() = runBlocking {
        setUser(xp = 0, daily = 300)
        repo.insertWorkout(set(xpAwarded = 999))

        assertEquals("the cap is reached so the set earned nothing", 0, dao.insertedWorkouts.single().xpAwarded)
    }

    @Test
    fun `deleting a set that earned xp takes exactly that back`() = runBlocking {
        repo.insertWorkout(set())
        repo.deleteWorkout(dao.insertedWorkouts.single().id)

        assertEquals(0L, user.xp)
        assertEquals(0L, user.dailyXPEarned)
        assertTrue(dao.insertedWorkouts.isEmpty())
    }

    @Test
    fun `deleting a set added past the daily cap takes nothing back`() = runBlocking {
        setUser(xp = 1000, daily = 300)
        repo.insertWorkout(set()) // earns 0: cap already reached
        val id = dao.insertedWorkouts.single().id

        repo.deleteWorkout(id)

        assertEquals("xp must be unchanged", 1000L, user.xp)
        assertEquals(300L, user.dailyXPEarned)
    }

    @Test
    fun `deleting a set that only partly earned xp takes back only the part`() = runBlocking {
        setUser(xp = 1000, daily = 295)
        repo.insertWorkout(set()) // only 5 of 10 fit under the cap
        val stored = dao.insertedWorkouts.single()
        assertEquals(5, stored.xpAwarded)
        assertEquals(1005L, user.xp)

        repo.deleteWorkout(stored.id)

        assertEquals(1000L, user.xp)
        assertEquals(295L, user.dailyXPEarned)
    }

    @Test
    fun `sets past the cap cannot drain xp when deleted - the reported bug`() = runBlocking {
        // 35 sets in one day: only the first 30 earn xp (cap 300).
        repeat(35) { repo.insertWorkout(set()) }
        assertEquals(300L, user.xp)
        assertEquals(30, dao.insertedWorkouts.count { it.xpAwarded == 10 })
        assertEquals(5, dao.insertedWorkouts.count { it.xpAwarded == 0 })

        // Delete the 5 sets that earned nothing.
        dao.insertedWorkouts.filter { it.xpAwarded == 0 }.map { it.id }.forEach { repo.deleteWorkout(it) }

        assertEquals("deleting no-xp sets must not cost xp", 300L, user.xp)
    }

    @Test
    fun `a legacy set with the migrated default of 10 still revokes 10`() = runBlocking {
        setUser(xp = 100, daily = 50)
        dao.insertedWorkouts.add(set(id = 1, xpAwarded = 10).copy(username = "testuser"))

        repo.deleteWorkout(1)

        assertEquals(90L, user.xp)
        assertEquals(40L, user.dailyXPEarned)
    }

    @Test
    fun `deleting a set that does not exist changes nothing`() = runBlocking {
        setUser(xp = 100, daily = 50)

        repo.deleteWorkout(12345)

        assertEquals(100L, user.xp)
        assertEquals(50L, user.dailyXPEarned)
    }

    @Test
    fun `xp never goes below zero when revoking more than the user has`() = runBlocking {
        setUser(xp = 4, daily = 4)
        dao.insertedWorkouts.add(set(id = 1, xpAwarded = 10).copy(username = "testuser"))

        repo.deleteWorkout(1)

        assertEquals(0L, user.xp)
        assertEquals(0L, user.dailyXPEarned)
    }

    @Test
    fun `copying yesterdays sets earns no xp and deleting a copy takes none back`() = runBlocking {
        setUser(xp = 500, daily = 0)
        val yesterday = today - 24L * 60 * 60 * 1000
        dao.insertedWorkouts.add(
            WorkoutEntity(id = 1, exerciseName = "Squat", weight = 80.0, reps = 5, dateInMillis = yesterday, username = "testuser", xpAwarded = 10)
        )

        repo.copyWorkoutsFromPreviousDay(today, sessionId = null)

        val copy = dao.insertedWorkouts.single { it.dateInMillis == today }
        assertEquals("a copy must not inherit the original's xp", 0, copy.xpAwarded)
        assertEquals("copying earns no xp", 500L, user.xp)

        repo.deleteWorkout(copy.id)
        assertEquals(500L, user.xp)
    }

    // ---- nutrition ----

    @Test
    fun `a new meal records 5 xp and deleting it takes 5 back`() = runBlocking {
        repo.insertNutrition(NutritionEntity(foodName = "Rice", calories = 300, dateInMillis = today))
        val meal = dao.insertedNutrition.single()
        assertEquals(5, meal.xpAwarded)
        assertEquals(5L, user.xp)

        repo.deleteNutrition(meal.id)

        assertEquals(0L, user.xp)
    }

    @Test
    fun `deleting a meal added past the daily cap takes nothing back`() = runBlocking {
        setUser(xp = 800, daily = 300)
        repo.insertNutrition(NutritionEntity(foodName = "Rice", calories = 300, dateInMillis = today))
        val id = dao.insertedNutrition.single().id

        repo.deleteNutrition(id)

        assertEquals(800L, user.xp)
    }

    @Test
    fun `copying yesterdays meals earns no xp and copies carry none`() = runBlocking {
        setUser(xp = 500)
        val yesterday = today - 24L * 60 * 60 * 1000
        dao.insertedNutrition.add(
            NutritionEntity(id = 1, foodName = "Egg", calories = 80, dateInMillis = yesterday, username = "testuser", xpAwarded = 5)
        )

        repo.copyNutritionFromPreviousDay(today)

        val copy = dao.insertedNutrition.single { it.dateInMillis == today }
        assertEquals(0, copy.xpAwarded)
        assertEquals(500L, user.xp)
    }

    // ---- awardXP return value and level ----

    @Test
    fun `awardXP reports how much was actually added`() = runBlocking {
        setUser(daily = 295)

        assertEquals(5, repo.awardXP(10))
        assertEquals(0, repo.awardXP(10))
    }

    @Test
    fun `awardXP on a new day resets the daily bucket and awards in full`() = runBlocking {
        setUser(xp = 500, daily = 300, lastXpDate = today - 24L * 60 * 60 * 1000)

        assertEquals(10, repo.awardXP(10))
        assertEquals(10L, user.dailyXPEarned)
    }

    @Test
    fun `reaching exactly 210 xp levels up to 3 - regression`() = runBlocking {
        setUser(xp = 200, level = 2)

        repo.awardXP(10)

        assertEquals(210L, user.xp)
        assertEquals(3, user.level)
    }

    @Test
    fun `revoking below a threshold drops the level`() = runBlocking {
        setUser(xp = 210, daily = 10, level = 3)
        dao.insertedWorkouts.add(set(id = 1, xpAwarded = 10).copy(username = "testuser"))

        repo.deleteWorkout(1)

        assertEquals(200L, user.xp)
        assertEquals(2, user.level)
    }

    @Test
    fun `level never drops below 1`() = runBlocking {
        setUser(xp = 3, daily = 3, level = 1)
        repo.revokeXP(50)

        assertEquals(0L, user.xp)
        assertEquals(1, user.level)
    }

    @Test
    fun `awardXP without a user does nothing`() = runBlocking {
        dao.mockUserFlow.value = null

        assertEquals(0, repo.awardXP(10))
    }

    // ---- concurrency ----

    /** A DAO whose user read is slow, so concurrent read-modify-write cycles overlap unless the repo serialises them. */
    private class SlowReadDao : RecordingFakeGlarmToDao() {
        override fun getUser(username: String): Flow<UserEntity?> = flow {
            delay(5)
            emit(mockUserFlow.value)
        }
    }

    @Test
    fun `concurrent awards all count - no lost updates`() = runBlocking {
        val slow = SlowReadDao()
        slow.mockUserFlow.value = UserEntity(username = "testuser", xp = 0, dailyXPEarned = 0, lastXPDate = today)
        val slowRepo = GlarmToRepository(slow, StaticFakeSessionManager())

        val results = (1..20).map { async(Dispatchers.Default) { slowRepo.awardXP(10) } }.awaitAll()

        assertEquals(200, results.sum())
        assertEquals(200L, slow.mockUserFlow.value!!.xp)
        assertEquals(200L, slow.mockUserFlow.value!!.dailyXPEarned)
    }

    @Test
    fun `concurrent awards still respect the daily cap exactly`() = runBlocking {
        val slow = SlowReadDao()
        slow.mockUserFlow.value = UserEntity(username = "testuser", xp = 0, dailyXPEarned = 0, lastXPDate = today)
        val slowRepo = GlarmToRepository(slow, StaticFakeSessionManager())

        val results = (1..40).map { async(Dispatchers.Default) { slowRepo.awardXP(10) } }.awaitAll()

        assertEquals("40 x 10 requested, cap is 300", 300, results.sum())
        assertEquals(300L, slow.mockUserFlow.value!!.xp)
    }

    @Test
    fun `concurrent awards and revokes end at the right total`() = runBlocking {
        val slow = SlowReadDao()
        slow.mockUserFlow.value = UserEntity(username = "testuser", xp = 1000, dailyXPEarned = 0, lastXPDate = today)
        val slowRepo = GlarmToRepository(slow, StaticFakeSessionManager())

        val jobs = (1..10).map { async(Dispatchers.Default) { slowRepo.awardXP(10) } } +
            (1..10).map { async(Dispatchers.Default) { slowRepo.revokeXP(5); 0 } }
        jobs.awaitAll()

        assertEquals(1000L + 100 - 50, slow.mockUserFlow.value!!.xp)
    }
}
