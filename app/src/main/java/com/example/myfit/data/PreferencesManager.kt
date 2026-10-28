package com.example.myfit.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MyFitPrefs", Context.MODE_PRIVATE)

    fun saveTheme(themeName: String) = prefs.edit().putString("key_theme", themeName).apply()
    fun getTheme(): String = prefs.getString("key_theme", "CYAN_PULSE") ?: "CYAN_PULSE"

    fun saveGoal(goal: Int) = prefs.edit().putInt("key_goal", goal).apply()
    fun getGoal(): Int = prefs.getInt("key_goal", 10000)

    fun saveTransparency(alpha: Float) = prefs.edit().putFloat("key_alpha", alpha).apply()
    fun getTransparency(): Float = prefs.getFloat("key_alpha", 0.15f)

    fun saveUiStyle(style: String) = prefs.edit().putString("key_ui_style", style).apply()
    fun getUiStyle(): String = prefs.getString("key_ui_style", "TRANSLUCENT") ?: "TRANSLUCENT"

    fun saveUnitKm(isKm: Boolean) = prefs.edit().putBoolean("key_unit_km", isKm).apply()
    fun getUnitKm(): Boolean = prefs.getBoolean("key_unit_km", true)

    fun saveNotifications(enabled: Boolean) = prefs.edit().putBoolean("key_notif", enabled).apply()
    fun getNotifications(): Boolean = prefs.getBoolean("key_notif", true)
}
