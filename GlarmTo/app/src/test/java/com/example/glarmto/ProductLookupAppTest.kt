package com.example.glarmto

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.util.BarcodeNutrition
import com.example.glarmto.data.util.LookupResult
import com.example.glarmto.data.util.ProductCache
import com.example.glarmto.data.util.ProductLookup
import com.example.glarmto.data.util.ProductOrigin
import com.example.glarmto.data.util.SharedPreferencesStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The real app wiring: the bundled asset, the caches on SharedPreferences, and the offline behaviour. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = GlarmToApplication::class)
class ProductLookupAppTest {

    private val app get() = ApplicationProvider.getApplicationContext<GlarmToApplication>()

    @Before
    fun cleanAndGoOffline() {
        listOf("glarmto_product_cache", "glarmto_my_products").forEach {
            app.getSharedPreferences(it, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        }
        // Robolectric reports a connected network by default, which would make the lookup really call
        // Open Food Facts. These tests must never touch the real network, so switch it off.
        val connectivity = app.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        org.robolectric.Shadows.shadowOf(connectivity).setActiveNetworkInfo(null)
    }

    @Test
    fun `a real Thai product is found from the bundled asset with no network`() = runBlocking {
        val result = app.productLookup.lookup("8851717021008")

        assertTrue(result is LookupResult.Found)
        val found = result as LookupResult.Found
        assertEquals(ProductOrigin.BUNDLED, found.origin)
        assertEquals(90, found.product.calories)
        assertTrue(found.product.productName.contains("Dutchie", ignoreCase = true))
    }

    @Test
    fun `scanning works when the same product is scanned again and again`() = runBlocking {
        repeat(3) {
            assertEquals(ProductOrigin.BUNDLED, (app.productLookup.lookup("8850228007716") as LookupResult.Found).origin)
        }
    }

    @Test
    fun `an unknown product with no internet says it could not search online`() = runBlocking {
        // Robolectric has no network, so the online search is skipped, and the answer says why.
        assertEquals(LookupResult.NotFoundOffline, app.productLookup.lookup("8859999999999"))
    }

    @Test
    fun `text that is not a barcode is simply not found`() = runBlocking {
        assertEquals(LookupResult.NotFound, app.productLookup.lookup("https://example.com"))
    }

    @Test
    fun `a product the user types in is remembered across app restarts`() = runBlocking {
        app.productLookup.remember("8859999999999", BarcodeNutrition("ปาท่องโก๋ร้านลุงหมี", 210, 0, 0, 0))

        // A brand new lookup object reads the same saved preferences, like the next time the app opens.
        val restarted = ProductLookup(
            bundled = emptyList(), online = null, cache = null, isOnline = { false },
            mine = ProductCache(SharedPreferencesStore(app.getSharedPreferences("glarmto_my_products", android.content.Context.MODE_PRIVATE)), 5000)
        )
        val found = restarted.lookup("8859999999999") as LookupResult.Found
        assertEquals(ProductOrigin.MINE, found.origin)
        assertEquals(210, found.product.calories)
        assertEquals("ปาท่องโก๋ร้านลุงหมี", found.product.productName)
    }

    @Test
    fun `what the user typed overrides the bundled data for that barcode`() = runBlocking {
        app.productLookup.remember("8851717021008", BarcodeNutrition("My own numbers", 77, 0, 0, 0))

        val found = app.productLookup.lookup("8851717021008") as LookupResult.Found

        assertEquals(ProductOrigin.MINE, found.origin)
        assertEquals(77, found.product.calories)
    }

    @Test
    fun `a name only bundled product is returned, not reported as unknown`() = runBlocking {
        // 9999990869043 is in the list by name only (no nutrition facts).
        val found = app.productLookup.lookup("9999990869043") as LookupResult.Found

        assertFalse(found.product.nutritionKnown)
        assertTrue(found.product.productName.isNotBlank())
    }

    @Test
    fun `after the user fills in a name only product it has nutrition`() = runBlocking {
        val first = app.productLookup.lookup("9999990869043") as LookupResult.Found
        app.productLookup.remember("9999990869043", first.product.copy(calories = 150))

        val second = app.productLookup.lookup("9999990869043") as LookupResult.Found

        assertTrue(second.product.nutritionKnown)
        assertEquals(150, second.product.calories)
        assertEquals(ProductOrigin.MINE, second.origin)
    }

    @Test
    fun `the application is the single shared lookup`() {
        assertTrue(app.productLookup === app.productLookup)
        val plainApp: Application = app
        assertTrue(plainApp is GlarmToApplication)
    }
}
