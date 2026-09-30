package com.example.glarmto

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.glarmto.data.preferences.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SessionManagerRobolectricTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("glarmto_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `login persists username`() {
        val sm = SessionManager(context)
        sm.loginUser("alice")
        assertEquals("alice", sm.getCurrentUser())

        val sm2 = SessionManager(context)
        assertEquals("alice", sm2.getCurrentUser())
    }

    @Test
    fun `profile setup flag is per user`() {
        val sm = SessionManager(context)
        sm.loginUser("u1")
        sm.setProfileSetup(true)
        assertTrue(sm.isProfileSetup())

        sm.loginUser("u2")
        assertFalse(sm.isProfileSetup())
        sm.setProfileSetup(false)

        sm.loginUser("u1")
        assertTrue(sm.isProfileSetup())
    }

    @Test
    fun `logout clears username`() {
        val sm = SessionManager(context)
        sm.loginUser("bob")
        sm.logoutUser()
        assertNull(sm.getCurrentUser())
    }
}
