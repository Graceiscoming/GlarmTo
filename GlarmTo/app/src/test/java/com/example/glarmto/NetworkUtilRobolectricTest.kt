package com.example.glarmto

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.util.NetworkUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowConnectivityManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O]) // Test using NetworkCapabilities logic
class NetworkUtilRobolectricTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager

    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowConnectivityManager = shadowOf(connectivityManager)
    }

    @Test
    fun testIsInternetAvailable_whenNoNetwork_shouldReturnFalse() {
        // Explicitly clear all networks in Robolectric
        shadowConnectivityManager.setActiveNetworkInfo(null)
        
        val result = NetworkUtil.isInternetAvailable(context)
        assertFalse("Expected internet to be reported as unavailable", result)
    }
}
