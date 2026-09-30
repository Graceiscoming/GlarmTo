package com.example.glarmto

import android.app.Application
import com.example.glarmto.data.local.AppDatabase
import com.example.glarmto.data.repository.GlarmToRepository

import com.example.glarmto.data.preferences.SessionManager

class GlarmToApplication : Application() {
    // Lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionManager by lazy { SessionManager(this) }
    val themeManager by lazy { com.example.glarmto.data.preferences.ThemeManager(this) }
    val repository by lazy { GlarmToRepository(database.glarmToDao(), sessionManager) }
}
