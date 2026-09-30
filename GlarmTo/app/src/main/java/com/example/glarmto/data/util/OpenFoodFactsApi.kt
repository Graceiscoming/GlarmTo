package com.example.glarmto.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * What a scan found. The numbers are for one serving, or for 100 g when [per100g] is true (the pack doesn't
 * say how big a serving is); the screen shows that to the user.
 * [productName] is empty when the source has no name, so the screen can put in text in the current language.
 */
data class BarcodeNutrition(
    val productName: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val per100g: Boolean = false,
    /** False when the product is known by name only: the numbers are 0 and mean "not known", not "zero". */
    val nutritionKnown: Boolean = true
)

/** The online search: Open Food Facts, a free community database of food products (ODbL). */
object OpenFoodFactsApi : ProductSource {
    private const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product/"
    private const val FIELDS = "code,product_name,product_name_th,brands,serving_quantity,nutriments"
    private const val KJ_PER_KCAL = 4.184

    // Open Food Facts asks every app to say who it is, and throttles anonymous clients.
    private const val USER_AGENT = "GlarmTo/1.0 (Android fitness tracker; https://github.com/Graceiscoming/GlarmTo)"

    /** Tries each spelling of the barcode; throws [IOException] if the service can't be reached. */
    override suspend fun find(candidates: List<String>): BarcodeNutrition? = withContext(Dispatchers.IO) {
        for (code in candidates) {
            val body = fetch(code) ?: continue
            val parsed = try {
                parseProduct(JSONObject(body))
            } catch (e: JSONException) {
                null
            }
            if (parsed != null) return@withContext parsed
        }
        null
    }

    /** The response text, or null if the service says it doesn't know the product. */
    private fun fetch(code: String): String? {
        val url = URL("$BASE_URL${URLEncoder.encode(code, "UTF-8")}.json?fields=$FIELDS")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            return when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("Open Food Facts answered HTTP $status")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Maps a response to [BarcodeNutrition], or null if the product is unknown (or has neither nutrition nor a name).
     * A product with a name but no nutrition facts comes back with [BarcodeNutrition.nutritionKnown] false.
     *
     * Per-serving values are used when the database has them. Otherwise the per-100 g values are scaled to
     * one serving if the serving size is known, and only if it isn't are they returned per 100 g (flagged).
     * Energy falls back to kilojoules (the unit many Thai labels list first), then to the macros.
     */
    internal fun parseProduct(jsonResponse: JSONObject): BarcodeNutrition? {
        if (jsonResponse.optInt("status") != 1) return null
        val product = jsonResponse.optJSONObject("product") ?: return null
        val nutriments = product.optJSONObject("nutriments") ?: return null

        val servingGrams = product.optDouble("serving_quantity", Double.NaN).takeIf { it in 3.0..2000.0 }
        val hasServingValues = listOf("energy-kcal_serving", "energy_serving", "proteins_serving", "carbohydrates_serving", "fat_serving")
            .any { nutriments.has(it) }

        val suffix = if (hasServingValues) "_serving" else "_100g"
        val scale = if (!hasServingValues && servingGrams != null) servingGrams / 100.0 else 1.0

        fun value(key: String): Double? = nutriments.optDouble(key + suffix, Double.NaN).takeIf { !it.isNaN() }

        val protein = value("proteins")
        val carbs = value("carbohydrates")
        val fat = value("fat")
        val energy = value("energy-kcal")
            ?: (value("energy-kj") ?: value("energy"))?.div(KJ_PER_KCAL) // "energy" is in kilojoules
            ?: if (protein != null || carbs != null || fat != null) 4 * (protein ?: 0.0) + 4 * (carbs ?: 0.0) + 9 * (fat ?: 0.0) else null
        val name = product.optString("product_name_th").ifBlank { product.optString("product_name") }.trim()
        if (energy == null) {
            // Known product, but nobody has entered its nutrition facts: still useful for its name.
            return if (name.isNotBlank()) BarcodeNutrition(name, 0, 0, 0, 0, nutritionKnown = false) else null
        }
        return BarcodeNutrition(
            productName = name,
            calories = (energy * scale).roundToInt(),
            protein = ((protein ?: 0.0) * scale).roundToInt(),
            carbs = ((carbs ?: 0.0) * scale).roundToInt(),
            fats = ((fat ?: 0.0) * scale).roundToInt(),
            per100g = !hasServingValues && servingGrams == null
        )
    }
}
