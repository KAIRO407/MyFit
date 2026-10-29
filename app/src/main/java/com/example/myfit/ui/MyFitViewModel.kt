package com.example.myfit.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.AppDatabase
import com.example.myfit.data.PreferencesManager
import com.example.myfit.data.StepEntity
import com.example.myfit.data.UserAccountEntity
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
    SOLAR_GOLD(Color(0xFFFFD700), Color(0xFFFFB800), "Solar Gold"),
    CRIMSON_FIRE(Color(0xFFFF1744), Color(0xFFFF5252), "Crimson Fire"),
    SUNSET_ORANGE(Color(0xFFFF5722), Color(0xFFFF9800), "Sunset Orange"),
    ELECTRIC_VIOLET(Color(0xFF9C27B0), Color(0xFFE040FB), "Electric Violet"),
    MIDNIGHT_BLUE(Color(0xFF2979FF), Color(0xFF00B0FF), "Midnight Blue")
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
    val type: String,
    val icon: String
)

class MyFitViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val stepDao = AppDatabase.getDatabase(application).stepDao()
    private val accountDao = AppDatabase.getDatabase(application).userAccountDao()

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
    var notificationsEnabled by mutableStateOf(prefs.getNotifications())
        private set

    // Local Account State
    var userAccount by mutableStateOf<UserAccountEntity?>(null)
        private set

    // Real calculated statistics from Room DB history
    var lifetimeSteps by mutableStateOf(0)
        private set
    var weeklyAverage by mutableStateOf(0)
        private set
    var monthlyAverage by mutableStateOf(0)
        private set
    var currentStreak by mutableStateOf(0)
        private set
    var bestStreak by mutableStateOf(0)
        private set

    val richAchievements = listOf(
        RichAchievement("First Step", "Take your very first tracked step", 1, "steps", "👟"),
        RichAchievement("Century Walk", "Reach 100 steps in a day", 100, "steps", "🎯"),
        RichAchievement("Walking Enthusiast", "Reach 5,000 steps in a day", 5000, "steps", "⚡"),
        RichAchievement("10k Master", "Crush the 10,000 daily step target", 10000, "steps", "🔥"),
        RichAchievement("Half Marathon", "Walk 21,000 steps in a single day", 21000, "steps", "🏃"),
        RichAchievement("Marathon Walker", "Walk 42,000 steps in a single day", 42000, "steps", "🏅"),
        RichAchievement("3 Day Streak", "Maintain a 3-day walking streak", 3, "streak", "🌱"),
        RichAchievement("7 Day Streak", "Maintain a 7-day walking streak", 7, "streak", "🔥"),
        RichAchievement("14 Day Streak", "Maintain a 14-day walking streak", 14, "streak", "⚡"),
        RichAchievement("30 Day Streak", "Maintain a 30-day walking streak", 30, "streak", "👑"),
        RichAchievement("100 Day Streak", "Maintain an epic 100-day walking streak", 100, "streak", "🏆"),
        RichAchievement("Century Club", "Accumulate 100,000 lifetime steps", 100000, "lifetime", "⭐"),
        RichAchievement("Half Million", "Accumulate 500,000 lifetime steps", 500000, "lifetime", "🌟"),
        RichAchievement("Millionaire Walker", "Accumulate 1,000,000 lifetime steps", 1000000, "lifetime", "💎"),
        RichAchievement("Legendary Voyager", "Accumulate 5,000,000 lifetime steps", 5000000, "lifetime", "🌌")
    )

    val stepHistory: StateFlow<List<StepEntity>> = stepDao.getAllDailySteps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeTodayStepsAndHistory()
        observeAccount()
    }

    private fun observeAccount() {
        viewModelScope.launch {
            accountDao.getAccountFlow().collect { account ->
                userAccount = account
            }
        }
    }

    fun createAccount(username: String, avatar: String) {
        viewModelScope.launch {
            val newAcc = UserAccountEntity(username = username, avatar = avatar, level = 1, xp = 0, coins = 100)
            accountDao.saveAccount(newAcc)
            userAccount = newAcc
        }
    }

    fun signOutAccount() {
        viewModelScope.launch {
            accountDao.deleteAccount()
            userAccount = null
        }
    }

    private fun observeTodayStepsAndHistory() {
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

        viewModelScope.launch {
            stepDao.getAllDailySteps().collect { history ->
                if (history.isNotEmpty()) {
                    val total = history.sumOf { it.steps }
                    lifetimeSteps = total
                    weeklyAverage = total / history.size
                    monthlyAverage = weeklyAverage

                    var streak = 0
                    var best = 0
                    val sortedHistory = history.sortedBy { it.date }
                    for (item in sortedHistory) {
                        if (item.steps >= 1000 || (item.date == todayStr && currentSteps >= 1000)) {
                            streak++
                            if (streak > best) best = streak
                        } else if (item.date != todayStr) {
                            streak = 0
                        }
                    }
                    currentStreak = streak.coerceAtLeast(if (currentSteps >= 1000) 1 else 0)
                    bestStreak = best.coerceAtLeast(currentStreak)
                } else {
                    lifetimeSteps = currentSteps
                    weeklyAverage = currentSteps
                    monthlyAverage = currentSteps
                    currentStreak = if (currentSteps >= 1000) 1 else 0
                    bestStreak = currentStreak
                }
            }
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

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.saveNotifications(enabled)
    }

    fun refreshData() {
        observeTodayStepsAndHistory()
    }
}
