package com.example.glarmto.data.util

object NutritionOcrParser {
    
    /**
     * Scans a block of text recognized by ML Kit OCR and attempts to extract
     * Calories, Protein, Carbs, and Fats using Regex.
     */
    fun parseNutritionFromLabel(rawText: String): BarcodeNutrition {
        val lines = rawText.split("\n", "\r").map { it.lowercase().replace(" ", "") }
        
        var calories = 0
        var protein = 0
        var carbs = 0
        var fats = 0

        // รองรับทั้งฉลากภาษาอังกฤษ และฉลากโภชนาการภาษาไทย
        val proteinRegex = Regex("(?i)(?:protein|โปรตีน).*?(\\d+)")
        val carbsRegex = Regex("(?i)(?:carb|คาร์โบไฮเดรต|คาร์บ).*?(\\d+)")
        val fatRegex = Regex("(?i)(?:fat|ไขมัน).*?(\\d+)")
        val kcalRegex = Regex("(?i)(?:energy|kcal|calories|พลังงาน|แคลอรี|กิโลแคลอรี).*?(\\d+)")

        val flattenedText = rawText.replace("\n", " ").replace("\r", " ")
        proteinRegex.find(flattenedText)?.let { protein = it.groupValues[1].toIntOrNull() ?: 0 }
        carbsRegex.find(flattenedText)?.let { carbs = it.groupValues[1].toIntOrNull() ?: 0 }
        fatRegex.find(flattenedText)?.let { fats = it.groupValues[1].toIntOrNull() ?: 0 }
        kcalRegex.find(flattenedText)?.let { calories = it.groupValues[1].toIntOrNull() ?: 0 }

        return BarcodeNutrition(
            productName = "Scanned Label",
            calories = calories,
            protein = protein,
            carbs = carbs,
            fats = fats
        )
    }
}
