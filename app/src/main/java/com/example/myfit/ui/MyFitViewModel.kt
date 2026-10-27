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
    var currentStreak by mutableStateOf(7)
        private set
    var bestStreak by mutableStateOf(30)
        private set
    var totalActiveDays by mutableStateOf(42)
        private set
    var lifetimeSteps by mutableStateOf(142850)
        private set
    var weeklyAverage by mutableStateOf(9120)
        private set
    var monthlyAverage by mutableStateOf(8640)
        private set

    val achievements = mutableStateListOf(
        StepAchievement("First 1000 Steps", "Completed your initial milestone", true, "✅"),
        StepAchievement("7 Day Streak", "Walked consistently for 7 days", true, "🔥"),
        StepAchievement("10000 Steps In One Day", "Crushed 10k steps target", true, "👟"),
        StepAchievement("50000 Lifetime Steps", "Reached cumulative walking milestone", true, "🏆")
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
                    stepDao.insertOrUpdateStep(StepEntity(todayStr, currentSteps))
                }

                stepDao.getStepEntityFlowForDate(todayStr).collect { entity ->
                    if (entity != null) {
                        currentSteps = entity.steps
                        lifetimeSteps += 1 // or accumulate properly
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateGoal(goal: Int) {
        dailyGoal = goal
        prefs.saveGoal(goal)
    }

    fun refreshData() {
        observeTodaySteps()
    }
}
