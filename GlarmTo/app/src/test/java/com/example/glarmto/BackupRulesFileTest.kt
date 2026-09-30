package com.example.glarmto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The database holds password hashes and all training data. Automatic backup is off in the manifest;
 * these rules are a second line of defence so nothing is copied if someone turns it back on.
 */
class BackupRulesFileTest {

    private fun resFile(name: String): File {
        val candidates = listOf(
            File("src/main/res/xml/$name"),
            File("app/src/main/res/xml/$name"),
            File("GlarmTo/app/src/main/res/xml/$name")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("$name not found; looked in ${candidates.map { it.absolutePath }}")
    }

    private fun sectionExcludes(file: File, section: String?): Set<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val scope: Element = if (section == null) doc.documentElement
        else doc.getElementsByTagName(section).item(0) as Element
        val excludes = scope.getElementsByTagName("exclude")
        return (0 until excludes.length).map {
            val e = excludes.item(it) as Element
            e.getAttribute("domain") to e.getAttribute("path")
        }.toSet()
    }

    @Test
    fun `full backup rules exclude the database and preferences`() {
        val excludes = sectionExcludes(resFile("backup_rules.xml"), null)

        assertTrue(excludes.contains("database" to "."))
        assertTrue(excludes.contains("sharedpref" to "."))
    }

    @Test
    fun `cloud backup excludes the database and preferences`() {
        val excludes = sectionExcludes(resFile("data_extraction_rules.xml"), "cloud-backup")

        assertTrue(excludes.contains("database" to "."))
        assertTrue(excludes.contains("sharedpref" to "."))
    }

    @Test
    fun `device transfer excludes the database and preferences`() {
        val excludes = sectionExcludes(resFile("data_extraction_rules.xml"), "device-transfer")

        assertTrue(excludes.contains("database" to "."))
        assertTrue(excludes.contains("sharedpref" to "."))
    }

    @Test
    fun `the rules files are well formed and have the expected roots`() {
        val backup = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resFile("backup_rules.xml"))
        val extraction = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resFile("data_extraction_rules.xml"))

        assertEquals("full-backup-content", backup.documentElement.tagName)
        assertEquals("data-extraction-rules", extraction.documentElement.tagName)
    }
}
