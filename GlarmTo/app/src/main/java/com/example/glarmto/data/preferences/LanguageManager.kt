package com.example.glarmto.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * The language the app shows, chosen in the app (independent of the phone's language) and remembered.
 * Works like [ThemeManager]: observe [language], change it with [setLanguage] / [toggle].
 *
 * The first time the app runs it follows the phone: Thai if the phone is set to Thai, otherwise English.
 */
class LanguageManager(
    context: Context,
    private val deviceLanguage: () -> String = { Locale.getDefault().language }
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("glarmto_language_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LANGUAGE = "app_language"
        const val ENGLISH = "en"
        const val THAI = "th"
        val supported = listOf(ENGLISH, THAI)

        /** The language the toggle button switches to from [current]. */
        fun next(current: String): String = if (current == THAI) ENGLISH else THAI

        /** Text for the toggle button: the code of the language it will switch TO ("TH" while in English). */
        fun toggleLabel(current: String): String = next(current).uppercase(Locale.ROOT)

        fun localeFor(code: String): Locale = Locale(if (code == THAI) THAI else ENGLISH)
    }

    private val _language = MutableStateFlow(load())
    val language: StateFlow<String> = _language.asStateFlow()

    private fun load(): String {
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null && saved in supported) return saved
        return if (deviceLanguage() == THAI) THAI else ENGLISH
    }

    fun getLanguage(): String = _language.value

    fun setLanguage(code: String) {
        require(code in supported) { "unsupported language: $code" }
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        _language.value = code
    }

    /** Switches between English and Thai and returns the new language. */
    fun toggle(): String {
        val target = next(_language.value)
        setLanguage(target)
        return target
    }

    /** A context whose resources (strings, formats) are in [code], for code that has no Compose context. */
    fun localizedContext(base: Context, code: String = _language.value): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(localeFor(code))
        return base.createConfigurationContext(config)
    }
}
