package com.example.glarmto

import com.example.glarmto.testsupport.StringFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** The English and Thai string files must describe the same screens, with the same placeholders. */
class LocalizationResourcesTest {

    private val en by lazy { StringFiles.load("values") }
    private val th by lazy { StringFiles.load("values-th") }
    private val placeholder = Regex("%(\\d+)\\$([0-9.]*[sdf])")
    private val thaiChar = Regex("[฀-๿]")

    /** Strings that are legitimately identical in both languages (brand names, units, symbols). */
    private val sameInBothLanguages = setOf(
        "app_name", "ig_brand", "widget_title",
        "f_kg", "f_kcal", "f_ml", "f_ml_2", "f_ml_3", "f_xp", "f_bmi", "n_250_ml", "n_500_ml",
        "f_kg_x_reps_dummy"
    )

    private fun placeholdersOf(text: String) =
        placeholder.findAll(text).map { it.groupValues[1] + "$" + it.groupValues[2] }.sorted().toList()

    @Test
    fun `both files define the same set of strings`() {
        val onlyEnglish = en.keys - th.keys
        val onlyThai = th.keys - en.keys

        assertTrue("missing from values-th: $onlyEnglish", onlyEnglish.isEmpty())
        assertTrue("missing from values: $onlyThai", onlyThai.isEmpty())
    }

    @Test
    fun `no string name is defined twice in either file`() {
        listOf("values", "values-th").forEach { folder ->
            val names = StringFiles.names(folder)
            val duplicates = names.groupBy { it }.filter { it.value.size > 1 }.keys
            assertTrue("duplicates in $folder: $duplicates", duplicates.isEmpty())
        }
    }

    @Test
    fun `string names are valid resource names`() {
        en.keys.forEach { assertTrue("bad name: $it", it.matches(Regex("[a-z][a-z0-9_]*"))) }
    }

    @Test
    fun `every string has text in both languages`() {
        en.forEach { (k, v) -> assertTrue("empty English text for $k", v.isNotBlank()) }
        th.forEach { (k, v) -> assertTrue("empty Thai text for $k", v.isNotBlank()) }
    }

    @Test
    fun `placeholders match between English and Thai for every string`() {
        en.forEach { (key, english) ->
            assertEquals("placeholders of '$key' differ", placeholdersOf(english), placeholdersOf(th.getValue(key)))
        }
    }

    @Test
    fun `placeholder numbers are contiguous starting at 1`() {
        (en + th.mapKeys { "th:" + it.key }).forEach { (key, text) ->
            val numbers = placeholder.findAll(text).map { it.groupValues[1].toInt() }.toSet()
            if (numbers.isNotEmpty()) {
                assertEquals("placeholder numbering in $key", (1..numbers.max()).toSet(), numbers)
            }
        }
    }

    @Test
    fun `every string with placeholders formats without error in both languages`() {
        listOf("values" to en, "values-th" to th).forEach { (folder, map) ->
            map.forEach { (key, text) ->
                val specs = placeholder.findAll(text).toList()
                if (specs.isEmpty()) return@forEach
                val args = Array<Any>(specs.maxOf { it.groupValues[1].toInt() }) { "x" }
                specs.forEach {
                    args[it.groupValues[1].toInt() - 1] = when {
                        it.groupValues[2].endsWith("d") -> 7
                        it.groupValues[2].endsWith("f") -> 7.5
                        else -> "x"
                    }
                }
                try {
                    String.format(Locale.US, text, *args)
                } catch (e: Exception) {
                    throw AssertionError("$folder/$key does not format: $text", e)
                }
            }
        }
    }

    @Test
    fun `strings without placeholders have no stray format markers`() {
        (en + th.mapKeys { "th:" + it.key }).forEach { (key, text) ->
            if (!placeholder.containsMatchIn(text)) {
                assertTrue("'$key' has a %% but no placeholder: $text", !text.contains("%%"))
            }
        }
    }

    @Test
    fun `Thai strings are actually Thai unless they are brand names, units or symbols`() {
        th.forEach { (key, text) ->
            if (key in sameInBothLanguages) return@forEach
            if (en[key] == text) {
                // identical text is only fine when there is nothing to translate
                val letters = en.getValue(key).replace(placeholder, "").filter { it.isLetter() }
                assertTrue("'$key' is identical in Thai but has translatable text: $text", letters.length <= 3)
            } else {
                assertTrue("'$key' has no Thai characters: $text", thaiChar.containsMatchIn(text))
            }
        }
    }

    @Test
    fun `English strings contain no Thai and Thai strings keep their English placeholders`() {
        en.forEach { (key, text) -> assertTrue("'$key' has Thai in the English file", !thaiChar.containsMatchIn(text)) }
    }

    @Test
    fun `no placeholder text was left behind`() {
        (en + th.mapKeys { "th:" + it.key }).forEach { (key, text) ->
            assertTrue("'$key' looks unfinished: $text", !text.contains("TODO", ignoreCase = true) && !text.contains("FIXME"))
        }
    }

    @Test
    fun `every string is used by the app`() {
        val source = StringFiles.sourceRoot().walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        val unused = en.keys.filter { it != "app_name" && !source.contains("R.string.$it") }

        assertTrue("strings no code refers to: $unused", unused.isEmpty())
    }

    @Test
    fun `the language switch strings exist`() {
        assertEquals("Switch language", en["switch_language"])
        assertEquals("เปลี่ยนภาษา", th["switch_language"])
    }
}
