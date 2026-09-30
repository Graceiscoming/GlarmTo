package com.example.glarmto

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.ui.login.LoginScreen
import com.example.glarmto.ui.login.RegisterScreen
import com.example.glarmto.ui.login.WelcomeScreen
import com.example.glarmto.ui.util.LanguageProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Real screens, switched between English and Thai with the on-screen button. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = GlarmToApplication::class)
class LanguageSwitchScreensUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<GlarmToApplication>()
    private val languages get() = app.languageManager
    private val originalLocale: Locale = Locale.getDefault()

    @Before
    fun startInEnglish() {
        languages.setLanguage("en")
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    // ---------------- welcome screen ----------------

    @Test
    fun `welcome screen shows English and a TH button`() {
        rule.setContent { LanguageProvider(languages) { WelcomeScreen({}, {}) } }

        rule.onNodeWithText("BECOME UNSTOPPABLE.").assertIsDisplayed()
        rule.onNodeWithText("LOG IN").assertIsDisplayed()
        rule.onNodeWithText("CREATE ACCOUNT").assertIsDisplayed()
        rule.onNodeWithText("TH").assertIsDisplayed()
    }

    @Test
    fun `tapping TH on the welcome screen translates the whole screen`() {
        rule.setContent { LanguageProvider(languages) { WelcomeScreen({}, {}) } }

        rule.onNodeWithText("TH").performClick()

        rule.onNodeWithText("ไม่มีอะไรหยุดคุณได้").assertIsDisplayed()
        rule.onNodeWithText("เข้าสู่ระบบ").assertIsDisplayed()
        rule.onNodeWithText("สร้างบัญชี").assertIsDisplayed()
        rule.onNodeWithText("EN").assertIsDisplayed()
        rule.onNodeWithText("BECOME UNSTOPPABLE.").assertDoesNotExist()
        rule.onNodeWithText("LOG IN").assertDoesNotExist()
        rule.onNodeWithText("CREATE ACCOUNT").assertDoesNotExist()
    }

    @Test
    fun `tapping EN on the welcome screen goes back to English`() {
        languages.setLanguage("th")
        rule.setContent { LanguageProvider(languages) { WelcomeScreen({}, {}) } }
        rule.onNodeWithText("เข้าสู่ระบบ").assertIsDisplayed()

        rule.onNodeWithText("EN").performClick()

        rule.onNodeWithText("LOG IN").assertIsDisplayed()
        rule.onNodeWithText("TH").assertIsDisplayed()
    }

    @Test
    fun `the welcome screen buttons still work after switching language`() {
        var loginTapped = false
        var registerTapped = false
        rule.setContent { LanguageProvider(languages) { WelcomeScreen({ loginTapped = true }, { registerTapped = true }) } }

        rule.onNodeWithText("TH").performClick()
        rule.onNodeWithText("เข้าสู่ระบบ").performClick()
        rule.onNodeWithText("สร้างบัญชี").performClick()

        assertEquals(true, loginTapped)
        assertEquals(true, registerTapped)
    }

    @Test
    fun `the welcome screen switch is remembered for the next launch`() {
        rule.setContent { LanguageProvider(languages) { WelcomeScreen({}, {}) } }

        rule.onNodeWithText("TH").performClick()

        assertEquals("th", com.example.glarmto.data.preferences.LanguageManager(app).getLanguage())
    }

    // ---------------- login / register ----------------

    @Test
    fun `login screen labels follow the language`() {
        rule.setContent { LanguageProvider(languages) { LoginScreen({}, {}) } }
        rule.onNodeWithText("Log in to continue tracking.").assertIsDisplayed()

        rule.runOnIdle { languages.setLanguage("th") }

        rule.onNodeWithText("เข้าสู่ระบบเพื่อบันทึกต่อ").assertIsDisplayed()
        rule.onNodeWithText("ชื่อผู้ใช้").assertIsDisplayed()
        rule.onNodeWithText("รหัสผ่าน").assertIsDisplayed()
    }

    @Test
    fun `login error messages come out in the current language`() {
        languages.setLanguage("th")
        rule.setContent { LanguageProvider(languages) { LoginScreen({}, {}) } }

        rule.onNodeWithText("เข้าสู่ระบบ").performClick()

        rule.onNodeWithText("กรุณากรอกชื่อผู้ใช้และรหัสผ่าน").assertIsDisplayed()
    }

    @Test
    fun `login error message in English`() {
        rule.setContent { LanguageProvider(languages) { LoginScreen({}, {}) } }

        rule.onNodeWithText("LOG IN").performClick()

        rule.onNodeWithText("Please enter username and password.").assertIsDisplayed()
    }

    @Test
    fun `register screen validation messages are localized`() {
        languages.setLanguage("th")
        rule.setContent { LanguageProvider(languages) { RegisterScreen({}, {}) } }

        // The button is below the fields, off the small test screen: scroll to it before tapping.
        rule.onNodeWithText("สมัครสมาชิก").performScrollTo().performClick()

        rule.onNodeWithText("กรุณากรอกข้อมูลให้ครบทุกช่อง").assertExists()
    }

    @Test
    fun `register screen shows its title in both languages`() {
        rule.setContent { LanguageProvider(languages) { RegisterScreen({}, {}) } }
        rule.onNodeWithText("Create an account to start tracking your gains.").assertIsDisplayed()

        rule.runOnIdle { languages.setLanguage("th") }

        rule.onNodeWithText("สร้างบัญชีเพื่อเริ่มติดตามความก้าวหน้าของคุณ").assertIsDisplayed()
        rule.onNodeWithContentDescription("กลับ").assertIsDisplayed()
    }
}
