package com.example.glarmto

import com.example.glarmto.data.util.NutritionOcrParser
import org.junit.Assert.assertEquals
import org.junit.Test

class MLKitWowFactorsTest {

    @Test
    fun testNutritionOcrParser_extractsMacrosCorrectly() {
        val rawTextFromCamera = """
            Nutrition Facts
            Serving Size 100g
            
            Energy 250 kcal
            Fat 12 g
            Protein 20g
            Carb 15 g
        """.trimIndent()

        val parsed = NutritionOcrParser.parseNutritionFromLabel(rawTextFromCamera)

        assertEquals("Calories should be extracted correctly", 250, parsed.calories)
        assertEquals("Protein should be extracted correctly", 20, parsed.protein)
        assertEquals("Carbs should be extracted correctly", 15, parsed.carbs)
        assertEquals("Fats should be extracted correctly", 12, parsed.fats)
    }

    @Test
    fun testNutritionOcrParser_handlesMessyAndDistortedText() {
        val messyText = "enErGy   310kcal ... protEIN : 25g \n CAB 40 fat 5"

        val parsed = NutritionOcrParser.parseNutritionFromLabel(messyText)

        assertEquals("Calories extracted from messy text", 310, parsed.calories)
        assertEquals("Protein extracted from messy text", 25, parsed.protein)
        assertEquals("Fats extracted from messy text", 5, parsed.fats)
    }

    @Test
    fun testNutritionOcrParser_handlesMissingValues() {
        val incompleteText = "Just some random text with Protein 10g but no calories"

        val parsed = NutritionOcrParser.parseNutritionFromLabel(incompleteText)

        assertEquals("Calories should default to 0 if not found", 0, parsed.calories)
        assertEquals("Protein should be found", 10, parsed.protein)
        assertEquals("Carbs should default to 0", 0, parsed.carbs)
    }
}
