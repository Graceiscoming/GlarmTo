package com.example.glarmto.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.roundToInt

/** Nutrition facts per 100 g, like the Open Food Facts export they came from. */
data class NutritionPer100g(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double)

/** One product from the bundled list. [nutrition] is null when the product is known by name only. */
data class ProductRecord(
    val code: String,
    val name: String,
    val brand: String,
    val servingGrams: Double?,
    val nutrition: NutritionPer100g?
) {
    /** "Name (Brand)", unless the name already says the brand. */
    val displayName: String
        get() = if (brand.isNotBlank() && !name.contains(brand, ignoreCase = true)) "$name ($brand)" else name

    /**
     * Per serving when the serving size is known (that is what is on the pack), otherwise per 100 g, flagged
     * so the screen can say so. A product without nutrition facts gives just its name.
     */
    fun toNutrition(): BarcodeNutrition {
        val n = nutrition ?: return BarcodeNutrition(displayName, 0, 0, 0, 0, nutritionKnown = false)
        val grams = servingGrams
        val scale = if (grams != null) grams / 100.0 else 1.0
        return BarcodeNutrition(
            productName = displayName,
            calories = (n.kcal * scale).roundToInt(),
            protein = (n.protein * scale).roundToInt(),
            carbs = (n.carbs * scale).roundToInt(),
            fats = (n.fat * scale).roundToInt(),
            per100g = grams == null
        )
    }
}

/**
 * The products bundled in `assets/thai_products.tsv`: items sold in Thailand from the Open Food Facts
 * database (ODbL), built by `tools/build_thai_products.py`. One product per line, tab separated:
 * `code, name, brand, serving_g, kcal_100g, protein_100g, carbs_100g, fat_100g`
 * (the last four are empty for a product known by name only).
 */
object ProductDatasetParser {
    fun parseLine(line: String): ProductRecord? {
        val f = line.split('\t')
        if (f.size < 8) return null
        val code = f[0].trim()
        if (code.isEmpty() || !code.all { it in '0'..'9' }) return null
        val name = f[1].trim()
        if (name.isEmpty()) return null
        val serving = f[3].toDoubleOrNull()?.takeIf { it > 0 }

        val nutritionCells = f.subList(4, 8).map { it.trim() }
        val nutrition = if (nutritionCells.all { it.isEmpty() }) {
            null
        } else {
            val numbers = nutritionCells.map { it.toDoubleOrNull() ?: return null }
            if (numbers.any { it < 0 }) return null
            NutritionPer100g(numbers[0], numbers[1], numbers[2], numbers[3])
        }
        return ProductRecord(code, name, f[2].trim(), serving, nutrition)
    }

    /** Skips blank and malformed lines; if a code appears twice the first (best ranked) wins. */
    fun parse(lines: Sequence<String>): Map<String, ProductRecord> {
        val records = HashMap<String, ProductRecord>()
        for (line in lines) {
            val record = parseLine(line) ?: continue
            records.putIfAbsent(record.code, record)
        }
        return records
    }
}

/** The bundled products, read from the asset the first time they are needed (off the main thread). */
class AssetProductDataset(private val openStream: () -> InputStream) : ProductSource {

    private val records: Map<String, ProductRecord> by lazy {
        openStream().bufferedReader(Charsets.UTF_8).useLines { ProductDatasetParser.parse(it) }
    }

    val size: Int get() = records.size

    override suspend fun find(candidates: List<String>): BarcodeNutrition? = withContext(Dispatchers.IO) {
        candidates.firstNotNullOfOrNull { records[it] }?.toNutrition()
    }
}
