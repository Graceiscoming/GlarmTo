package com.example.glarmto

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.ui.util.LanguageProvider
import com.example.glarmto.ui.util.LanguageToggleButton
import com.example.glarmto.ui.util.findActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** The language switch, driven the way a user drives it, on small screens built from the real pieces. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = GlarmToApplication::class)
class LanguageSwitchUiTest {

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

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { LanguageProvider(languages) { content() } }
    }

    @Test
    fun `text shows in English first`() {
        show { Text(stringResource(R.string.nav_home)) }

        rule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun `changing the language swaps the text without restarting`() {
        show { Text(stringResource(R.string.nav_home)) }

        rule.runOnIdle { languages.setLanguage("th") }

        rule.onNodeWithText("หน้าหลัก").assertIsDisplayed()
        rule.onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun `text starts in Thai when Thai was chosen before`() {
        languages.setLanguage("th")

        show { Text(stringResource(R.string.nav_home)) }

        rule.onNodeWithText("หน้าหลัก").assertIsDisplayed()
    }

    @Test
    fun `formatted text is localized`() {
        show { Text(stringResource(R.string.f_hello, "Bob")) }
        rule.onNodeWithText("Hello, Bob! 💪").assertIsDisplayed()

        rule.runOnIdle { languages.setLanguage("th") }

        rule.onNodeWithText("สวัสดี Bob! 💪").assertIsDisplayed()
    }

    // ---------------- the toggle button ----------------

    @Test
    fun `the button reads TH while the app is English`() {
        show { LanguageToggleButton(languages) }

        rule.onNodeWithText("TH").assertIsDisplayed()
        rule.onNodeWithText("EN").assertDoesNotExist()
    }

    @Test
    fun `tapping TH switches the app to Thai and the button becomes EN`() {
        show {
            Column {
                LanguageToggleButton(languages)
                Text(stringResource(R.string.nav_home))
            }
        }

        rule.onNodeWithText("TH").performClick()

        rule.onNodeWithText("EN").assertIsDisplayed()
        rule.onNodeWithText("หน้าหลัก").assertIsDisplayed()
        assertEquals("th", languages.getLanguage())
    }

    @Test
    fun `tapping EN switches back to English and the button becomes TH`() {
        languages.setLanguage("th")
        show {
            Column {
                LanguageToggleButton(languages)
                Text(stringResource(R.string.nav_home))
            }
        }
        rule.onNodeWithText("EN").assertIsDisplayed()

        rule.onNodeWithText("EN").performClick()

        rule.onNodeWithText("TH").assertIsDisplayed()
        rule.onNodeWithText("Home").assertIsDisplayed()
        assertEquals("en", languages.getLanguage())
    }

    @Test
    fun `the button can be tapped back and forth`() {
        show {
            Column {
                LanguageToggleButton(languages)
                Text(stringResource(R.string.nav_home))
            }
        }

        repeat(3) {
            rule.onNodeWithText("TH").performClick()
            rule.onNodeWithText("หน้าหลัก").assertIsDisplayed()
            rule.onNodeWithText("EN").performClick()
            rule.onNodeWithText("Home").assertIsDisplayed()
        }
    }

    @Test
    fun `the button is labelled for screen readers in the current language`() {
        show { LanguageToggleButton(languages) }
        rule.onNodeWithContentDescription("Switch language").assertIsDisplayed()

        rule.runOnIdle { languages.setLanguage("th") }

        rule.onNodeWithContentDescription("เปลี่ยนภาษา").assertIsDisplayed()
    }

    // ---------------- what the screens rely on ----------------

    @Test
    fun `screens can still find their activity - picture in picture and the camera permission need it`() {
        var found: Activity? = null
        show { found = LocalContext.current.findActivity() }
        assertSame(rule.activity, found)

        rule.runOnIdle { languages.setLanguage("th") }

        assertSame(rule.activity, found)
    }

    @Test
    fun `screens can still reach the application`() {
        var application: Any? = null
        show { application = LocalContext.current.applicationContext }

        assertTrue(application is GlarmToApplication)
    }

    @Test
    fun `the default locale follows the language so dates and numbers format to match`() {
        show { Text(stringResource(R.string.nav_home)) }
        assertEquals("en", Locale.getDefault().language)

        rule.runOnIdle { languages.setLanguage("th") }
        rule.waitForIdle()

        assertEquals("th", Locale.getDefault().language)

        rule.runOnIdle { languages.setLanguage("en") }
        rule.waitForIdle()

        assertEquals("en", Locale.getDefault().language)
    }

    @Test
    fun `the composition configuration carries the chosen locale`() {
        var language = ""
        show { language = LocalConfiguration.current.locales[0].language }
        assertEquals("en", language)

        rule.runOnIdle { languages.setLanguage("th") }
        rule.waitForIdle()

        assertEquals("th", language)
    }

    @Test
    fun `the application's own language choice is independent of the phone setting`() {
        // Robolectric's phone is English; the app still shows Thai when told to.
        languages.setLanguage("th")
        show { Text(stringResource(R.string.log_in)) }

        rule.onNodeWithText("เข้าสู่ระบบ").assertIsDisplayed()
        assertEquals("en", app.resources.configuration.locales[0].language)
    }
}
