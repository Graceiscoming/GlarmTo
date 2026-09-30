package com.example.glarmto

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DashboardViewModelRobolectricTest {

    private lateinit var app: Application
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        val dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser", dailyGoal = 2400)
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    @Test
    fun `dailyGoal reflects user after flow is collected`() = runBlocking {
        val vm = DashboardViewModel(app, repository)
        assertNotNull(vm)
        // dailyGoal uses its own stateIn; it only tracks user when something subscribes to dailyGoal
        val job = launch { vm.dailyGoal.collect {} }
        delay(150)
        assertEquals(2400, vm.dailyGoal.value)
        job.cancel()
    }
}
