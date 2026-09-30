package com.example.glarmto

import android.app.Application
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.routines.RoutineViewModel
import kotlinx.coroutines.delay
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
class RoutineViewModelRobolectricTest {

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
    fun `addRoutine persists pipe separated exercises`() = runBlocking {
        val vm = RoutineViewModel(repository)
        delay(40)
        vm.addRoutine("Leg day", listOf("Squat", "Leg Press"))
        delay(200)

        assertEquals(1, dao.insertedRoutines.size)
        val r = dao.insertedRoutines.first()
        assertEquals("Leg day", r.routineName)
        assertEquals("Squat|Leg Press", r.exercises)
        assertEquals("testuser", r.username)
    }

    @Test
    fun `addRoutine ignores blank name`() = runBlocking {
        val vm = RoutineViewModel(repository)
        vm.addRoutine("   ", listOf("A"))
        delay(100)
        assertTrue(dao.insertedRoutines.isEmpty())
    }
}
