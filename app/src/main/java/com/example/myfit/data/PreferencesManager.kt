package com.example.myfit.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MyFitPrefs", Context.MODE_PRIVATE)

    fun saveTheme(themeName: String) = prefs.edit().putString("key_theme", themeName).apply()
    fun getTheme(): String = prefs.getString("key_theme", "CYAN_GLOW") ?: "CYAN_GLOW"

    fun saveGoal(goal: Int) = prefs.edit().putInt("key_goal", goal).apply()
    fun getGoal(): Int = prefs.getInt("key_goal", 10000)

    fun saveTransparency(alpha: Float) = prefs.edit().putFloat("key_alpha", alpha).apply()
    fun getTransparency(): Float = prefs.getFloat("key_alpha", 0.15f)

    fun saveUiStyle(style: String) = prefs.edit().putString("key_ui_style", style).apply()
    fun getUiStyle(): String = prefs.getString("key_ui_style", "TRANSLUCENT") ?: "TRANSLUCENT"
}
