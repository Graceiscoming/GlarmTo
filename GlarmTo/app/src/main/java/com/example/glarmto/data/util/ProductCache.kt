package com.example.glarmto.data.util

import android.content.SharedPreferences

/** The little bit of storage [ProductCache] needs, so it can be tested without Android. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}

/**
 * Remembers products by barcode: earlier online hits, or products the user entered themselves. Keeps the
 * most recent [maxEntries]; older ones are dropped. Stores one short line per product.
 */
class ProductCache(private val store: KeyValueStore, private val maxEntries: Int = 300) : ProductSource {

    companion object {
        private const val INDEX_KEY = "index"
        private const val ENTRY_PREFIX = "p_"
        private const val SEPARATOR = '\u001F'
    }

    override suspend fun find(candidates: List<String>): BarcodeNutrition? =
        candidates.firstNotNullOfOrNull { decode(store.get(ENTRY_PREFIX + it)) }

    fun put(code: String, product: BarcodeNutrition) {
        val order = readIndex().toMutableList()
        order.remove(code)
        order.add(code) // most recent last
        while (order.size > maxEntries) store.remove(ENTRY_PREFIX + order.removeAt(0))
        store.put(ENTRY_PREFIX + code, encode(product))
        store.put(INDEX_KEY, order.joinToString(","))
    }

    val size: Int get() = readIndex().size

    private fun readIndex(): List<String> =
        store.get(INDEX_KEY)?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()

    private fun encode(p: BarcodeNutrition): String = listOf(
        p.productName.replace(SEPARATOR, ' '), p.calories, p.protein, p.carbs, p.fats,
        if (p.per100g) 1 else 0, if (p.nutritionKnown) 1 else 0
    ).joinToString(SEPARATOR.toString())

    private fun decode(text: String?): BarcodeNutrition? {
        val parts = text?.split(SEPARATOR) ?: return null
        if (parts.size != 7) return null
        val numbers = parts.drop(1).map { it.toIntOrNull() ?: return null }
        return BarcodeNutrition(parts[0], numbers[0], numbers[1], numbers[2], numbers[3], numbers[4] == 1, numbers[5] == 1)
    }
}
