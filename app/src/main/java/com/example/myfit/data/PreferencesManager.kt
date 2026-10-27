package com.example.myfit.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MyFitPrefs", Context.MODE_PRIVATE)

    fun saveTheme(themeName: String) = prefs.edit().putString("key_theme", themeName).apply()
    fun getTheme(): String = prefs.getString("key_theme", "EMERALD_GREEN") ?: "EMERALD_GREEN"

    fun saveGoal(goal: Int) = prefs.edit().putInt("key_goal", goal).apply()
    fun getGoal(): Int = prefs.getInt("key_goal", 10000)

    fun saveNotificationsEnabled(enabled: Boolean) = prefs.edit().putBoolean("key_notifications", enabled).apply()
    fun getNotificationsEnabled(): Boolean = prefs.getBoolean("key_notifications", true)
}
