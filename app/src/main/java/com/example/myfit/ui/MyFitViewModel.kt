package com.example.myfit.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.AppDatabase
import com.example.myfit.data.PreferencesManager
import com.example.myfit.data.StepEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class StepVersePreset(val primary: Color, val accent: Color, val label: String) {
    CYAN_PULSE(Color(0xFF00E5FF), Color(0xFF7B61FF), "Cyan Pulse"),
    VIBRANT_PURPLE(Color(0xFF7B61FF), Color(0xFF00E5FF), "Vibrant Purple"),
    EMERALD_GLOW(Color(0xFF00FF94), Color(0xFF00E5FF), "Emerald Glow"),
    SOLAR_GOLD(Color(0xFFFFD700), Color(0xFFFFB800), "Solar Gold")
}

enum class UiStyle(val label: String) {
    GLASSY("Glassy (Neon Glow)"),
    TRANSLUCENT("Translucent (Frosted)"),
    OPAQUE("Opaque (Solid Dark)")
}

data class RichAchievement(
    val title: String,
    val description: String,
    val target: Int,
    val icon: String
)

class MyFitViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val stepDao = AppDatabase.getDatabase(application).stepDao()

    var currentSteps by mutableStateOf(0)
        private set
    var dailyGoal by mutableStateOf(prefs.getGoal())
        private set
    var selectedCardAlpha by mutableStateOf(prefs.getTransparency())
        private set
    var selectedUiStyle by mutableStateOf(
        try { UiStyle.valueOf(prefs.getUiStyle()) } catch (e: Exception) { UiStyle.TRANSLUCENT }
    )
        private set
    var selectedTheme by mutableStateOf(
        try { StepVersePreset.valueOf(prefs.getTheme()) } catch (e: Exception) { StepVersePreset.CYAN_PULSE }
    )
        private set
    var unitKm by mutableStateOf(prefs.getUnitKm())
        private set
    var notificationsEnabled by mutableStateOf(prefs.getNotifications())
        private set

    var currentStreak by mutableStateOf(7)
        private set
    var bestStreak by mutableStateOf(30)
        private set
    var totalActiveDays by mutableStateOf(42)
        private set
    var userLevel by mutableStateOf(10)
        private set
    var currentXp by mutableStateOf(1850)
        private set
    var maxXp by mutableStateOf(2500)
        private set
    var lifetimeSteps by mutableStateOf(142850)
        private set
    var weeklyAverage by mutableStateOf(9120)
        private set
    var monthlyAverage by mutableStateOf(8640)
        private set

    val richAchievements = listOf(
        RichAchievement("First Step", "Take your very first tracked step", 1, "👟"),
        RichAchievement("Century Walk", "Reach 100 steps in a day", 100, "🎯"),
        RichAchievement("Walking Enthusiast", "Reach 5,000 steps in a day", 5000, "⚡"),
        RichAchievement("10k Master", "Crush the 10,000 daily step target", 10000, "🔥"),
        RichAchievement("Half Marathon", "Walk 21,000 steps in a single day", 21000, "🏃"),
        RichAchievement("Marathon Walker", "Walk 42,000 steps in a single day", 42000, "🏅"),
        RichAchievement("7 Day Streak", "Maintain a 7-day walking streak", 7, "🔥"),
        RichAchievement("30 Day Streak", "Maintain a 30-day walking streak", 30, "👑"),
        RichAchievement("Century Club", "Accumulate 100,000 lifetime steps", 100000, "⭐"),
        RichAchievement("Millionaire Walker", "Accumulate 1,000,000 lifetime steps", 1000000, "💎")
    )

    val stepHistory: StateFlow<List<StepEntity>> = stepDao.getAllDailySteps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeTodaySteps()
    }

    private fun observeTodaySteps() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        viewModelScope.launch {
            try {
                val existing = stepDao.getStepEntityForDate(todayStr)
                if (existing != null) {
                    currentSteps = existing.steps
                } else {
                    stepDao.insertOrUpdateStep(StepEntity(todayStr, 0))
                    currentSteps = 0
                }

                stepDao.getStepEntityFlowForDate(todayStr).collect { entity ->
                    if (entity != null) {
                        currentSteps = entity.steps
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateTheme(preset: StepVersePreset) {
        selectedTheme = preset
        prefs.saveTheme(preset.name)
    }

    fun updateGoal(goal: Int) {
        dailyGoal = goal
        prefs.saveGoal(goal)
    }

    fun updateTransparency(alpha: Float) {
        selectedCardAlpha = alpha
        prefs.saveTransparency(alpha)
    }

    fun updateUiStyle(style: UiStyle) {
        selectedUiStyle = style
        prefs.saveUiStyle(style.name)
    }

    fun toggleUnits(isKm: Boolean) {
        unitKm = isKm
        prefs.saveUnitKm(isKm)
    }

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.saveNotifications(enabled)
    }

    fun refreshData() {
        observeTodaySteps()
    }
}
