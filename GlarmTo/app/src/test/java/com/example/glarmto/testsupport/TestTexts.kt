package com.example.glarmto.testsupport

import com.example.glarmto.data.util.AppTexts
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** Reads the real `strings.xml` files from the source tree, so JVM tests can check the actual wording. */
object StringFiles {
    private val moduleDirs = listOf(File("."), File("app"), File("GlarmTo/app"))

    fun dir(name: String): File {
        val candidates = moduleDirs.map { File(it, "src/main/res/$name") }
        return candidates.firstOrNull { it.isDirectory }
            ?: error("res/$name not found; looked in ${candidates.map { it.absolutePath }}")
    }

    fun sourceRoot(): File {
        val candidates = moduleDirs.map { File(it, "src/main/java") }
        return candidates.firstOrNull { it.isDirectory } ?: error("src/main/java not found")
    }

    /** name -> text exactly as Android would resolve it (escapes like \' and \n undone). */
    fun load(valuesFolder: String): Map<String, String> {
        val file = File(dir(valuesFolder), "strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val result = LinkedHashMap<String, String>()
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as Element
            val name = e.getAttribute("name")
            check(name !in result) { "duplicate string name '$name' in $valuesFolder" }
            result[name] = unescape(collapseWhitespace(e.textContent))
        }
        return result
    }

    /** Names in file order, including duplicates (used to report them instead of failing on the first). */
    fun names(valuesFolder: String): List<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(dir(valuesFolder), "strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("name") }
    }

    /** Unquoted strings lose leading/trailing whitespace and runs of whitespace become one space (aapt's rule). */
    fun collapseWhitespace(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    fun unescape(raw: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (val n = raw[i + 1]) {
                    'n' -> { out.append('\n'); i += 2 }
                    't' -> { out.append('\t'); i += 2 }
                    'u' -> {
                        out.append(raw.substring(i + 2, i + 6).toInt(16).toChar()); i += 6
                    }
                    else -> { out.append(n); i += 2 }
                }
            } else {
                out.append(c); i++
            }
        }
        return out.toString()
    }
}

/**
 * [AppTexts] for JVM tests that resolves `R.string.*` ids against the real strings.xml of a language,
 * formatting like Android does: only strings used with arguments are run through String.format.
 */
class XmlTexts(private val valuesFolder: String) : AppTexts {
    private val byName = StringFiles.load(valuesFolder)
    private val nameById: Map<Int, String> by lazy {
        Class.forName("com.example.glarmto.R\$string").fields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate { it.getInt(null) to it.name }
    }

    override fun get(id: Int, vararg args: Any): String {
        val name = nameById[id] ?: error("no string resource with id $id")
        val text = byName[name] ?: error("string '$name' is missing in $valuesFolder")
        return if (args.isEmpty()) text else String.format(Locale.US, text, *args)
    }
}

object TestTexts {
    val english: AppTexts by lazy { XmlTexts("values") }
    val thai: AppTexts by lazy { XmlTexts("values-th") }
}
