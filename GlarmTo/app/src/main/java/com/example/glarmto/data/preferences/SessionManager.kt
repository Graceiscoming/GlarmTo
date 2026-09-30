package com.example.glarmto.data.preferences

import android.content.Context
import android.content.SharedPreferences

open class SessionManager(context: Context?) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("glarmto_prefs", Context.MODE_PRIVATE)

    open fun loginUser(username: String) {
        prefs?.edit()?.putString(KEY_USERNAME, username)?.apply()
    }

    open fun setProfileSetup(setup: Boolean) {
        prefs?.edit()?.putBoolean("${KEY_PROFILE_SETUP}_${getCurrentUser()}", setup)?.apply()
    }

    open fun isProfileSetup(): Boolean {
        return prefs?.getBoolean("${KEY_PROFILE_SETUP}_${getCurrentUser()}", false) ?: false
    }

    open fun logoutUser() {
        val user = getCurrentUser()
        prefs?.edit()?.apply {
            user?.let { remove("${KEY_PROFILE_SETUP}_$it") }
            remove(KEY_USERNAME)
        }?.apply()
    }

    open fun getCurrentUser(): String? {
        return prefs?.getString(KEY_USERNAME, null)
    }

    companion object {
        private const val KEY_USERNAME = "current_username"
        private const val KEY_PROFILE_SETUP = "profile_setup"
    }
}
