package com.example.myfit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.service.StepTrackingService
import com.example.myfit.ui.ColorPreset
import com.example.myfit.ui.MyFitViewModel
import com.example.myfit.ui.RequestActivityPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        startStepService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startStepService()
        }

        setContent {
            var permissionApproved by remember { mutableStateOf(false) }
            RequestActivityPermission {
                permissionApproved = true
                startStepService()
            }

            val viewModel: MyFitViewModel = viewModel()
            
            LaunchedEffect(Unit) {
                viewModel.refreshData()
            }

            MyFitApp(viewModel = viewModel)
        }
    }

    private fun startStepService() {
        try {
            val intent = Intent(this, StepTrackingService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun MyFitApp(viewModel: MyFitViewModel) {
    val theme = viewModel.selectedTheme
    val coroutineScope = rememberCoroutineScope()
    
    var showSettings by remember { mutableStateOf(false) }
    var showBadgeModal by remember { mutableStateOf(false) }
    var celebratedToday by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: History, 2: Badges

    val progress = if (viewModel.dailyGoal > 0) {
        (viewModel.currentSteps.toFloat() / viewModel.dailyGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val percentage = (progress * 100).toInt()

    if (viewModel.currentSteps >= viewModel.dailyGoal && !celebratedToday && viewModel.dailyGoal > 0) {
        showBadgeModal = true
        celebratedToday = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP HEADER
                HeaderSection(
                    themeColor = theme.primary,
                    onSettingsClick = { showSettings = !showSettings },
                    isSettingsOpen = showSettings
                )

                if (showSettings) {
                    // NEW APP CUSTOMIZATION MENU (Version 1.2.0)
                    SettingsMenuSection(viewModel = viewModel)
                } else {
                    when (selectedTab) {
                        0 -> {
                            // Dashboard Screen
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 2 & 3. TODAY'S STEPS & PROGRESS CIRCLE SECTION
                                StepsSection(
                                    currentSteps = viewModel.currentSteps,
                                    progress = progress,
                                    percentage = percentage,
                                    themeColor = theme.primary
                                )

                                // 4. GOAL PROGRESS BAR SECTION
                                GoalProgressSection(
                                    currentSteps = viewModel.currentSteps,
                                    dailyGoal = viewModel.dailyGoal,
                                    progress = progress,
                                    themeColor = theme.primary
                                )

                                // 5. STATS CARDS ROW (Calories, Distance, Active Time)
                                StatsCardsSection(
                                    currentSteps = viewModel.currentSteps,
                                    themeColor = theme.primary
                                )
                            }
                        }
                        1 -> {
                            // History Log Screen
                            HistorySection(viewModel = viewModel)
                        }
                        2 -> {
                            // Badges & Milestones Screen
                            BadgesSection(viewModel = viewModel)
                        }
                    }
                }
            }

            // 6 & 7. BOTTOM NAVIGATION & FOOTER TEXT
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!showSettings) {
                    BottomNavigationSection(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        themeColor = theme.primary
                    )
                }
                FooterSection()
            }
        }

        // Goal Achieved Celebratory Modal
        AnimatedVisibility(
            visible = showBadgeModal,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showBadgeModal = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = theme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GOAL ACHIEVED!",
                            color = Color(0xFF212121),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fantastic job! You crushed your daily target of ${viewModel.dailyGoal} steps.",
                            color = Color(0xFF757575),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showBadgeModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Awesome", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(showBadgeModal) {
        if (showBadgeModal) {
            delay(4000)
            showBadgeModal = false
        }
    }
}

// 1. TOP HEADER COMPOSABLE
@Composable
fun HeaderSection(themeColor: Color, onSettingsClick: () -> Unit, isSettingsOpen: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                )
                Text(
                    text = "MyFit",
                    color = Color(0xFF212121),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSettingsOpen) themeColor.copy(alpha = 0.15f) else Color(0xFFF8F9FA))
            ) {
                Icon(
                    imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isSettingsOpen) themeColor else Color(0xFF757575),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// 2 & 3. TODAY'S STEPS & PROGRESS CIRCLE SECTION
@Composable
fun StepsSection(currentSteps: Int, progress: Float, percentage: Int, themeColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Today's Steps",
                    color = Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "%,d Steps".format(currentSteps),
                    color = Color(0xFF212121),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFE0E0E0),
                    strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    color = themeColor,
                    strokeWidth = 8.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        color = Color(0xFF212121),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Goal",
                        color = Color(0xFF757575),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// 4. GOAL PROGRESS BAR SECTION
@Composable
fun GoalProgressSection(currentSteps: Int, dailyGoal: Int, progress: Float, themeColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Goal",
                    color = Color(0xFF212121),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%,d / %,d".format(currentSteps, dailyGoal),
                    color = Color(0xFF757575),
                    fontSize = 12.sp
                )
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = themeColor,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

// 5. STATS CARDS ROW SECTION
@Composable
fun StatsCardsSection(currentSteps: Int, themeColor: Color) {
    val calories = (currentSteps * 0.045).toInt()
    val distanceKm = currentSteps * 0.76 / 1000.0
    val activeMinutes = currentSteps / 100
    val hours = activeMinutes / 60
    val mins = activeMinutes % 60
    val activeTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    val distanceStr = String.format(Locale.getDefault(), "%.1f km", distanceKm)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            icon = Icons.Default.LocalFireDepartment,
            label = "Calories",
            value = "$calories kcal",
            themeColor = themeColor,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Map,
            label = "Distance",
            value = distanceStr,
            themeColor = themeColor,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Schedule,
            label = "Active Time",
            value = activeTimeStr,
            themeColor = themeColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, themeColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                color = Color(0xFF757575),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color(0xFF212121),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 6. BOTTOM NAVIGATION SECTION
@Composable
fun BottomNavigationSection(selectedTab: Int, onTabSelected: (Int) -> Unit, themeColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                title = "Dashboard",
                selectedIcon = Icons.Default.Home,
                unselectedIcon = Icons.Outlined.Home,
                isSelected = selectedTab == 0,
                themeColor = themeColor,
                onClick = { onTabSelected(0) }
            )
            NavBarItem(
                title = "History",
                selectedIcon = Icons.Default.History,
                unselectedIcon = Icons.Outlined.History,
                isSelected = selectedTab == 1,
                themeColor = themeColor,
                onClick = { onTabSelected(1) }
            )
            NavBarItem(
                title = "Badges",
                selectedIcon = Icons.Default.EmojiEvents,
                unselectedIcon = Icons.Outlined.EmojiEvents,
                isSelected = selectedTab == 2,
                themeColor = themeColor,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
fun NavBarItem(title: String, selectedIcon: androidx.compose.ui.graphics.vector.ImageVector, unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, themeColor: Color, onClick: () -> Unit) {
    val bg = if (isSelected) themeColor.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isSelected) themeColor else Color(0xFF757575)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 7. FOOTER TEXT
@Composable
fun FooterSection() {
    Text(
        text = "by Sujal",
        color = Color(0xFF757575),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

// NEW APP CUSTOMIZATION MENU (Version 1.2.0)
@Composable
fun SettingsMenuSection(viewModel: MyFitViewModel) {
    val theme = viewModel.selectedTheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = theme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = "App Customization v1.2.0",
                    color = Color(0xFF212121),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = Color(0xFFE0E0E0))

            // Theme Accent Selection
            Text(
                text = "Theme Accent Palette",
                color = Color(0xFF757575),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ColorPreset.entries.forEach { preset ->
                    val isSelected = theme == preset
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(preset.primary)
                            .border(
                                2.5.dp,
                                if (isSelected) Color(0xFF212121) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.updateTheme(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = Color(0xFFE0E0E0))

            // Daily Goal Adjustment Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Step Target",
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%,d steps".format(viewModel.dailyGoal),
                    color = theme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = viewModel.dailyGoal.toFloat(),
                onValueChange = { viewModel.updateGoal((it / 500).toInt() * 500) },
                valueRange = 2000f..30000f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.primary,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
    }
}

// History Screen
@Composable
fun HistorySection(viewModel: MyFitViewModel) {
    val historyList by viewModel.stepHistory.collectAsState()
    val theme = viewModel.selectedTheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Step History Logs",
            color = Color(0xFF212121),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        if (historyList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No history records found yet. Keep walking!", color = Color(0xFF757575), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList.reversed()) { entity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = entity.date, color = Color(0xFF212121), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Daily Walk Record", color = Color(0xFF757575), fontSize = 11.sp)
                            }
                            Text(
                                text = "%,d steps".format(entity.steps),
                                color = theme.primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Badges Screen
@Composable
fun BadgesSection(viewModel: MyFitViewModel) {
    val theme = viewModel.selectedTheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Fitness Milestones & Badges",
            color = Color(0xFF212121),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.achievements) { achievement ->
                val unlocked = viewModel.currentSteps >= achievement.threshold
                val progress = (viewModel.currentSteps.toFloat() / achievement.threshold.toFloat()).coerceIn(0f, 1f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (unlocked) theme.primary.copy(alpha = 0.2f) else Color(0xFFF0F0F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (unlocked) theme.primary else Color(0xFFBDBDBD),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = achievement.title,
                                    color = Color(0xFF212121),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = achievement.description,
                                    color = Color(0xFF757575),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = theme.primary,
                                    trackColor = Color(0xFFE0E0E0)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (unlocked) theme.primary else Color(0xFFF0F0F0))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (unlocked) "WON" else "${(progress * 100).toInt()}%",
                                color = if (unlocked) Color.White else Color(0xFF757575),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
