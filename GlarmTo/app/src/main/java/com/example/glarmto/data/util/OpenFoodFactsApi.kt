package com.example.glarmto.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
            val url = URL("$BASE_URL$barcode.json")
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

                val jsonResponse = JSONObject(response.toString())
                if (jsonResponse.getInt("status") == 1) {
                    val product = jsonResponse.getJSONObject("product")
                    val nutriments = product.optJSONObject("nutriments")
                    
                    if (nutriments != null) {
                        // Prioritize per-serving data over 100g, as 100g doesn't usually match the box
                        val calories = nutriments.optDouble("energy-kcal_serving", nutriments.optDouble("energy-kcal_100g", 0.0)).toInt()
                        val protein = nutriments.optDouble("proteins_serving", nutriments.optDouble("proteins_100g", 0.0)).toInt()
                        val carbs = nutriments.optDouble("carbohydrates_serving", nutriments.optDouble("carbohydrates_100g", 0.0)).toInt()
                        val fats = nutriments.optDouble("fat_serving", nutriments.optDouble("fat_100g", 0.0)).toInt()

                        return@withContext BarcodeNutrition(
                            productName = product.optString("product_name", "Unknown Product"),
                            calories = calories,
                            protein = protein,
                            carbs = carbs,
                            fats = fats
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 🚨 Fallback: ถ้า API ไม่เจอ ให้ลองค้นหาในคลังข้อมูลจำลองของแอป (Database 7-11 ของเราเอง!)
        return@withContext ThaiProductDatabase.database[barcode]
    }
}
