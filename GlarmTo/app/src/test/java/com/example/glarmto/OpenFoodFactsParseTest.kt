package com.example.glarmto

import android.os.Build
import com.example.glarmto.data.util.OpenFoodFactsApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class OpenFoodFactsParseTest {

    private fun response(nutriments: String, status: Int = 1) =
        JSONObject("""{"status":$status,"product":{"product_name":"Choc","nutriments":{$nutriments}}}""")

    @Test
    fun `per serving values are used when available`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_serving":200,"proteins_serving":5,"carbohydrates_serving":30,"fat_serving":8,"energy-kcal_100g":500""")
        )

        assertNotNull(parsed)
        assertEquals("Choc", parsed!!.productName)
        assertEquals(200, parsed.calories)
        assertEquals(5, parsed.protein)
    }

    @Test
    fun `per 100g fallback is labelled in the product name`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_100g":500,"proteins_100g":10,"carbohydrates_100g":60,"fat_100g":25""")
        )!!

        assertEquals("Choc (per 100g)", parsed.productName)
        assertEquals(500, parsed.calories)
        assertEquals(25, parsed.fats)
    }

    @Test
    fun `serving and 100g values are never mixed`() {
        val parsed = OpenFoodFactsApi.parseProduct(
            response(""""energy-kcal_serving":200,"proteins_100g":10""")
        )!!

        assertEquals(200, parsed.calories)
        assertEquals("Protein only has a 100g value, which is not comparable", 0, parsed.protein)
    }

    @Test
    fun `unknown product or missing nutriments gives null`() {
        assertNull(OpenFoodFactsApi.parseProduct(response(""""energy-kcal_100g":1""", status = 0)))
        assertNull(OpenFoodFactsApi.parseProduct(JSONObject("""{"status":1,"product":{"product_name":"X"}}""")))
    }
}
