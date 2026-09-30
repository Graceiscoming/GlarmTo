package com.example.glarmto

import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WorkoutEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.MutableFakeSessionManager
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class GlarmToRepositoryTest {

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var session: MutableFakeSessionManager
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        session = MutableFakeSessionManager()
        repository = GlarmToRepository(dao, session)
    }

    @Test
    fun `register new user inserts row and marks profile not setup`() = runBlocking {
        dao.mockUserFlow.value = null
        val success = repository.register("newuser", "mypass")

        assertTrue(success)
        assertTrue(dao.insertedUsers.any { it.username == "newuser" && it.password == "mypass" })
        assertFalse(session.isProfileSetup())
    }

    @Test
    fun `register existing user returns false`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "existing", password = "old")
        val success = repository.register("existing", "newpassword")

        assertFalse(success)
    }

    @Test
    fun `login with correct password copies profileSetup from Room`() = runBlocking {
        val bob = UserEntity(username = "bob", password = "secretpassword", profileSetup = true)
        dao.mockUserFlow.value = bob
        val success = repository.login("bob", "secretpassword")

        assertTrue(success)
        assertTrue(session.isProfileSetup())
    }

    @Test
    fun `login with incorrect password returns false`() = runBlocking {
        val bob = UserEntity(username = "bob", password = "secretpassword", profileSetup = true)
        dao.mockUserFlow.value = bob
        val success = repository.login("bob", "wrongpassword")

        assertFalse(success)
    }

    @Test
    fun `login with empty password allows backward compatibility`() = runBlocking {
        val legacyBob = UserEntity(username = "legacy", password = "", profileSetup = true)
        dao.mockUserFlow.value = legacyBob
        val success = repository.login("legacy", "anypassword") // Should pass because db password is ""

        assertTrue(success)
        assertTrue(session.isProfileSetup())
    }

    @Test
    fun `insertWorkout stamps username and increases XP`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "testuser", xp = 0, dailyXPEarned = 0, lastXPDate = 0)
        val staticSession = StaticFakeSessionManager()
        val repo = GlarmToRepository(dao, staticSession)

        val day = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        repo.insertWorkout(
            WorkoutEntity(
                exerciseName = "Press",
                weight = 40.0,
                reps = 8,
                dateInMillis = day,
                username = "ignored",
                sessionId = null
            )
        )

        assertEquals("testuser", dao.insertedWorkouts.single().username)
        assertEquals(10L, dao.lastUpdatedUser?.dailyXPEarned)
    }

    @Test
    fun `deleteWorkout revokes XP`() = runBlocking {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        dao.mockUserFlow.value = UserEntity(
            username = "testuser",
            xp = 100,
            dailyXPEarned = 50,
            lastXPDate = today
        )
        val staticSession = StaticFakeSessionManager()
        val repo = GlarmToRepository(dao, staticSession)
        dao.insertedWorkouts.add(
            WorkoutEntity(id = 1, exerciseName = "A", weight = 1.0, reps = 1, dateInMillis = today, username = "testuser")
        )

        repo.deleteWorkout(1)

        assertEquals(90L, dao.lastUpdatedUser?.xp)
    }

    @Test
    fun `getWorkoutsForSession filters by username`() = runBlocking {
        dao.insertedWorkouts.add(
            WorkoutEntity(id = 1, exerciseName = "A", weight = 1.0, reps = 1, dateInMillis = 1L, username = "testuser", sessionId = 7)
        )
        dao.insertedWorkouts.add(
            WorkoutEntity(id = 2, exerciseName = "B", weight = 2.0, reps = 2, dateInMillis = 1L, username = "other", sessionId = 7)
        )
        val staticSession = StaticFakeSessionManager()
        val repo = GlarmToRepository(dao, staticSession)

        val list = repo.getWorkoutsForSession(7).first()
        assertEquals(1, list.size)
        assertEquals("testuser", list.single().username)
    }

    @Test
    fun `getTotalXPThreshold level 1 is zero`() {
        assertEquals(0L, repository.getTotalXPThreshold(1))
    }

    @Test
    fun `getXPRequiredForNextLevel grows with level`() {
        val l1 = repository.getXPRequiredForNextLevel(1)
        val l5 = repository.getXPRequiredForNextLevel(5)
        assertTrue(l5 > l1)
    }
}
