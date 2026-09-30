package com.example.glarmto

import com.example.glarmto.testsupport.StringFiles
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A guard so text that a user reads never goes back to being typed straight into the code: it would
 * silently stay English when the language is switched to Thai.
 */
class NoHardcodedUiTextTest {

    // Files that build UI text; theme code and the camera/context plumbing carry no user-facing words.
    private fun uiFiles(): List<File> =
        File(StringFiles.sourceRoot(), "com/example/glarmto/ui").walkTopDown()
            .filter { it.extension == "kt" }
            .filter { "/theme/" !in it.invariantSeparatorsPath }
            .toList()

    private val literal = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
    private val templatePart = Regex("\\$\\{[^}]*}|\\$[A-Za-z_][A-Za-z0-9_]*")

    /** True if the literal contains words (ignoring ${...} / $name parts). */
    private fun hasWords(text: String): Boolean =
        templatePart.replace(text, "").any { it.isLetter() }

    private val visibleCall = listOf(
        Regex("\\bText\\("),
        Regex("contentDescription\\s*=\\s*\""),
        Regex("\\b(errorMessage|message)\\s*=\\s*\"")
    )

    @Test
    fun `no Text or contentDescription in the UI is built from a typed string`() {
        val offenders = mutableListOf<String>()
        uiFiles().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("import ")) return@forEachIndexed
                if (visibleCall.none { it.containsMatchIn(line) }) return@forEachIndexed
                if ("stringResource(" in line && literal.findAll(line).none { hasWords(it.groupValues[1]) }) return@forEachIndexed
                literal.findAll(line).forEach { m ->
                    if (hasWords(m.groupValues[1])) offenders.add("${file.name}:${index + 1}: ${trimmed.take(110)}")
                }
            }
        }
        assertTrue("typed UI text that should be a string resource:\n" + offenders.distinct().joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun `messages built in view models and helpers come from resources too`() {
        val root = File(StringFiles.sourceRoot(), "com/example/glarmto")
        val files = root.walkTopDown().filter {
            it.extension == "kt" && (it.name.endsWith("ViewModel.kt") || it.name in setOf("WorkoutGenerator.kt", "InstagramShareHelper.kt", "GlarmToWidget.kt"))
        }.toList()
        val patterns = listOf(
            Regex("_smart(Suggestion|Recommendation)\\.value\\s*=\\s*\""),
            Regex("MutableStateFlow<String>\\(\""),
            Regex("\\b(warnings|insights)\\.add\\(\""),
            Regex("createChooser\\([^\\n]*,\\s*\""),
            Regex("EXTRA_SUBJECT,\\s*\""),
            Regex("\\btitle\\s*=\\s*\"")
        )
        val drawnText = Regex("drawText\\(\"((?:[^\"\\\\]|\\\\.)*)\"")
        val offenders = mutableListOf<String>()
        files.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.trim().startsWith("//") || line.trim().startsWith("*")) return@forEachIndexed
                val typedDrawText = drawnText.find(line)?.let { hasWords(it.groupValues[1]) } ?: false
                if (typedDrawText || patterns.any { it.containsMatchIn(line) }) {
                    offenders.add("${file.name}:${index + 1}: ${line.trim().take(110)}")
                }
            }
        }
        assertTrue("typed user-facing text outside the screens:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun `every screen and helper that shows text imports the resources it uses`() {
        // A screen that calls stringResource must be using the app's R, not some library's.
        val offenders = uiFiles().filter { "stringResource(R.string." in it.readText() && "import com.example.glarmto.R" !in it.readText() }
        assertTrue("files using R.string without importing the app's R: ${offenders.map { it.name }}", offenders.isEmpty())
    }

    @Test
    fun `text is not assigned from typed strings to UI state`() {
        val offenders = mutableListOf<String>()
        val state = Regex("\\b(feedbackText|errorMessage|statusText|hintText)\\s*=\\s*\"")
        uiFiles().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (state.containsMatchIn(line)) offenders.add("${file.name}:${index + 1}: ${line.trim().take(110)}")
            }
        }
        assertTrue("typed UI text:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }
}
