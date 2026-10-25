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

enum class ColorPreset(val primary: Color, val darkPrimary: Color, val label: String) {
    EMERALD_GREEN(Color(0xFF4CAF50), Color(0xFF2E7D32), "Emerald Green"),
    OCEAN_BLUE(Color(0xFF2196F3), Color(0xFF1565C0), "Ocean Blue"),
    SUNSET_ORANGE(Color(0xFFFF9800), Color(0xFFEF6C00), "Sunset Orange"),
    ROYAL_PURPLE(Color(0xFF9C27B0), Color(0xFF6A1B9A), "Royal Purple")
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val threshold: Int,
    val icon: String
)

class MyFitViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val stepDao = AppDatabase.getDatabase(application).stepDao()

    var currentSteps by mutableStateOf(0)
        private set
    var dailyGoal by mutableStateOf(prefs.getGoal())
        private set
    var notificationsEnabled by mutableStateOf(prefs.getNotificationsEnabled())
        private set
    var selectedTheme by mutableStateOf(
        try { ColorPreset.valueOf(prefs.getTheme()) } catch (e: Exception) { ColorPreset.EMERALD_GREEN }
    )
        private set

    val achievements = listOf(
        Achievement("starter", "Step Starter", "Reach 1,000 steps in a day", 1000, "👟"),
        Achievement("fitness", "Fitness Initiate", "Reach 5,000 steps in a day", 5000, "🔥"),
        Achievement("addict", "Step Addict", "Reach 10,000 steps in a day", 10000, "⚡"),
        Achievement("marathon", "Marathon Master", "Reach 20,000 steps in a day", 20000, "🏆"),
        Achievement("legend", "Legendary Walker", "Reach 50,000 steps in a day", 50000, "👑")
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

    fun updateTheme(preset: ColorPreset) {
        selectedTheme = preset
        prefs.saveTheme(preset.name)
    }

    fun updateGoal(goal: Int) {
        dailyGoal = goal
        prefs.saveGoal(goal)
    }

    fun updateNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.saveNotificationsEnabled(enabled)
    }

    fun refreshData() {
        observeTodaySteps()
    }
}
