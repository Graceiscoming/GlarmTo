package com.example.glarmto.data.util

import kotlin.math.roundToInt

object NutritionOcrParser {

    private const val KJ_PER_KCAL = 4.184
    private const val NUMBER = "(\\d+(?:\\.\\d+)?)"

    // Sub-rows of "fat" / "calories" that would otherwise be mistaken for the headline value.
    private val subRowRegex = Regex(
        "(?i)(?:saturated|trans|poly\\w*|mono\\w*)\\s*fat|calories\\s*from\\s*fat|ไขมัน(?:อิ่มตัว|ทรานส์)"
    )

    // รองรับทั้งฉลากภาษาอังกฤษ และฉลากโภชนาการภาษาไทย
    private val proteinRegex = Regex("(?i)(?:protein|โปรตีน)\\D{0,20}?$NUMBER")
    private val carbsRegex = Regex("(?i)(?:carb|คาร์โบไฮเดรต|คาร์บ)\\D{0,20}?$NUMBER")
    private val totalFatRegex = Regex("(?i)(?:total\\s*fat|ไขมันทั้งหมด)\\D{0,20}?$NUMBER")
    private val fatRegex = Regex("(?i)(?:fat|ไขมัน)\\D{0,20}?$NUMBER")

    // "290 kcal" wins over "Energy 1200 kJ / 290 kcal": label energy is often listed in kJ first.
    private val kcalValueRegex = Regex("(?i)$NUMBER\\s*(?:kcal|กิโลแคลอรี)")
    private val energyRegex = Regex("(?i)(?:energy|calories|kcal|พลังงาน|แคลอรี)\\D{0,20}?$NUMBER\\s*(kj|กิโลจูล)?")

    /**
     * Scans a block of text recognized by ML Kit OCR and attempts to extract
     * Calories, Protein, Carbs, and Fats using Regex.
     */
    fun parseNutritionFromLabel(rawText: String): BarcodeNutrition {
        val text = rawText.replace("\n", " ").replace("\r", " ").replace(subRowRegex, " ")

        val calories = kcalValueRegex.find(text)?.let { it.groupValues[1].toRoundedInt() }
            ?: energyRegex.find(text)?.let {
                val value = it.groupValues[1].toDoubleOrNull() ?: 0.0
                (if (it.groupValues[2].isNotEmpty()) value / KJ_PER_KCAL else value).roundToInt()
            }
            ?: 0

        return BarcodeNutrition(
            productName = "Scanned Label",
            calories = calories,
            protein = proteinRegex.find(text)?.groupValues?.get(1)?.toRoundedInt() ?: 0,
            carbs = carbsRegex.find(text)?.groupValues?.get(1)?.toRoundedInt() ?: 0,
            fats = (totalFatRegex.find(text) ?: fatRegex.find(text))?.groupValues?.get(1)?.toRoundedInt() ?: 0
        )
    }

    private fun String.toRoundedInt(): Int = toDoubleOrNull()?.roundToInt() ?: 0
}
