package com.example.glarmto.data.util

import kotlin.math.roundToInt

object NutritionOcrParser {

    private const val KJ_PER_KCAL = 4.184
    private const val NUMBER = "(\\d+(?:\\.\\d+)?)"

    // Sub-rows of "fat" / "calories" that would otherwise be mistaken for the headline value.
    private val subRowRegex = Regex(
        "(?i)(?:saturated|trans|poly\\w*|mono\\w*)\\s*fat|calories\\s*from\\s*fat|ไขมัน(?:อิ่มตัว|ทรานส์)"
    )

    // The gap between a label and its number may not run into another nutrient's label, so a row with a
    // missing value ("Protein" then "Carbs 30") doesn't borrow the next row's number.
    private const val GAP_TO_NUMBER =
        "(?:(?!protein|carb|fat|energy|calories|kcal|โปรตีน|คาร์|ไขมัน|พลังงาน|แคลอรี)\\D){0,20}?"
    private const val GAP_TO_ENERGY_NUMBER =
        "(?:(?!protein|carb|fat|โปรตีน|คาร์|ไขมัน)\\D){0,20}?"

    // รองรับทั้งฉลากภาษาอังกฤษ และฉลากโภชนาการภาษาไทย
    private val proteinRegex = Regex("(?i)(?:protein|โปรตีน)$GAP_TO_NUMBER$NUMBER")
    private val carbsRegex = Regex("(?i)(?:carb|คาร์โบไฮเดรต|คาร์บ)$GAP_TO_NUMBER$NUMBER")
    private val totalFatRegex = Regex("(?i)(?:total\\s*fat|ไขมันทั้งหมด)$GAP_TO_NUMBER$NUMBER")
    private val fatRegex = Regex("(?i)(?:fat|ไขมัน)$GAP_TO_NUMBER$NUMBER")

    // "290 kcal" wins over "Energy 1200 kJ / 290 kcal": label energy is often listed in kJ first.
    private val kcalValueRegex = Regex("(?i)$NUMBER\\s*(?:kcal|กิโลแคลอรี)")
    private val energyRegex =
        Regex("(?i)(?:energy|calories|kcal|พลังงาน|แคลอรี)$GAP_TO_ENERGY_NUMBER$NUMBER\\s*(kj|กิโลจูล)?")

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
            productName = "", // the screen names it in the current language
            calories = calories,
            protein = proteinRegex.find(text)?.groupValues?.get(1)?.toRoundedInt() ?: 0,
            carbs = carbsRegex.find(text)?.groupValues?.get(1)?.toRoundedInt() ?: 0,
            fats = (totalFatRegex.find(text) ?: fatRegex.find(text))?.groupValues?.get(1)?.toRoundedInt() ?: 0
        )
    }

    private fun String.toRoundedInt(): Int = toDoubleOrNull()?.roundToInt() ?: 0
}
