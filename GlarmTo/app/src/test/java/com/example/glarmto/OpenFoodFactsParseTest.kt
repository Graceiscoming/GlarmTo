package com.example.glarmto

import android.os.Build
import com.example.glarmto.data.util.OpenFoodFactsApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** How an Open Food Facts answer becomes calories and macros. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class OpenFoodFactsParseTest {

    private fun response(
        nutriments: String,
        status: Int = 1,
        name: String = "Choc",
        extra: String = ""
    ) = JSONObject("""{"status":$status,"product":{"product_name":"$name"$extra,"nutriments":{$nutriments}}}""")

    // ---------------- which values are used ----------------

    @Test
    fun `per serving values are used when available`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_serving":200,"proteins_serving":5,"carbohydrates_serving":30,"fat_serving":8,"energy-kcal_100g":500""")
        )

        assertNotNull(parsed)
        assertEquals("Choc", parsed!!.productName)
        assertEquals(200, parsed.calories)
        assertEquals(5, parsed.protein)
        assertEquals(30, parsed.carbs)
        assertEquals(8, parsed.fats)
        assertFalse(parsed.per100g)
    }

    @Test
    fun `per 100g values are scaled to one serving when the serving size is known`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_100g":500,"proteins_100g":10,"carbohydrates_100g":60,"fat_100g":25""",
                extra = ""","serving_quantity":"40"""")
        )!!

        assertEquals(200, parsed.calories)
        assertEquals(4, parsed.protein)
        assertEquals(24, parsed.carbs)
        assertEquals(10, parsed.fats)
        assertFalse("these are real per-serving numbers now", parsed.per100g)
    }

    @Test
    fun `per 100g values are returned as they are, and flagged, when the serving size is unknown`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_100g":500,"proteins_100g":10,"carbohydrates_100g":60,"fat_100g":25""")
        )!!

        assertEquals(500, parsed.calories)
        assertEquals(25, parsed.fats)
        assertTrue(parsed.per100g)
        assertEquals("the name is left alone - the screen adds the wording", "Choc", parsed.productName)
    }

    @Test
    fun `serving and 100g values are never mixed`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy-kcal_serving":200,"proteins_100g":10"""))!!

        assertEquals(200, parsed.calories)
        assertEquals("Protein only has a 100g value, which is not comparable", 0, parsed.protein)
    }

    @Test
    fun `a nonsense serving size is ignored`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_100g":100""", extra = ""","serving_quantity":"0"""")
        )!!

        assertEquals(100, parsed.calories)
        assertTrue(parsed.per100g)
    }

    // ---------------- energy fallbacks ----------------

    @Test
    fun `calories come from kilojoules when kcal is missing`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy-kj_100g":1046,"proteins_100g":2"""))!!

        assertEquals(250, parsed.calories)
    }

    @Test
    fun `the plain energy field is read as kilojoules`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy_100g":418.4"""))!!

        assertEquals(100, parsed.calories)
    }

    @Test
    fun `kcal wins over kilojoules`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy-kcal_100g":300,"energy-kj_100g":9999"""))!!

        assertEquals(300, parsed.calories)
    }

    @Test
    fun `calories are worked out from the macros when no energy is listed`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""proteins_100g":10,"carbohydrates_100g":20,"fat_100g":5"""))!!

        assertEquals(4 * 10 + 4 * 20 + 9 * 5, parsed.calories)
    }

    @Test
    fun `kilojoules per serving are converted too`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy_serving":836.8,"proteins_serving":3"""))!!

        assertEquals(200, parsed.calories)
        assertFalse(parsed.per100g)
    }

    @Test
    fun `scaling uses the energy fallback as well`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kj_100g":2092""", extra = ""","serving_quantity":30""")
        )!!

        assertEquals(150, parsed.calories)
    }

    // ---------------- names ----------------

    @Test
    fun `the Thai name is preferred`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_100g":10""", extra = ""","product_name_th":"นมเปรี้ยว"""")
        )!!

        assertEquals("นมเปรี้ยว", parsed.productName)
    }

    @Test
    fun `a missing name comes back empty so the screen can name it`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy-kcal_100g":10""", name = "   "))!!

        assertEquals("", parsed.productName)
    }

    // ---------------- nothing usable ----------------

    @Test
    fun `unknown product or missing nutriments gives null`() {
        assertNull(OpenFoodFactsApi.parseProduct(response(""""energy-kcal_100g":1""", status = 0)))
        assertNull(OpenFoodFactsApi.parseProduct(JSONObject("""{"status":1,"product":{"product_name":"X"}}""")))
        assertNull(OpenFoodFactsApi.parseProduct(JSONObject("""{"status":1}""")))
        assertNull(OpenFoodFactsApi.parseProduct(JSONObject("""{}""")))
    }

    @Test
    fun `a product with a name but no nutrition facts is known by name only - not as zero calories`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""))!!

        assertEquals("Choc", parsed.productName)
        assertFalse("zeros here mean 'not known', and must not be saved as a real meal", parsed.nutritionKnown)
        assertEquals(0, parsed.calories)
    }

    @Test
    fun `nutriments holding nothing useful also give a name only result`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""nutrition-score-fr_100g":12,"salt_100g":1.2"""))!!

        assertFalse(parsed.nutritionKnown)
    }

    @Test
    fun `no nutrition and no name is nothing at all`() {
        assertNull(OpenFoodFactsApi.parseProduct(response("", name = "   ")))
    }

    @Test
    fun `a product with nutrition is marked as known`() {
        assertTrue(OpenFoodFactsApi.parseProduct(response(""""energy-kcal_100g":10"""))!!.nutritionKnown)
    }

    @Test
    fun `extreme values do not crash`() {
        val parsed = OpenFoodFactsApi.parseProduct(response(""""energy-kcal_100g":1e9,"proteins_100g":-5"""))

        assertNotNull(parsed)
    }
}
