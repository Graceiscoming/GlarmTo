package com.example.glarmto

import android.app.Application
import com.example.glarmto.testsupport.MainDispatcherRule
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.nutrition.NutritionViewModel
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
class NutritionViewModelRobolectricTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var app: Application
    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var repository: GlarmToRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser", dailyGoal = 2000)
        repository = GlarmToRepository(dao, StaticFakeSessionManager())
    }

    @Test
    fun `addNutrition persists entity with username`() = runBlocking {
        val vm = NutritionViewModel(app, repository)
        delay(50)
        vm.addNutrition("Oatmeal", 320)
        delay(150)

        assertEquals(1, dao.insertedNutrition.size)
        val row = dao.insertedNutrition.first()
        assertTrue(row.foodName == "Oatmeal" && row.calories == 320 && row.username == "testuser")
    }
}
