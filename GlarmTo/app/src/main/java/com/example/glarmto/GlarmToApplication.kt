package com.example.glarmto

import android.app.Application
import com.example.glarmto.data.local.AppDatabase
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.AssetProductDataset
import com.example.glarmto.data.util.NetworkUtil
import com.example.glarmto.data.util.OpenFoodFactsApi
import com.example.glarmto.data.util.ProductCache
import com.example.glarmto.data.util.ProductLookup
import com.example.glarmto.data.util.SharedPreferencesStore

import com.example.glarmto.data.preferences.SessionManager

class GlarmToApplication : Application() {
    // Lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionManager by lazy { SessionManager(this) }
    val themeManager by lazy { com.example.glarmto.data.preferences.ThemeManager(this) }
    val languageManager by lazy { com.example.glarmto.data.preferences.LanguageManager(this) }
    val repository by lazy { GlarmToRepository(database.glarmToDao(), sessionManager) }

    /**
     * Barcode search: the cache of earlier online hits, then the products bundled in the app (works offline),
     * then Open Food Facts online.
     */
    val productLookup by lazy {
        ProductLookup(
            bundled = listOf(AssetProductDataset { assets.open("thai_products.tsv") }),
            online = OpenFoodFactsApi,
            cache = ProductCache(SharedPreferencesStore(getSharedPreferences("glarmto_product_cache", MODE_PRIVATE))),
            isOnline = { NetworkUtil.isInternetAvailable(this) },
            // Products the user typed in: kept separately, and many more of them, because they can't be re-downloaded.
            mine = ProductCache(SharedPreferencesStore(getSharedPreferences("glarmto_my_products", MODE_PRIVATE)), maxEntries = 5000)
        )
    }
}
