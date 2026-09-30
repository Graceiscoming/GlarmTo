package com.example.glarmto

import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class XPLimitAndMidnightTest {

    private lateinit var fakeDao: RecordingFakeGlarmToDao
    private lateinit var fakeSession: StaticFakeSessionManager
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        fakeDao = RecordingFakeGlarmToDao()
        fakeSession = StaticFakeSessionManager()
        repository = GlarmToRepository(fakeDao, fakeSession)
    }

    @Test
    fun `test getDayRange creates correct start and end bounds`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 6, 12, 34)
        }
        val (start, end) = repository.getDayRange(cal)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(6, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun `test XP awards up to 300 limit per day`() = runBlocking {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val user = UserEntity(
            username = "testuser",
            xp = 1000,
            dailyXPEarned = 295,
            lastXPDate = today
        )
        fakeDao.mockUserFlow.value = user

        repository.awardXP(10)

        assertEquals(1005.toLong(), fakeDao.lastUpdatedUser?.xp)
        assertEquals(300.toLong(), fakeDao.lastUpdatedUser?.dailyXPEarned)
    }

    @Test
    fun `test XP resets on a new day`() = runBlocking {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis

        val user = UserEntity(
            username = "testuser",
            xp = 500,
            dailyXPEarned = 300,
            lastXPDate = yesterday
        )
        fakeDao.mockUserFlow.value = user

        repository.awardXP(10)

        assertEquals(10.toLong(), fakeDao.lastUpdatedUser?.dailyXPEarned)
        assertEquals(510.toLong(), fakeDao.lastUpdatedUser?.xp)
    }

    @Test
    fun `test revokeXP subtracts total and daily when same day`() = runBlocking {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val user = UserEntity(
            username = "testuser",
            xp = 100,
            dailyXPEarned = 50,
            lastXPDate = today
        )
        fakeDao.mockUserFlow.value = user

        repository.revokeXP(10)

        assertEquals(90.toLong(), fakeDao.lastUpdatedUser?.xp)
        assertEquals(40.toLong(), fakeDao.lastUpdatedUser?.dailyXPEarned)
    }
}
