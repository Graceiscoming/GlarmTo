package com.example.glarmto

import android.Manifest
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks the merged manifest: the permissions the app really needs, and nothing it doesn't use. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppManifestTest {

    private val requested: Set<String> by lazy {
        val context = ApplicationProvider.getApplicationContext<Application>()
        @Suppress("DEPRECATION")
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()
    }

    @Test
    fun `camera and internet are requested - scanner and barcode lookup need them`() {
        assertTrue(requested.contains(Manifest.permission.CAMERA))
        assertTrue(requested.contains(Manifest.permission.INTERNET))
    }

    @Test
    fun `automatic backup is off - the database holds password hashes and all training data`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun `microphone permission is not requested - voice logging uses the system recognizer`() {
        // RecognizerIntent opens the system speech UI, which holds its own microphone permission.
        assertFalse(requested.contains(Manifest.permission.RECORD_AUDIO))
    }
}
