package com.example.glarmto

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.ui.dashboard.DashboardScreen
import com.example.glarmto.ui.util.LanguageProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/**
 * The language button joins the dashboard's row of icon buttons. Its geometry is checked here (icon
 * buttons have fixed sizes, so this is reliable in Robolectric, unlike text widths) to make sure it
 * stays compact and leaves the greeting enough room on a 360dp phone.
 */
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = GlarmToApplication::class, qualifiers = "w360dp-h740dp")
class DashboardHeaderLayoutUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<GlarmToApplication>()

    private fun bounds(description: String) =
        rule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun showInEnglish() {
        app.languageManager.setLanguage("en")
        runBlocking { if (!app.repository.register("tester", "pw-1234")) app.repository.login("tester", "pw-1234") }
        rule.setContent { LanguageProvider(app.languageManager) { DashboardScreen() } }
    }

    @Test
    fun `the language button is icon sized`() {
        showInEnglish()

        val button = bounds("Switch language")
        assertEquals(40f, button.width, 1f)
        assertEquals(40f, button.height, 1f)
    }

    @Test
    fun `the four action buttons sit side by side inside the screen without overlapping`() {
        showInEnglish()

        val row = listOf("History", "Switch language", "Themes", "Logout").map { bounds(it) }

        row.zipWithNext().forEach { (left, right) ->
            assertTrue("$left overlaps $right", left.right <= right.left)
        }
        assertTrue("last button runs off the screen: ${row.last()}", row.last().right <= 360f)
    }

    @Test
    fun `the greeting keeps at least 140dp of room on a 360dp phone`() {
        showInEnglish()

        // The greeting column fills the space to the left of the buttons, starting at the 16dp margin.
        val room = bounds("History").left - 16f
        assertTrue("only ${room}dp left for the greeting", room >= 140f)
    }

    @Test
    fun `the row of buttons is the same width in Thai`() {
        app.languageManager.setLanguage("th")
        runBlocking { if (!app.repository.register("tester", "pw-1234")) app.repository.login("tester", "pw-1234") }
        rule.setContent { LanguageProvider(app.languageManager) { DashboardScreen() } }

        val button = bounds("เปลี่ยนภาษา")
        assertEquals(40f, button.width, 1f)
        assertTrue(bounds("ประวัติ").right <= button.left)
    }
}
