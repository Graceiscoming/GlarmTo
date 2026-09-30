package com.example.glarmto

import com.example.glarmto.data.util.BarcodeNutrition
import com.example.glarmto.data.util.KeyValueStore
import com.example.glarmto.data.util.ProductCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCacheTest {

    private class MemoryStore : KeyValueStore {
        val data = mutableMapOf<String, String>()
        override fun get(key: String) = data[key]
        override fun put(key: String, value: String) { data[key] = value }
        override fun remove(key: String) { data.remove(key) }
    }

    private fun product(name: String = "Chips", kcal: Int = 160) = BarcodeNutrition(name, kcal, 2, 16, 10)

    private fun ProductCache.get(code: String) = runBlocking { find(listOf(code)) }

    @Test
    fun `a stored product comes back exactly`() {
        val cache = ProductCache(MemoryStore())
        val original = BarcodeNutrition("เลย์ รสโนริสาหร่าย (Lay's)", 160, 2, 16, 10, per100g = true, nutritionKnown = true)

        cache.put("8850999320052", original)

        assertEquals(original, cache.get("8850999320052"))
    }

    @Test
    fun `every field survives the round trip`() {
        val cache = ProductCache(MemoryStore())

        listOf(
            BarcodeNutrition("A", 0, 0, 0, 0),
            BarcodeNutrition("B", 1, 2, 3, 4, per100g = true),
            BarcodeNutrition("C", 99999, 1, 1, 1, nutritionKnown = false),
            BarcodeNutrition("", 5, 6, 7, 8)
        ).forEachIndexed { i, p ->
            cache.put("10000000$i", p)
            assertEquals(p, cache.get("10000000$i"))
        }
    }

    @Test
    fun `an unknown barcode is not found`() {
        assertNull(ProductCache(MemoryStore()).get("8850999320052"))
    }

    @Test
    fun `the first matching spelling wins`() = runBlocking {
        val cache = ProductCache(MemoryStore())
        cache.put("012000001086", product("UPC form"))

        assertEquals("UPC form", cache.find(listOf("0012000001086", "012000001086"))!!.productName)
    }

    @Test
    fun `putting the same barcode again replaces it without growing`() {
        val cache = ProductCache(MemoryStore())
        cache.put("8850999320052", product("Old", 100))
        cache.put("8850999320052", product("New", 200))

        assertEquals(1, cache.size)
        assertEquals("New", cache.get("8850999320052")!!.productName)
    }

    @Test
    fun `the oldest entries are dropped past the limit`() {
        val store = MemoryStore()
        val cache = ProductCache(store, maxEntries = 3)

        listOf("100", "200", "300", "400").forEach { cache.put("8850000000$it", product(it)) }

        assertEquals(3, cache.size)
        assertNull("the oldest is gone", cache.get("8850000000100"))
        assertEquals("400", cache.get("8850000000400")!!.productName)
        assertFalse("and its storage was freed", store.data.containsKey("p_8850000000100"))
    }

    @Test
    fun `using an entry again keeps it`() {
        val cache = ProductCache(MemoryStore(), maxEntries = 2)
        cache.put("88500000001", product("first"))
        cache.put("88500000002", product("second"))

        cache.put("88500000001", product("first")) // used again: now the most recent
        cache.put("88500000003", product("third"))

        assertEquals("first", cache.get("88500000001")!!.productName)
        assertNull("the least recently used one went", cache.get("88500000002"))
    }

    @Test
    fun `the cache survives being opened again on the same storage`() {
        val store = MemoryStore()
        ProductCache(store).put("8850999320052", product("Kept"))

        assertEquals("Kept", ProductCache(store).get("8850999320052")!!.productName)
        assertEquals(1, ProductCache(store).size)
    }

    @Test
    fun `a corrupted entry is ignored instead of crashing`() {
        val store = MemoryStore()
        val cache = ProductCache(store)
        store.put("p_8850999320052", "garbage")
        store.put("p_8850999320053", "a\u001F1\u001F2")
        store.put("p_8850999320054", "a\u001Fx\u001F2\u001F3\u001F4\u001F0\u001F1")

        assertNull(cache.get("8850999320052"))
        assertNull(cache.get("8850999320053"))
        assertNull(cache.get("8850999320054"))
    }

    @Test
    fun `a name containing the separator character is stored safely`() {
        val cache = ProductCache(MemoryStore())

        cache.put("8850999320052", product("Bad\u001FName"))

        assertEquals("Bad Name", cache.get("8850999320052")!!.productName)
    }

    @Test
    fun `names with commas and unicode are fine`() {
        val cache = ProductCache(MemoryStore())
        cache.put("8850999320052", product("ข้าวเกรียบ, รสต้มยำ 🍤"))

        assertEquals("ข้าวเกรียบ, รสต้มยำ 🍤", cache.get("8850999320052")!!.productName)
        assertEquals(1, cache.size)
    }

    @Test
    fun `an empty cache has size zero`() {
        assertEquals(0, ProductCache(MemoryStore()).size)
        assertTrue(ProductCache(MemoryStore()).size == 0)
    }

    @Test
    fun `a large limit keeps many entries`() {
        val cache = ProductCache(MemoryStore(), maxEntries = 5000)
        repeat(1000) { cache.put("885" + it.toString().padStart(10, '0'), product("p$it")) }

        assertEquals(1000, cache.size)
        assertEquals("p0", cache.get("8850000000000")!!.productName)
    }
}
