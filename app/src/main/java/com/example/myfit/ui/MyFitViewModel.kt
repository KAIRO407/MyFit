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

data class StepAchievement(
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val icon: String
)

class MyFitViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val stepDao = AppDatabase.getDatabase(application).stepDao()

    var currentSteps by mutableStateOf(8452)
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

    // Level & XP System
    var userLevel by mutableStateOf(10)
        private set
    var levelTitle by mutableStateOf("Level 10 Adventurer")
        private set
    var currentXp by mutableStateOf(1850)
        private set
    var maxXp by mutableStateOf(2500)
        private set

    // Streaks
    var currentStreak by mutableStateOf(7)
        private set
    var bestStreak by mutableStateOf(30)
        private set
    var activeDays by mutableStateOf(42)
        private set

    // Statistics
    var lifetimeSteps by mutableStateOf(142850)
        private set
    var weeklyAverage by mutableStateOf(9120)
        private set
    var monthlyAverage by mutableStateOf(8640)
        private set
    var yearlyAverage by mutableStateOf(8400)
        private set

    // Settings
    var unitKm by mutableStateOf(true)
        private set
    var notificationsEnabled by mutableStateOf(true)
        private set

    val achievements = mutableStateListOf(
        StepAchievement("First 1000 Steps", "Completed your initial milestone", true, "🏆"),
        StepAchievement("First 5000 Steps", "Walked 5,000 steps in a day", true, "🏆"),
        StepAchievement("First 10000 Steps", "Crushed the 10k daily step target", true, "🏆"),
        StepAchievement("7 Day Streak", "Walked consistently for 7 days", true, "🔥"),
        StepAchievement("50000 Lifetime Steps", "Reached cumulative walking milestone", true, "👑"),
        StepAchievement("1 Million Steps", "Elite multi-million step club", false, "⭐")
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
                        currentXp += 5
                        if (currentXp >= maxXp) {
                            currentXp -= maxXp
                            userLevel += 1
                            levelTitle = when {
                                userLevel >= 100 -> "Level 100 Master Walker"
                                userLevel >= 50 -> "Level 50 Legend"
                                userLevel >= 25 -> "Level 25 Pathfinder"
                                userLevel >= 10 -> "Level 10 Adventurer"
                                userLevel >= 5 -> "Level 5 Explorer"
                                else -> "Level 1 Walker"
                            }
                            maxXp += 600
                        }
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
    }

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    fun refreshData() {
        observeTodaySteps()
    }
}
