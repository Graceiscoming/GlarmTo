package com.example.glarmto

import com.example.glarmto.data.util.AssetProductDataset
import com.example.glarmto.data.util.ProductDatasetParser
import com.example.glarmto.data.util.ProductRecord
import com.example.glarmto.testsupport.StringFiles
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the real bundled file (`assets/thai_products.tsv`, from Open Food Facts): that it is there, is
 * well formed, is big enough to be useful, holds believable numbers, and finds real Thai products.
 */
class ThaiProductsAssetTest {

    private val assetDir: File by lazy { File(StringFiles.dir("values").parentFile.parentFile, "assets") } // .../src/main/assets
    private val asset: File get() = File(assetDir, "thai_products.tsv")

    private val lines: List<String> by lazy { asset.readLines(Charsets.UTF_8) }
    private val records: List<ProductRecord> by lazy { lines.mapNotNull { ProductDatasetParser.parseLine(it) } }
    private val dataset by lazy { AssetProductDataset { asset.inputStream() } }

    @Test
    fun `the file exists and is small enough to ship`() {
        assertTrue("missing: ${asset.absolutePath}", asset.isFile)
        assertTrue("file is ${asset.length()} bytes", asset.length() in 200_000..6_000_000)
    }

    @Test
    fun `there are thousands of products, and thousands with nutrition`() {
        assertTrue("only ${records.size} products", records.size >= 10_000)
        assertTrue("only ${records.count { it.nutrition != null }} with nutrition", records.count { it.nutrition != null } >= 4_000)
    }

    @Test
    fun `every line in the file is valid`() {
        val bad = lines.withIndex().filter { ProductDatasetParser.parseLine(it.value) == null }.map { it.index + 1 }

        assertTrue("invalid lines: ${bad.take(10)}", bad.isEmpty())
    }

    @Test
    fun `barcodes are unique digits of a known length`() {
        assertEquals("duplicate barcodes", records.size, records.map { it.code }.toSet().size)
        assertTrue(records.all { it.code.length in setOf(8, 12, 13, 14) })
    }

    @Test
    fun `most products are Thai, with the 885 barcode prefix`() {
        val thai = records.count { it.code.startsWith("885") }

        assertTrue("only $thai of ${records.size} start with 885", thai * 10 >= records.size * 6)
    }

    @Test
    fun `nutrition numbers are believable`() {
        records.mapNotNull { it.nutrition?.let { n -> it to n } }.forEach { (r, n) ->
            assertTrue("${r.code} kcal ${n.kcal}", n.kcal in 0.0..950.0)
            listOf(n.protein, n.carbs, n.fat).forEach { assertTrue("${r.code} macro $it", it in 0.0..100.0) }
            assertTrue("${r.code} macros add up to ${n.protein + n.carbs + n.fat} g per 100 g", n.protein + n.carbs + n.fat <= 105.0)
        }
    }

    @Test
    fun `serving sizes are believable`() {
        records.mapNotNull { it.servingGrams }.forEach { assertTrue("serving $it g", it in 3.0..2000.0) }
    }

    @Test
    fun `names are readable`() {
        records.forEach {
            assertTrue("${it.code} name '${it.name}'", it.name.length in 2..90)
            assertFalse("${it.code} has a tab or newline", it.name.any { c -> c == '\t' || c == '\n' || c == '\r' })
        }
    }

    @Test
    fun `there is Thai text in the names`() {
        val thaiChar = Regex("[฀-๿]")

        assertTrue(records.count { thaiChar.containsMatchIn(it.name) } >= 1_500)
    }

    @Test
    fun `products with nutrition come first`() {
        val firstBlock = records.take(3_000).count { it.nutrition != null }

        assertTrue("only $firstBlock of the first 3000 have nutrition", firstBlock >= 2_900)
    }

    // ---------------- real products ----------------

    private fun find(code: String) = runBlocking { dataset.find(listOf(code)) }

    @Test
    fun `finds a real Thai yoghurt with its serving size`() {
        val dutchie = find("8851717021008")

        assertNotNull(dutchie)
        assertTrue(dutchie!!.productName.contains("Dutchie", ignoreCase = true))
        assertEquals("72 kcal per 100 g x 125 g serving", 90, dutchie.calories)
        assertFalse(dutchie.per100g)
    }

    @Test
    fun `finds a real energy drink and a real sauce`() {
        assertEquals(15, find("8850228007716")!!.calories) // Red Bull Soda, 6 kcal/100 g x 250 g
        assertEquals(20, find("8850343000166")!!.calories) // Heinz tomato ketchup, 133 kcal/100 g x 15 g
    }

    @Test
    fun `a Thai name comes back readable`() {
        assertTrue(find("8850393800440")!!.productName.contains("นมเปรี้ยว"))
    }

    @Test
    fun `an imported product sold in Thailand is there too`() {
        assertNotNull(find("8076800195057")) // Barilla spaghetti
    }

    @Test
    fun `a made up barcode is not found`() {
        assertNull(find("8859999999999"))
        assertNull(find("1234567890128"))
    }

    @Test
    fun `the old placeholder barcodes are gone`() {
        // The previous built in list used invented barcodes such as these; none of them is a real product.
        listOf("8850987123456", "8852000000001", "8851111100001", "8853331100001").forEach { assertNull(it, find(it)) }
    }

    @Test
    fun `the file is plain text - the Android build unzips and renames gz assets`() {
        assertFalse(File(assetDir, "thai_products.tsv.gz").exists())
        assertTrue(asset.readBytes().take(2) != listOf(0x1f.toByte(), 0x8b.toByte()))
    }

    @Test
    fun `the license notice ships with the data`() {
        val notice = File(assetDir, "OPEN_FOOD_FACTS_NOTICE.txt")

        assertTrue("missing ${notice.absolutePath}", notice.isFile)
        val text = notice.readText()
        assertTrue(text.contains("Open Food Facts"))
        assertTrue(text.contains("Open Database License") || text.contains("ODbL"))
        assertTrue(text.contains("https://world.openfoodfacts.org"))
    }
}
