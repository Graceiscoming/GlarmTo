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

    @Test
    fun testNutritionOcrParser_prefersKcalOverKj() {
        val parsed = NutritionOcrParser.parseNutritionFromLabel("Energy 1200 kJ / 290 kcal\nProtein 10g")

        assertEquals(290, parsed.calories)
    }

    @Test
    fun testNutritionOcrParser_convertsKjWhenNoKcalGiven() {
        val parsed = NutritionOcrParser.parseNutritionFromLabel("Energy 1046 kJ")

        assertEquals(250, parsed.calories)
    }

    @Test
    fun testNutritionOcrParser_usesTotalFatNotSaturatedFat() {
        val parsed = NutritionOcrParser.parseNutritionFromLabel("Saturated fat 2g\nTotal fat 10g")

        assertEquals(10, parsed.fats)
    }

    @Test
    fun testNutritionOcrParser_ignoresCaloriesFromFat() {
        val parsed = NutritionOcrParser.parseNutritionFromLabel("Calories from fat 30\nCalories 250\nFat 12g")

        assertEquals(250, parsed.calories)
        assertEquals(12, parsed.fats)
    }

    @Test
    fun testNutritionOcrParser_missingValueDoesNotBorrowNextRowsNumber() {
        val parsed = NutritionOcrParser.parseNutritionFromLabel("Protein\nCarbs 30")

        assertEquals("Protein has no number of its own", 0, parsed.protein)
        assertEquals(30, parsed.carbs)
    }

    @Test
    fun testNutritionOcrParser_roundsDecimals() {
        val parsed = NutritionOcrParser.parseNutritionFromLabel("Protein 5.6g")

        assertEquals(6, parsed.protein)
    }
}
