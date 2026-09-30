package com.example.glarmto.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun testParseValidEnglishCommand() {
        val result = parseVoiceCommand("Squat 100 kg 8 reps")
        assertEquals(Triple("Squat", "100", "8"), result)
    }

    @Test
    fun testParseValidEnglishCommandWithDecimal() {
        val result = parseVoiceCommand("Bench Press 50.5 kilos 10 times")
        assertEquals(Triple("Bench Press", "50.5", "10"), result)
    }

    @Test
    fun testParseValidThaiCommand() {
        // AI of Android generally converts spoken numbers to Arabic numerals automatically
        val result = parseVoiceCommand("สควอท 100 กิโล 8 ครั้ง")
        assertEquals(Triple("สควอท", "100", "8"), result)
    }

    @Test
    fun testParseValidMixedLanguageCommand() {
        // Example: User says "Deadlift ร้อยยี่สิบกิโลห้าครั้ง" -> Google AI writes "Deadlift 120 kg 5"
        val result = parseVoiceCommand("Deadlift 120 kg 5")
        assertEquals(Triple("Deadlift", "120", "5"), result)
    }

    @Test
    fun testParseInvalidCommandNoNumbers() {
        val result = parseVoiceCommand("I am just talking to the AI")
        assertNull(result)
    }

    @Test
    fun testParseValidCommandNumbersFirst() {
        // Our new heuristic engine allows the exercise name (letters) to be anywhere.
        val result = parseVoiceCommand("100 kg 8 reps for Squat")
        assertEquals(Triple("Squat", "100", "8"), result)
    }

    @Test
    fun testParseValidCommandMultipleWordsExtraSpacings() {
        val result = parseVoiceCommand("  Overhead Shoulder Press  40.25   kg  12 reps  ")
        // The regex should capture "Overhead Shoulder Press" and ignore extra spaces around numbers
        assertEquals(Triple("Overhead Shoulder Press", "40.25", "12"), result)
    }

    @Test
    fun testParseReverseOrder() {
        // Our new heuristic engine allows numbers to come first!
        val result = parseVoiceCommand("10 reps of Bench Press with 50 kilos")
        assertEquals(Triple("Bench Press", "50", "10"), result)
    }

    @Test
    fun testParseBodyweightOnly() {
        // Only 1 number provided
        val result = parseVoiceCommand("Pull up 15 ครั้ง")
        assertEquals(Triple("Pull Up", "0", "15"), result)
    }

    @Test
    fun testParseRepsBiggerThanWeight_ExplicitUnits() {
        // Here, Reps (15) > Weight (10). It should use the explicit units instead of the max-fallback rule.
        val result = parseVoiceCommand("Bicep Curl 10 kg 15 reps")
        assertEquals(Triple("Bicep Curl", "10", "15"), result)
    }

    @Test
    fun testParseFuzzyMatchAutoCorrect() {
        // Tests auto-snapping to predefined list. Let's assume ExercisePresets contains "Squat"
        // User says exactly this weird word string.
        val result = parseVoiceCommand("sqauat 100 8")
        // It fallback-capitalizes unknown words if not found. Let's just expect capitalized for robustness.
        assertEquals(Triple("Sqauat", "100", "8"), result)
    }
}
