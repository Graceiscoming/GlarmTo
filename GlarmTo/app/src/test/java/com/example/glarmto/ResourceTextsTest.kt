package com.example.glarmto

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.ResourceTexts
import com.example.glarmto.testsupport.MainDispatcherRule
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import com.example.glarmto.testsupport.StaticFakeSessionManager
import com.example.glarmto.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Text looked up from code that has no Compose context follows the language picked in the app, live. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = GlarmToApplication::class)
class ResourceTextsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val app get() = ApplicationProvider.getApplicationContext<GlarmToApplication>()

    @Before
    fun startInEnglish() {
        app.languageManager.setLanguage("en")
    }

    @Test
    fun `gives English text while the app is English`() {
        assertEquals("Home", ResourceTexts(app).get(R.string.nav_home))
    }

    @Test
    fun `gives Thai text while the app is Thai`() {
        app.languageManager.setLanguage("th")

        assertEquals("หน้าหลัก", ResourceTexts(app).get(R.string.nav_home))
    }

    @Test
    fun `follows a language switch without being recreated`() {
        val texts = ResourceTexts(app)
        assertEquals("Home", texts.get(R.string.nav_home))

        app.languageManager.toggle()
        assertEquals("หน้าหลัก", texts.get(R.string.nav_home))

        app.languageManager.toggle()
        assertEquals("Home", texts.get(R.string.nav_home))
    }

    @Test
    fun `fills in arguments in both languages`() {
        val texts = ResourceTexts(app)
        assertEquals("Hello, Mai! 💪", texts.get(R.string.f_hello, "Mai"))
        assertEquals("LVL: 7", texts.get(R.string.f_ig_level, 7))

        app.languageManager.setLanguage("th")

        assertEquals("สวัสดี Mai! 💪", texts.get(R.string.f_hello, "Mai"))
        assertEquals("เลเวล: 7", texts.get(R.string.f_ig_level, 7))
        assertEquals("สตรีค: 12 วัน🔥", texts.get(R.string.f_ig_streak, 12))
    }

    @Test
    fun `typed numbers format the same in both languages`() {
        val texts = ResourceTexts(app)
        val english = texts.get(R.string.f_bmi, 22.456)
        app.languageManager.setLanguage("th")

        assertEquals("BMI: 22.5", english)
        assertEquals("BMI: 22.5", texts.get(R.string.f_bmi, 22.456))
    }

    @Test
    fun `percent signs in a format string come out as a single percent`() {
        assertEquals("⚠️ Chest is only 30% recovered — sets trimmed, consider going lighter today.",
            ResourceTexts(app).get(R.string.f_gen_warn_low_recovery, "Chest", 30))
    }

    @Test
    fun `a plain application without a language manager falls back to its own resources`() {
        // Some tests build view models with a bare Application; text must still resolve (in English).
        class BareApplication : Application() {
            fun attach(base: android.content.Context) = attachBaseContext(base)
        }
        val bare = BareApplication().also { it.attach(app.baseContext) }

        assertEquals("Home", ResourceTexts(bare).get(R.string.nav_home))
    }

    // ---------------- export sheets use the app's language ----------------

    private fun dashboardViewModel(): DashboardViewModel {
        val dao = RecordingFakeGlarmToDao()
        dao.mockUserFlow.value = UserEntity(username = "testuser")
        return DashboardViewModel(app, GlarmToRepository(dao, StaticFakeSessionManager()))
    }

    private fun awaitStartedActivity(): Intent {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(app).nextStartedActivity?.let { return it }
            Thread.sleep(20)
        }
        throw AssertionError("no chooser was started")
    }

    private fun Intent.innerSubject(): String? =
        getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.getStringExtra(Intent.EXTRA_SUBJECT)

    @Test
    fun `sharing the JSON export uses English titles in English`() {
        dashboardViewModel().shareExportJson()

        val chooser = awaitStartedActivity()
        assertEquals("Export JSON", chooser.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals("GlarmTo backup (JSON)", chooser.innerSubject())
    }

    @Test
    fun `sharing the JSON export uses Thai titles in Thai`() {
        app.languageManager.setLanguage("th")

        dashboardViewModel().shareExportJson()

        val chooser = awaitStartedActivity()
        assertEquals("ส่งออก JSON", chooser.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals("สำรองข้อมูล GlarmTo (JSON)", chooser.innerSubject())
    }

    @Test
    fun `sharing the CSV export is localized too`() {
        app.languageManager.setLanguage("th")

        dashboardViewModel().shareExportCsv()

        val chooser = awaitStartedActivity()
        assertEquals("ส่งออก CSV", chooser.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals("สำรองข้อมูล GlarmTo (CSV)", chooser.innerSubject())
        assertNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
    }
}
