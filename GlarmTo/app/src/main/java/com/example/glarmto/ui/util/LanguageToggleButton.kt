package com.example.glarmto.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glarmto.R
import com.example.glarmto.data.preferences.LanguageManager

/**
 * The language switch: shows the code of the language you will switch TO ("TH" while the app is in
 * English, "EN" while it is in Thai) and switches when tapped.
 */
@Composable
fun LanguageToggleButton(languageManager: LanguageManager, modifier: Modifier = Modifier) {
    val language by languageManager.language.collectAsState()
    val description = stringResource(R.string.switch_language)
    // Compact on purpose (icon-sized): it sits in the dashboard's row of icon buttons, and anything wider
    // squeezes the greeting next to it on narrow phones.
    TextButton(
        onClick = { languageManager.toggle() },
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.size(40.dp).semantics { contentDescription = description }
    ) {
        Text(
            LanguageManager.toggleLabel(language),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Display text for a stored goal ("Cut", "Maintain", "Bulk") in the current language. */
@Composable
fun goalLabel(goal: String): String =
    com.example.glarmto.data.util.Goals.labelRes(goal)?.let { stringResource(it) } ?: goal
