package com.example.glarmto

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.CalendarDayUtils
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.history.HistoryViewModel
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
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HistoryViewModelRobolectricTest {

    private lateinit var app: Application
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        val dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    @Test
    fun `setSelectedDate normalizes to local day from UTC picker millis`() = runBlocking {
        val vm = HistoryViewModel(app, repository)
        val job = launch { vm.sessions.collect {} }
        val local = Calendar.getInstance()
        val y = local.get(Calendar.YEAR)
        val m = local.get(Calendar.MONTH)
        val d = local.get(Calendar.DAY_OF_MONTH)
        val utcPicker = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        vm.setSelectedDate(utcPicker)
        delay(100)

        assertEquals(CalendarDayUtils.localTodayStartMillis(), vm.selectedDate.value)
        job.cancel()
    }

    @Test
    fun `viewModel instantiates`() {
        val vm = HistoryViewModel(app, repository)
        assertNotNull(vm)
    }
}
