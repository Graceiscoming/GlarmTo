package com.example.glarmto

import com.example.glarmto.data.util.BarcodeNutrition
import com.example.glarmto.data.util.KeyValueStore
import com.example.glarmto.data.util.LookupResult
import com.example.glarmto.data.util.ProductCache
import com.example.glarmto.data.util.ProductLookup
import com.example.glarmto.data.util.ProductOrigin
import com.example.glarmto.data.util.ProductSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ProductLookupTest {

    private val chips = BarcodeNutrition("Chips", 160, 2, 16, 10)
    private val water = BarcodeNutrition("Water", 0, 0, 0, 0)
    private val nameOnly = BarcodeNutrition("Mystery snack", 0, 0, 0, 0, nutritionKnown = false)

    private class MapSource(private val items: Map<String, BarcodeNutrition>) : ProductSource {
        val asked = mutableListOf<List<String>>()
        override suspend fun find(candidates: List<String>): BarcodeNutrition? {
            asked.add(candidates)
            return candidates.firstNotNullOfOrNull { items[it] }
        }
    }

    private class FailingSource(private val error: Throwable) : ProductSource {
        var calls = 0
        override suspend fun find(candidates: List<String>): BarcodeNutrition? {
            calls++
            throw error
        }
    }

    private class MemoryStore : KeyValueStore {
        val data = mutableMapOf<String, String>()
        override fun get(key: String) = data[key]
        override fun put(key: String, value: String) { data[key] = value }
        override fun remove(key: String) { data.remove(key) }
    }

    private fun lookup(
        bundled: List<ProductSource> = emptyList(),
        online: ProductSource? = null,
        cache: ProductCache? = null,
        isOnline: Boolean = true,
        mine: ProductCache? = null
    ) = ProductLookup(bundled, online, cache, { isOnline }, mine)

    private val code = "8850999320052"

    // ---------------- where it finds things ----------------

    @Test
    fun `finds a product in the bundled list`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(mapOf(code to chips)))).lookup(code)

        assertEquals(LookupResult.Found(chips, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `bundled products are found with no internet`() = runBlocking {
        val online = FailingSource(IOException("no network"))
        val result = lookup(bundled = listOf(MapSource(mapOf(code to chips))), online = online, isOnline = false).lookup(code)

        assertEquals(LookupResult.Found(chips, ProductOrigin.BUNDLED), result)
        assertEquals("never even tried the network", 0, online.calls)
    }

    @Test
    fun `falls back to the online search when the bundled list does not know it`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(emptyMap())), online = MapSource(mapOf(code to chips))).lookup(code)

        assertEquals(LookupResult.Found(chips, ProductOrigin.ONLINE), result)
    }

    @Test
    fun `bundled sources are tried in order`() = runBlocking {
        val first = MapSource(mapOf(code to chips))
        val second = MapSource(mapOf(code to water))

        val result = lookup(bundled = listOf(first, second)).lookup(code)

        assertEquals(LookupResult.Found(chips, ProductOrigin.BUNDLED), result)
        assertTrue("the second source was not asked", second.asked.isEmpty())
    }

    @Test
    fun `nothing anywhere is reported as not found`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(emptyMap())), online = MapSource(emptyMap())).lookup(code)

        assertEquals(LookupResult.NotFound, result)
    }

    @Test
    fun `with no online source an unknown product is simply not found`() = runBlocking {
        assertEquals(LookupResult.NotFound, lookup(bundled = listOf(MapSource(emptyMap()))).lookup(code))
    }

    // ---------------- offline ----------------

    @Test
    fun `an unknown product while offline says so`() = runBlocking {
        val online = FailingSource(IOException("should not be called"))

        val result = lookup(online = online, isOnline = false).lookup(code)

        assertEquals(LookupResult.NotFoundOffline, result)
        assertEquals(0, online.calls)
    }

    @Test
    fun `a network failure while asking online is reported as offline, not as unknown`() = runBlocking {
        assertEquals(LookupResult.NotFoundOffline, lookup(online = FailingSource(IOException("timeout"))).lookup(code))
    }

    @Test(expected = CancellationException::class)
    fun `cancelling the scan cancels the lookup`() {
        runBlocking { lookup(online = FailingSource(CancellationException("scan closed"))).lookup(code) }
    }

    // ---------------- products known by name only ----------------

    @Test
    fun `a name only product is returned when there is no online source`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(mapOf(code to nameOnly)))).lookup(code)

        assertEquals(LookupResult.Found(nameOnly, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `a name only product is upgraded by the online search when it has nutrition`() = runBlocking {
        val result = lookup(
            bundled = listOf(MapSource(mapOf(code to nameOnly))),
            online = MapSource(mapOf(code to chips))
        ).lookup(code)

        assertEquals(LookupResult.Found(chips, ProductOrigin.ONLINE), result)
    }

    @Test
    fun `a name only product is kept when the online search has nothing better`() = runBlocking {
        val result = lookup(
            bundled = listOf(MapSource(mapOf(code to nameOnly))),
            online = MapSource(mapOf(code to nameOnly.copy(productName = "Other name")))
        ).lookup(code)

        assertEquals(LookupResult.Found(nameOnly, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `a name only product is kept when the online search does not know it`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(mapOf(code to nameOnly))), online = MapSource(emptyMap())).lookup(code)

        assertEquals(LookupResult.Found(nameOnly, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `a name only product is kept while offline - never reported as unknown`() = runBlocking {
        val online = FailingSource(IOException("offline"))

        val offline = lookup(bundled = listOf(MapSource(mapOf(code to nameOnly))), online = online, isOnline = false).lookup(code)
        val failing = lookup(bundled = listOf(MapSource(mapOf(code to nameOnly))), online = online, isOnline = true).lookup(code)

        assertEquals(LookupResult.Found(nameOnly, ProductOrigin.BUNDLED), offline)
        assertEquals(LookupResult.Found(nameOnly, ProductOrigin.BUNDLED), failing)
    }

    @Test
    fun `a bundled product with nutrition wins over an earlier name only one`() = runBlocking {
        val result = lookup(
            bundled = listOf(MapSource(mapOf(code to nameOnly)), MapSource(mapOf(code to chips)))
        ).lookup(code)

        assertEquals(LookupResult.Found(chips, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `an online name only answer is used when nothing else knows the product`() = runBlocking {
        val result = lookup(online = MapSource(mapOf(code to nameOnly))).lookup(code)

        assertEquals(LookupResult.Found(nameOnly, ProductOrigin.ONLINE), result)
    }

    @Test
    fun `a name only answer is not cached - nutrition may be added later`() = runBlocking {
        val cache = ProductCache(MemoryStore())

        lookup(online = MapSource(mapOf(code to nameOnly)), cache = cache).lookup(code)

        assertEquals(0, cache.size)
    }

    // ---------------- cache ----------------

    @Test
    fun `an online hit is cached for next time`() = runBlocking {
        val cache = ProductCache(MemoryStore())
        val lookup = lookup(online = MapSource(mapOf(code to chips)), cache = cache)

        assertEquals(LookupResult.Found(chips, ProductOrigin.ONLINE), lookup.lookup(code))

        assertEquals(1, cache.size)
        assertEquals(LookupResult.Found(chips, ProductOrigin.CACHE), lookup.lookup(code))
    }

    @Test
    fun `a cached product works with no internet`() = runBlocking {
        val cache = ProductCache(MemoryStore())
        lookup(online = MapSource(mapOf(code to chips)), cache = cache).lookup(code)

        val offline = lookup(online = FailingSource(IOException("offline")), cache = cache, isOnline = false)

        assertEquals(LookupResult.Found(chips, ProductOrigin.CACHE), offline.lookup(code))
    }

    @Test
    fun `the cache is checked before the bundled list and the network`() = runBlocking {
        val cache = ProductCache(MemoryStore())
        cache.put(code, water)
        val bundled = MapSource(mapOf(code to chips))

        val result = lookup(bundled = listOf(bundled), cache = cache).lookup(code)

        assertEquals(LookupResult.Found(water, ProductOrigin.CACHE), result)
        assertTrue(bundled.asked.isEmpty())
    }

    @Test
    fun `misses are not cached`() = runBlocking {
        val cache = ProductCache(MemoryStore())

        lookup(online = MapSource(emptyMap()), cache = cache).lookup(code)

        assertEquals(0, cache.size)
    }

    // ---------------- products the user entered ----------------

    @Test
    fun `a product the user entered is found next time`() = runBlocking {
        val mine = ProductCache(MemoryStore())
        val lookup = lookup(mine = mine)
        assertEquals(LookupResult.NotFound, lookup.lookup(code))

        lookup.remember(code, BarcodeNutrition("Grandma's curry puff", 180, 0, 0, 0))

        val found = lookup.lookup(code) as LookupResult.Found
        assertEquals(ProductOrigin.MINE, found.origin)
        assertEquals("Grandma's curry puff", found.product.productName)
        assertEquals(180, found.product.calories)
    }

    @Test
    fun `what the user entered beats every other source`() = runBlocking {
        val mine = ProductCache(MemoryStore())
        val cache = ProductCache(MemoryStore())
        cache.put(code, water)
        val lookup = lookup(bundled = listOf(MapSource(mapOf(code to chips))), cache = cache, mine = mine)

        lookup.remember(code, BarcodeNutrition("My version", 99, 1, 2, 3))

        assertEquals(99, (lookup.lookup(code) as LookupResult.Found).product.calories)
    }

    @Test
    fun `a remembered product works with no internet`() = runBlocking {
        val mine = ProductCache(MemoryStore())
        val lookup = lookup(online = FailingSource(IOException("offline")), isOnline = false, mine = mine)
        lookup.remember(code, BarcodeNutrition("Snack", 120, 0, 0, 0))

        assertEquals(ProductOrigin.MINE, (lookup.lookup(code) as LookupResult.Found).origin)
    }

    @Test
    fun `remembering works under any spelling of the barcode`() = runBlocking {
        val mine = ProductCache(MemoryStore())
        val lookup = lookup(mine = mine)

        lookup.remember("012000001086", BarcodeNutrition("Imported", 50, 0, 0, 0)) // UPC-A

        assertEquals(ProductOrigin.MINE, (lookup.lookup("0012000001086") as LookupResult.Found).origin)
        assertEquals(ProductOrigin.MINE, (lookup.lookup("012000001086") as LookupResult.Found).origin)
    }

    @Test
    fun `a remembered entry counts as having nutrition and is not per 100g`() = runBlocking {
        val mine = ProductCache(MemoryStore())
        val lookup = lookup(mine = mine)

        lookup.remember(code, BarcodeNutrition("Snack", 120, 0, 0, 0, per100g = true, nutritionKnown = false))

        val product = (lookup.lookup(code) as LookupResult.Found).product
        assertTrue(product.nutritionKnown)
        assertFalse(product.per100g)
    }

    @Test
    fun `remembering something that is not a barcode does nothing`() = runBlocking {
        val mine = ProductCache(MemoryStore())

        lookup(mine = mine).remember("hello", BarcodeNutrition("X", 1, 0, 0, 0))

        assertEquals(0, mine.size)
    }

    @Test
    fun `remembering without a store is harmless`() = runBlocking {
        lookup().remember(code, BarcodeNutrition("X", 1, 0, 0, 0))
    }

    // ---------------- barcode handling ----------------

    @Test
    fun `text that is not a barcode is not found and nothing is asked`() = runBlocking {
        val source = MapSource(mapOf(code to chips))

        assertEquals(LookupResult.NotFound, lookup(bundled = listOf(source)).lookup("not a barcode"))
        assertEquals(LookupResult.NotFound, lookup(bundled = listOf(source)).lookup(""))
        assertTrue(source.asked.isEmpty())
    }

    @Test
    fun `a UPC-A scan finds the product stored under its 13 digit form`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(mapOf("0012000001086" to water)))).lookup("012000001086")

        assertEquals(LookupResult.Found(water, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `a product stored without its leading zero is found too`() = runBlocking {
        val result = lookup(bundled = listOf(MapSource(mapOf("012000001086" to water)))).lookup("0012000001086")

        assertEquals(LookupResult.Found(water, ProductOrigin.BUNDLED), result)
    }

    @Test
    fun `a not found result is a value, not an exception`() = runBlocking {
        assertNull(lookup().lookup(code) as? LookupResult.Found)
    }
}
