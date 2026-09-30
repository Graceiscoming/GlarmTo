package com.example.glarmto.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.glarmto.data.preferences.LanguageManager
import java.util.Locale

/**
 * Makes everything inside [content] use the app's chosen language: `stringResource`, date pickers and
 * number/date formats all follow [LanguageManager.language], and switching it re-composes the screen
 * immediately without restarting the activity.
 */
@Composable
fun LanguageProvider(languageManager: LanguageManager, content: @Composable () -> Unit) {
    val language by languageManager.language.collectAsState()
    val baseContext = LocalContext.current

    val localizedContext = remember(language, baseContext) {
        LocalizedContext(baseContext, languageManager.localizedContext(baseContext, language).resources)
    }
    // Date and number formatting in the app reads the default locale.
    remember(language) { LanguageManager.localeFor(language).also { Locale.setDefault(it) } }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content
    )
}
