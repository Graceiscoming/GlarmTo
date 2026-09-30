package com.example.glarmto.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources

/**
 * Wraps the activity's context so that string lookups use [localizedResources] (the language picked in
 * the app), while everything else still reaches the real activity through [getBaseContext]. A plain
 * createConfigurationContext() would hide the activity, which breaks code that looks for it:
 * the workout screen's picture-in-picture timer and the camera permission request.
 */
class LocalizedContext(base: Context, private val localizedResources: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}

/** The activity behind this context, looking through any [ContextWrapper]s, or null if there is none. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
