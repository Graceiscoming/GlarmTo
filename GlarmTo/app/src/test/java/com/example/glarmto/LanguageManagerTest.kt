package com.example.glarmto

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.preferences.LanguageManager
import com.example.glarmto.testsupport.StringFiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LanguageManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("glarmto_language_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun manager(device: String = "en") = LanguageManager(context) { device }

    // ---------------- first launch follows the phone ----------------

    @Test
    fun `an English phone starts in English`() {
        assertEquals("en", manager("en").getLanguage())
    }

    @Test
    fun `a Thai phone starts in Thai`() {
        assertEquals("th", manager("th").getLanguage())
    }

    @Test
    fun `any other phone language starts in English`() {
        listOf("fr", "ja", "de", "", "zh").forEach { assertEquals("device=$it", "en", manager(it).getLanguage()) }
    }

    // ---------------- choosing and remembering ----------------

    @Test
    fun `setLanguage changes the language and emits it`() = runBlocking {
        val m = manager("en")

        m.setLanguage("th")

        assertEquals("th", m.getLanguage())
        assertEquals("th", m.language.first())
    }

    @Test
    fun `the choice is remembered by the next launch`() {
        manager("en").setLanguage("th")

        assertEquals("th", manager("en").getLanguage())
    }

    @Test
    fun `a saved choice wins over the phone language`() {
        manager("th").setLanguage("en")

        assertEquals("en", manager("th").getLanguage())
    }

    @Test
    fun `an unsupported language is rejected and changes nothing`() {
        val m = manager("en")

        try {
            m.setLanguage("fr")
            fail("expected an error")
        } catch (expected: IllegalArgumentException) {
        }

        assertEquals("en", m.getLanguage())
        assertEquals("en", manager("en").getLanguage())
    }

    @Test
    fun `a corrupted saved value falls back to the phone language`() {
        context.getSharedPreferences("glarmto_language_prefs", Context.MODE_PRIVATE)
            .edit().putString(LanguageManager.KEY_LANGUAGE, "klingon").commit()

        assertEquals("th", manager("th").getLanguage())
        assertEquals("en", manager("en").getLanguage())
    }

    @Test
    fun `setting the same language again is harmless`() {
        val m = manager("en")
        m.setLanguage("en")
        m.setLanguage("en")

        assertEquals("en", m.getLanguage())
    }

    // ---------------- the toggle ----------------

    @Test
    fun `toggle switches between English and Thai and returns the new language`() {
        val m = manager("en")

        assertEquals("th", m.toggle())
        assertEquals("th", m.getLanguage())
        assertEquals("en", m.toggle())
        assertEquals("en", m.getLanguage())
    }

    @Test
    fun `toggle is remembered`() {
        val m = manager("en")
        m.toggle()

        assertEquals("th", manager("en").getLanguage())
    }

    @Test
    fun `the button shows the language it will switch to`() {
        assertEquals("TH", LanguageManager.toggleLabel("en"))
        assertEquals("EN", LanguageManager.toggleLabel("th"))
    }

    @Test
    fun `next is its own inverse`() {
        LanguageManager.supported.forEach {
            assertEquals(it, LanguageManager.next(LanguageManager.next(it)))
            assertNotEquals(it, LanguageManager.next(it))
        }
    }

    @Test
    fun `the label always names a supported language`() {
        LanguageManager.supported.forEach {
            assertTrue(LanguageManager.toggleLabel(it).lowercase() in LanguageManager.supported)
        }
    }

    @Test
    fun `locales are plain language locales`() {
        assertEquals(Locale("th"), LanguageManager.localeFor("th"))
        assertEquals(Locale("en"), LanguageManager.localeFor("en"))
        assertEquals("unknown codes fall back to English", Locale("en"), LanguageManager.localeFor("xx"))
    }

    // ---------------- localized resources ----------------

    @Test
    fun `localizedContext gives Thai or English text whatever the phone is set to`() {
        val m = manager("en")

        assertEquals("Home", m.localizedContext(context, "en").getString(R.string.nav_home))
        assertEquals("หน้าหลัก", m.localizedContext(context, "th").getString(R.string.nav_home))
    }

    @Test
    fun `localizedContext follows the current language by default`() {
        val m = manager("en")
        assertEquals("Home", m.localizedContext(context).getString(R.string.nav_home))

        m.toggle()

        assertEquals("หน้าหลัก", m.localizedContext(context).getString(R.string.nav_home))
    }

    @Test
    fun `localizedContext fills in format arguments in both languages`() {
        val m = manager("en")

        assertEquals("Hello, Bob! 💪", m.localizedContext(context, "en").getString(R.string.f_hello, "Bob"))
        assertEquals("สวัสดี Bob! 💪", m.localizedContext(context, "th").getString(R.string.f_hello, "Bob"))
    }

    @Test
    fun `localizedContext leaves the original context alone`() {
        val before = context.resources.configuration.locales[0]

        manager("en").localizedContext(context, "th")

        assertEquals(before, context.resources.configuration.locales[0])
        assertEquals("Home", context.getString(R.string.nav_home))
    }

    @Test
    fun `every string resolves in both languages on a real Android resource table`() {
        val m = manager("en")
        val placeholder = Regex("%(\\d+)\\$([0-9.]*[sdf])")
        val failures = mutableListOf<String>()
        listOf("values" to "en", "values-th" to "th").forEach { (folder, code) ->
            val ctx = m.localizedContext(context, code)
            StringFiles.load(folder).forEach { (name, text) ->
                val id = ctx.resources.getIdentifier(name, "string", context.packageName)
                if (id == 0) { failures.add("$code/$name: no resource id"); return@forEach }
                val specs = placeholder.findAll(text).toList()
                val args = Array<Any>(specs.maxOfOrNull { it.groupValues[1].toInt() } ?: 0) { "x" }
                specs.forEach {
                    args[it.groupValues[1].toInt() - 1] = when {
                        it.groupValues[2].endsWith("d") -> 7
                        it.groupValues[2].endsWith("f") -> 7.5
                        else -> "x"
                    }
                }
                val real = if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
                val expected = if (args.isEmpty()) text else String.format(Locale.US, text, *args)
                if (real != expected) failures.add("$code/$name: Android gave '$real' but the file says '$expected'")
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `switching language changes the text of every screen title the user sees`() {
        val m = manager("en")
        val keys = listOf(
            R.string.nav_home, R.string.nav_routines, R.string.nav_workout, R.string.nav_nutrition, R.string.nav_profile,
            R.string.log_in, R.string.create_account, R.string.register, R.string.workout_summary, R.string.history
        )
        keys.forEach {
            assertNotEquals(
                "resource $it is the same in both languages",
                m.localizedContext(context, "en").getString(it),
                m.localizedContext(context, "th").getString(it)
            )
        }
        assertFalse(keys.isEmpty())
    }
}
