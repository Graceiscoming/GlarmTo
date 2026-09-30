package com.example.glarmto

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.ui.util.LanguageProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.util.Locale

/**
 * The real navigation shell with the real dashboard: the TH button translates the menu, the screen and
 * every screen reached afterwards.
 */
@RunWith(RobolectricTestRunner::class)
// Room reads on its own background threads; Robolectric's default SQLite mode only allows one thread.
@SQLiteMode(SQLiteMode.Mode.NATIVE)
// A typical phone width (the Robolectric default is a 320dp-wide screen, narrower than real phones).
@Config(sdk = [28], application = GlarmToApplication::class, qualifiers = "w411dp-h891dp")
class LanguageSwitchDashboardUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<GlarmToApplication>()
    private val languages get() = app.languageManager
    private val originalLocale: Locale = Locale.getDefault()

    @Before
    fun signIn() {
        languages.setLanguage("en")
        // The Room singleton outlives a single test, so the user may already exist: then just log in.
        runBlocking { if (!app.repository.register("tester", "pw-1234")) app.repository.login("tester", "pw-1234") }
        rule.setContent { LanguageProvider(languages) { MainScreen(Screen.Dashboard.route) } }
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    private fun tapSwitchLanguage() = rule.onNodeWithContentDescription(
        if (languages.getLanguage() == "en") "Switch language" else "เปลี่ยนภาษา"
    ).performClick()

    @Test
    fun `dashboard starts in English with the TH button`() {
        rule.onNodeWithText("Hello, tester! 💪").assertIsDisplayed()
        rule.onNodeWithText("TH").assertIsDisplayed()
        listOf("Home", "Routines", "Nutrition", "Profile").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `tapping TH translates the greeting and the bottom menu`() {
        tapSwitchLanguage()

        rule.onNodeWithText("สวัสดี tester! 💪").assertIsDisplayed()
        listOf("หน้าหลัก", "โปรแกรมฝึก", "โภชนาการ", "โปรไฟล์").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        listOf("Home", "Routines", "Nutrition", "Profile").forEach { rule.onNodeWithText(it).assertDoesNotExist() }
        rule.onNodeWithText("EN").assertIsDisplayed()
    }

    @Test
    fun `the icon descriptions are translated too`() {
        tapSwitchLanguage()

        rule.onNodeWithContentDescription("ประวัติ").assertIsDisplayed()
        rule.onNodeWithContentDescription("ธีม").assertIsDisplayed()
        rule.onNodeWithContentDescription("ออกจากระบบ").assertIsDisplayed()
        rule.onNodeWithContentDescription("History").assertDoesNotExist()
    }

    @Test
    fun `tapping EN comes back to English`() {
        tapSwitchLanguage()
        tapSwitchLanguage()

        rule.onNodeWithText("Hello, tester! 💪").assertIsDisplayed()
        rule.onNodeWithText("Home").assertIsDisplayed()
        assertEquals("en", languages.getLanguage())
    }

    @Test
    fun `the language carries over to the screens reached from the menu`() {
        tapSwitchLanguage()

        rule.onNodeWithText("โปรแกรมฝึก").performClick()

        rule.onNodeWithText("โปรแกรมฝึกของฉัน").assertIsDisplayed()
        rule.onNodeWithText("My Customs Routines").assertDoesNotExist()
    }

    @Test
    fun `routines screen is English when the app is English`() {
        rule.onNodeWithText("Routines").performClick()

        rule.onNodeWithText("My Customs Routines").assertIsDisplayed()
    }

    @Test
    fun `the profile screen tabs follow the language`() {
        tapSwitchLanguage()

        rule.onNodeWithText("โปรไฟล์").performClick()

        rule.onNodeWithText("โปรไฟล์ของฉัน").assertIsDisplayed()
        rule.onNodeWithText("เครื่องคำนวณ 1RM").assertIsDisplayed()
        rule.onNodeWithText("แผ่นน้ำหนัก").assertIsDisplayed()
    }

    @Test
    fun `the workout screen shows Thai after switching`() {
        tapSwitchLanguage()

        rule.onNodeWithText("ออกกำลังกาย").performClick()

        rule.onNodeWithText("พร้อมลุยหรือยัง?").assertIsDisplayed()
        rule.onNodeWithText("Ready to crush it?").assertDoesNotExist()
    }

    @Test
    fun `switching language on one screen and back leaves the menu working`() {
        tapSwitchLanguage()
        rule.onNodeWithText("โปรแกรมฝึก").performClick()
        rule.onNodeWithText("หน้าหลัก").performClick()

        rule.onNodeWithText("สวัสดี tester! 💪").assertIsDisplayed()
    }
}
