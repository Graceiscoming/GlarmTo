package com.example.glarmto.data.util

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import com.example.glarmto.GlarmToApplication

/**
 * User-facing text for code that isn't a composable (ViewModels, the workout generator, share
 * sheets). It is looked up at the moment it is needed, in the language the app is showing then.
 */
fun interface AppTexts {
    fun get(@StringRes id: Int, vararg args: Any): String
}

/** [AppTexts] backed by the string resources, following the language picked in the app. */
class ResourceTexts(private val app: Application) : AppTexts {
    override fun get(id: Int, vararg args: Any): String = localizedContext().getString(id, *args)

    private fun localizedContext(): Context =
        (app as? GlarmToApplication)?.languageManager?.localizedContext(app) ?: app
}
