package com.example.glarmto.data.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class BarcodeNutrition(
    val productName: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

object OpenFoodFactsApi {
    private const val BASE_URL = "https://world.openfoodfacts.org/api/v0/product/"

    /**
     * Fetches nutritional data for a given barcode using the free OpenFoodFacts API.
     */
    suspend fun getNutritionByBarcode(barcode: String): BarcodeNutrition? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL${URLEncoder.encode(barcode, "UTF-8")}.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                parseProduct(JSONObject(response.toString()))?.let { return@withContext it }
            }
        } catch (e: CancellationException) {
            // A cancelled scan must stop here, not fall through to the offline lookup.
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 🚨 Fallback: ถ้า API ไม่เจอ ให้ลองค้นหาในคลังข้อมูลจำลองของแอป (Database 7-11 ของเราเอง!)
        return@withContext ThaiProductDatabase.database[barcode]
    }

    /**
     * Maps an OpenFoodFacts response to [BarcodeNutrition], or null if the product or its nutrition data is missing.
     *
     * Prefers per-serving values, as 100g doesn't usually match the box. All four macros use the same basis
     * (mixing serving and 100g values would be wrong), and when only per-100g data exists the product name is
     * tagged "(per 100g)" so the user knows the numbers aren't for one serving.
     */
    internal fun parseProduct(jsonResponse: JSONObject): BarcodeNutrition? {
        if (jsonResponse.optInt("status") != 1) return null
        val product = jsonResponse.optJSONObject("product") ?: return null
        val nutriments = product.optJSONObject("nutriments") ?: return null

        val hasServing = nutriments.has("energy-kcal_serving")
        val suffix = if (hasServing) "_serving" else "_100g"

        val name = product.optString("product_name", "").ifBlank { "Unknown Product" }
        return BarcodeNutrition(
            productName = if (hasServing) name else "$name (per 100g)",
            calories = nutriments.optDouble("energy-kcal$suffix", 0.0).toInt(),
            protein = nutriments.optDouble("proteins$suffix", 0.0).toInt(),
            carbs = nutriments.optDouble("carbohydrates$suffix", 0.0).toInt(),
            fats = nutriments.optDouble("fat$suffix", 0.0).toInt()
        )
    }
}
