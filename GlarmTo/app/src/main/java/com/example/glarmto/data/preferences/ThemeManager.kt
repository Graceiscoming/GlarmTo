package com.example.glarmto.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("glarmto_theme_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "app_theme"
        
        const val THEME_CLASSIC = "GlarmTo Classic"
        const val THEME_OCEAN = "Ocean Flow"
        const val THEME_NEON = "Neon Energy"
        const val THEME_FOREST = "Forest Ground"
        const val THEME_AURA = "Aura 120Hz"

        val availableThemes = listOf(
            THEME_CLASSIC,
            THEME_OCEAN,
            THEME_NEON,
            THEME_FOREST,
            THEME_AURA
        )
    }

    private val _currentTheme = MutableStateFlow(getTheme())
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == KEY_THEME) {
            _currentTheme.value = sharedPreferences.getString(KEY_THEME, THEME_CLASSIC) ?: THEME_CLASSIC
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, THEME_CLASSIC) ?: THEME_CLASSIC
    }
}
