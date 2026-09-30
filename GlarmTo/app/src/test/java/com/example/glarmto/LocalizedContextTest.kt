package com.example.glarmto

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.preferences.LanguageManager
import com.example.glarmto.ui.util.LocalizedContext
import com.example.glarmto.ui.util.findActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The context the screens see while a language is applied must still reach the real activity: the
 * workout screen's picture-in-picture timer and the camera permission request both look for it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalizedContextTest {

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val manager by lazy { LanguageManager(app) { "en" } }

    private fun activity(): ComponentActivity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()

    private fun localized(base: Context, code: String) =
        LocalizedContext(base, manager.localizedContext(base, code).resources)

    @Test
    fun `strings come from the chosen language`() {
        val base = activity()

        assertEquals("Home", localized(base, "en").getString(R.string.nav_home))
        assertEquals("หน้าหลัก", localized(base, "th").getString(R.string.nav_home))
    }

    @Test
    fun `format arguments work through the wrapper`() {
        assertEquals("สวัสดี Ann! 💪", localized(activity(), "th").getString(R.string.f_hello, "Ann"))
    }

    @Test
    fun `the real activity is still reachable - the regression this wrapper exists for`() {
        val base = activity()

        assertSame(base, localized(base, "th").findActivity())
        assertSame(base, localized(base, "en").findActivity())
    }

    @Test
    fun `a plain configuration context would have hidden the activity`() {
        val base = activity()
        val plain = manager.localizedContext(base, "th")

        assertNull("this is why LocalizedContext is a ContextWrapper", plain.findActivity())
    }

    @Test
    fun `findActivity looks through several wrappers`() {
        val base = activity()
        val nested = ContextWrapper(ContextWrapper(localized(base, "th")))

        assertSame(base, nested.findActivity())
    }

    @Test
    fun `findActivity returns the activity itself when given one`() {
        val base = activity()

        assertSame(base, base.findActivity())
    }

    @Test
    fun `findActivity returns null for the application context`() {
        assertNull(app.findActivity())
    }

    @Test
    fun `the application context is still the application - screens cast it to GlarmToApplication`() {
        val base = activity()

        assertSame(base.applicationContext, localized(base, "th").applicationContext)
        assertNotNull(localized(base, "th").applicationContext)
    }

    @Test
    fun `the base context is the activity`() {
        val base = activity()

        assertSame(base, localized(base, "th").baseContext)
    }

    @Test
    fun `switching language means making a new wrapper around the same activity`() {
        val base = activity()
        val english = localized(base, "en")
        val thai = localized(base, "th")

        assertSame(english.findActivity(), thai.findActivity())
        assertEquals("Workout", english.getString(R.string.nav_workout))
        assertEquals("ออกกำลังกาย", thai.getString(R.string.nav_workout))
    }
}
