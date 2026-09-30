package com.example.glarmto

import com.example.glarmto.data.util.AssetProductDataset
import com.example.glarmto.data.util.NutritionPer100g
import com.example.glarmto.data.util.ProductDatasetParser
import com.example.glarmto.data.util.ProductRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ProductDatasetTest {

    private fun line(vararg fields: String) = fields.joinToString("\t")

    // ---------------- reading a line ----------------

    @Test
    fun `a full line becomes a record`() {
        val r = ProductDatasetParser.parseLine(line("8851717021008", "Dutchie yoghurt", "Dutchie", "125", "72", "5", "19", "0"))!!

        assertEquals("8851717021008", r.code)
        assertEquals("Dutchie yoghurt", r.name)
        assertEquals("Dutchie", r.brand)
        assertEquals(125.0, r.servingGrams!!, 0.0)
        assertEquals(NutritionPer100g(72.0, 5.0, 19.0, 0.0), r.nutrition)
    }

    @Test
    fun `a name only line has no nutrition`() {
        val r = ProductDatasetParser.parseLine(line("8851717021008", "Mystery", "", "", "", "", "", ""))!!

        assertNull(r.nutrition)
        assertNull(r.servingGrams)
    }

    @Test
    fun `a missing serving size is allowed`() {
        val r = ProductDatasetParser.parseLine(line("8851717021008", "Salt", "Brand", "", "0", "0", "0", "0"))!!

        assertNull(r.servingGrams)
        assertEquals(0.0, r.nutrition!!.kcal, 0.0)
    }

    @Test
    fun `malformed lines are skipped`() {
        listOf(
            "",
            "just one column",
            line("8851717021008", "Name"),
            line("", "Name", "", "", "1", "1", "1", "1"),
            line("ABC123", "Name", "", "", "1", "1", "1", "1"),
            line("8851717021008", "", "", "", "1", "1", "1", "1"),
            line("8851717021008", "Name", "", "", "abc", "1", "1", "1"),
            line("8851717021008", "Name", "", "", "100", "x", "1", "1"),
            line("8851717021008", "Name", "", "", "-5", "1", "1", "1"),
            line("8851717021008", "Name", "", "", "100", "1", "1")
        ).forEach { assertNull("should be skipped: '$it'", ProductDatasetParser.parseLine(it)) }
    }

    @Test
    fun `a zero serving size counts as unknown`() {
        assertNull(ProductDatasetParser.parseLine(line("8851717021008", "N", "", "0", "1", "1", "1", "1"))!!.servingGrams)
    }

    @Test
    fun `whitespace around fields is trimmed`() {
        val r = ProductDatasetParser.parseLine(line(" 8851717021008 ", " Name ", " Brand ", "", "1", "1", "1", "1"))!!

        assertEquals("8851717021008", r.code)
        assertEquals("Name", r.name)
        assertEquals("Brand", r.brand)
    }

    @Test
    fun `Thai text is read correctly`() {
        val r = ProductDatasetParser.parseLine(line("8850393800440", "นมเปรี้ยว บีทาเก้น", "บีทาเกน", "300", "60", "2", "13", "0"))!!

        assertEquals("นมเปรี้ยว บีทาเก้น (บีทาเกน)", r.displayName)
    }

    // ---------------- reading many lines ----------------

    @Test
    fun `blank and broken lines do not stop the parse`() {
        val records = ProductDatasetParser.parse(
            sequenceOf(
                line("8850000000001", "One", "", "", "1", "1", "1", "1"),
                "",
                "garbage",
                line("8850000000002", "Two", "", "", "2", "2", "2", "2")
            )
        )

        assertEquals(setOf("8850000000001", "8850000000002"), records.keys)
    }

    @Test
    fun `the first line for a code wins`() {
        val records = ProductDatasetParser.parse(
            sequenceOf(
                line("8850000000001", "Best ranked", "", "", "1", "1", "1", "1"),
                line("8850000000001", "Duplicate", "", "", "9", "9", "9", "9")
            )
        )

        assertEquals("Best ranked", records.getValue("8850000000001").name)
    }

    // ---------------- display name ----------------

    @Test
    fun `the brand is added to the name`() {
        assertEquals("Sour Cream & Onion (Pringles)", ProductRecord("1", "Sour Cream & Onion", "Pringles", null, null).displayName)
    }

    @Test
    fun `the brand is not repeated when the name already has it`() {
        assertEquals("Pringles Original", ProductRecord("1", "Pringles Original", "Pringles", null, null).displayName)
        assertEquals("pringles original", ProductRecord("1", "pringles original", "Pringles", null, null).displayName)
    }

    @Test
    fun `no brand means the name alone`() {
        assertEquals("Water", ProductRecord("1", "Water", "", null, null).displayName)
    }

    // ---------------- per serving or per 100 g ----------------

    private val per100 = NutritionPer100g(kcal = 72.0, protein = 5.0, carbs = 19.0, fat = 1.0)

    @Test
    fun `with a serving size the numbers are for one serving`() {
        val n = ProductRecord("1", "Yoghurt", "", 125.0, per100).toNutrition()

        assertEquals(90, n.calories)
        assertEquals(6, n.protein) // 6.25
        assertEquals(24, n.carbs) // 23.75
        assertEquals(1, n.fats) // 1.25
        assertFalse(n.per100g)
        assertTrue(n.nutritionKnown)
    }

    @Test
    fun `without a serving size the numbers stay per 100 g and are flagged`() {
        val n = ProductRecord("1", "Yoghurt", "", null, per100).toNutrition()

        assertEquals(72, n.calories)
        assertEquals(5, n.protein)
        assertTrue(n.per100g)
    }

    @Test
    fun `a product without nutrition reports that it is not known`() {
        val n = ProductRecord("1", "Mystery", "Brand", 30.0, null).toNutrition()

        assertFalse(n.nutritionKnown)
        assertEquals("Mystery (Brand)", n.productName)
        assertEquals(0, n.calories)
    }

    @Test
    fun `rounding is to the nearest whole number`() {
        val n = ProductRecord("1", "X", "", 30.0, NutritionPer100g(kcal = 351.0, protein = 0.0, carbs = 0.0, fat = 0.0)).toNutrition()

        assertEquals(105, n.calories) // 105.3
    }

    // ---------------- the asset stream ----------------

    private fun dataset(text: String) = AssetProductDataset { ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)) }

    @Test
    fun `the dataset finds products from a stream`() = runBlocking {
        val data = dataset(
            line("8851717021008", "Dutchie", "Dutchie", "125", "72", "5", "19", "0") + "\n" +
                line("8850228007716", "Red Bull Soda", "Red Bull", "250", "6", "0", "1.6", "0") + "\n"
        )

        assertEquals(2, data.find(listOf("8850228007716"))!!.let { 2 })
        assertEquals(90, data.find(listOf("8851717021008"))!!.calories)
        assertEquals(15, data.find(listOf("8850228007716"))!!.calories)
        assertEquals(2, data.size)
    }

    @Test
    fun `an unknown barcode is not found`() = runBlocking {
        assertNull(dataset(line("8851717021008", "Dutchie", "", "", "1", "1", "1", "1") + "\n").find(listOf("9999999999999")))
    }

    @Test
    fun `any of the candidate spellings can match`() = runBlocking {
        val data = dataset(line("012000001086", "Imported", "", "", "50", "0", "10", "0") + "\n")

        assertNotNull(data.find(listOf("0012000001086", "012000001086")))
    }

    @Test
    fun `an empty dataset finds nothing`() = runBlocking {
        val data = dataset("")

        assertNull(data.find(listOf("8851717021008")))
        assertEquals(0, data.size)
    }

    @Test
    fun `the stream is only opened once however many lookups happen`() = runBlocking {
        var opened = 0
        val data = AssetProductDataset {
            opened++
            ByteArrayInputStream((line("8851717021008", "Dutchie", "", "", "1", "1", "1", "1") + "\n").toByteArray())
        }

        repeat(5) { data.find(listOf("8851717021008")) }

        assertEquals(1, opened)
    }

    @Test
    fun `Thai names come back intact`() = runBlocking {
        val data = dataset(line("8850393800440", "นมเปรี้ยว", "บีทาเกน", "300", "60", "2", "13", "0") + "\n")

        assertEquals("นมเปรี้ยว (บีทาเกน)", data.find(listOf("8850393800440"))!!.productName)
    }
}
