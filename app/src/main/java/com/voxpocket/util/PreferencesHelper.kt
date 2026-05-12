package com.voxpocket.util

import android.content.Context
import androidx.preference.PreferenceManager

class PreferencesHelper(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun saveModelPath(path: String) {
        prefs.edit().putString("model_path", path).apply()
    }

    fun getModelPath(): String? = prefs.getString("model_path", null)

    fun clearModelPath() {
        prefs.edit().remove("model_path").apply()
    }
}

