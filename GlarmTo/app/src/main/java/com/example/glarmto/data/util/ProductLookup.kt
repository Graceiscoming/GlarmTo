package com.example.glarmto.data.util

import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Somewhere a barcode can be looked up. [find] returns null for "not here" and throws [IOException] if it could not ask. */
interface ProductSource {
    suspend fun find(candidates: List<String>): BarcodeNutrition?
}

enum class ProductOrigin { MINE, CACHE, BUNDLED, ONLINE }

sealed interface LookupResult {
    data class Found(val product: BarcodeNutrition, val origin: ProductOrigin) : LookupResult

    /** Nothing in any source. */
    data object NotFound : LookupResult

    /** Not in the offline data, and the online search could not be reached (no internet, timeout...). */
    data object NotFoundOffline : LookupResult
}

/**
 * Finds a product by barcode, cheapest source first:
 *  1. [mine]: products the user entered themselves (they know best, and it overrides everything else),
 *  2. [cache]: earlier online hits,
 *  3. the products bundled with the app (works with no internet),
 *  4. Open Food Facts online; hits with nutrition facts are cached so the product works offline next time.
 *
 * A bundled product may be known by name only (no nutrition facts yet). Then the online search is still
 * tried for better data, but the name is kept as the answer if it has nothing, or can't be reached.
 */
class ProductLookup(
    private val bundled: List<ProductSource>,
    private val online: ProductSource?,
    private val cache: ProductCache?,
    private val isOnline: () -> Boolean,
    private val mine: ProductCache? = null
) {
    suspend fun lookup(rawBarcode: String): LookupResult {
        val candidates = BarcodeNormalizer.candidates(rawBarcode)
        if (candidates.isEmpty()) return LookupResult.NotFound

        mine?.find(candidates)?.let { return LookupResult.Found(it, ProductOrigin.MINE) }
        cache?.find(candidates)?.let { return LookupResult.Found(it, ProductOrigin.CACHE) }

        var nameOnly: BarcodeNutrition? = null
        for (source in bundled) {
            val product = source.find(candidates) ?: continue
            if (product.nutritionKnown) return LookupResult.Found(product, ProductOrigin.BUNDLED)
            if (nameOnly == null) nameOnly = product
        }
        val fallback = nameOnly?.let { LookupResult.Found(it, ProductOrigin.BUNDLED) }

        val remote = online ?: return fallback ?: LookupResult.NotFound
        if (!isOnline()) return fallback ?: LookupResult.NotFoundOffline
        return try {
            val product = remote.find(candidates)
            when {
                product == null -> fallback ?: LookupResult.NotFound
                product.nutritionKnown -> {
                    cache?.put(candidates.first(), product)
                    LookupResult.Found(product, ProductOrigin.ONLINE)
                }
                else -> fallback ?: LookupResult.Found(product, ProductOrigin.ONLINE)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            fallback ?: LookupResult.NotFoundOffline
        }
    }

    /** Remembers a product the user entered by hand, so scanning its barcode fills it in from now on. */
    fun remember(rawBarcode: String, product: BarcodeNutrition) {
        val code = BarcodeNormalizer.normalize(rawBarcode) ?: return
        mine?.put(code, product.copy(nutritionKnown = true, per100g = false))
    }
}
