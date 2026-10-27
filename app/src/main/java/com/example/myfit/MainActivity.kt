package com.example.myfit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.service.StepTrackingService
import com.example.myfit.ui.MyFitViewModel
import com.example.myfit.ui.RequestActivityPermission
import com.example.myfit.ui.StepVersePreset
import com.example.myfit.ui.UiStyle
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

            StepVerseApp(viewModel = viewModel)
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
fun StepVerseApp(viewModel: MyFitViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Stats, 2: Achievements, 3: Profile
    var showSettings by remember { mutableStateOf(false) }

    val progress = if (viewModel.dailyGoal > 0) {
        (viewModel.currentSteps.toFloat() / viewModel.dailyGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
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
                // APP HEADER WITH SETTINGS GEAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "StepVerse",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1A1A1A))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "🔥", fontSize = 13.sp)
                                Text(text = "${viewModel.currentStreak} Day Streak", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = { showSettings = !showSettings },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1A1A1A))
                                .border(1.dp, if (showSettings) Color(0xFF00E5FF) else Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = if (showSettings) Icons.Default.Close else Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (showSettings) Color(0xFF00E5FF) else Color(0xFFA0A0A0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (showSettings) {
                    // RESTORED APP CUSTOMIZATION MENU
                    CustomizationMenu(viewModel = viewModel)
                } else {
                    // TAB ROUTER
                    when (selectedTab) {
                        0 -> HomeScreen(viewModel = viewModel, progress = progress)
                        1 -> StatsScreen(viewModel = viewModel)
                        2 -> AchievementsScreen(viewModel = viewModel)
                        3 -> ProfileScreen(viewModel = viewModel)
                    }
                }
            }

            // BOTTOM NAVIGATION
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!showSettings) {
                    CleanBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
                Text(text = "StepVerse • by Sujal", color = Color(0xFF707070), fontSize = 10.sp)
            }
        }
    }
}

// RESTORED APP CUSTOMIZATION MENU
@Composable
fun CustomizationMenu(viewModel: MyFitViewModel) {
    val theme = viewModel.selectedTheme
    val uiStyle = viewModel.selectedUiStyle

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(text = "App Customization Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "Personalize your StepVerse experience.", color = Color(0xFFA0A0A0), fontSize = 12.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Theme Palette", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StepVersePreset.entries.forEach { preset ->
                            val isSelected = theme == preset
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(preset.primary)
                                    .border(
                                        2.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateTheme(preset) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "App UI Style", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        UiStyle.entries.forEach { style ->
                            val isSelected = uiStyle == style
                            Button(
                                onClick = { viewModel.updateUiStyle(style) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2A2A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = style.name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
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
                        Text(text = "Daily Step Goal", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "%,d steps".format(viewModel.dailyGoal), color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = viewModel.dailyGoal.toFloat(),
                        onValueChange = { viewModel.updateGoal((it / 500).toInt() * 500) },
                        valueRange = 2000f..30000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF2A2A2A)
                        )
                    )
                }
            }
        }

        if (uiStyle != UiStyle.OPAQUE) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
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
                            Text(text = "Translucency Intensity", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${(viewModel.selectedCardAlpha * 100).toInt()}%", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = viewModel.selectedCardAlpha,
                            onValueChange = { viewModel.updateTransparency(it) },
                            valueRange = 0.05f..0.45f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color(0xFF2A2A2A)
                            )
                        )
                    }
                }
            }
        }
    }
}

// HOME SCREEN
@Composable
fun HomeScreen(viewModel: MyFitViewModel, progress: Float) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF2A2A2A),
                            strokeWidth = 12.dp
                        )
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF00E5FF),
                            strokeWidth = 12.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%,d".format(viewModel.currentSteps),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Today's Steps",
                                color = Color(0xFFA0A0A0),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem("Distance", String.format(Locale.getDefault(), "%.1f km", viewModel.currentSteps * 0.76 / 1000))
                        MetricItem("Calories", "${(viewModel.currentSteps * 0.04).toInt()} kcal")
                        MetricItem("Streak", "${viewModel.currentStreak} Days")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Daily Goal", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "%,d / %,d Steps".format(viewModel.currentSteps, viewModel.dailyGoal), color = Color(0xFFA0A0A0), fontSize = 12.sp)
                    }
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00FF94),
                        trackColor = Color(0xFF2A2A2A)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "🔥", fontSize = 28.sp)
                        Column {
                            Text(text = "${viewModel.currentStreak}-Day Streak", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Best Streak: ${viewModel.bestStreak} Days", color = Color(0xFFA0A0A0), fontSize = 11.sp)
                        }
                    }
                    Text(text = "${viewModel.totalActiveDays} Active Days", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// STATISTICS PAGE
@Composable
fun StatsScreen(viewModel: MyFitViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(text = "Statistics & Averages", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "Your walking performance over time.", color = Color(0xFFA0A0A0), fontSize = 12.sp)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox("Daily Steps", "%,d".format(viewModel.currentSteps), Modifier.weight(1f))
                StatBox("Weekly Avg", "%,d".format(viewModel.weeklyAverage), Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox("Monthly Avg", "%,d".format(viewModel.monthlyAverage), Modifier.weight(1f))
                StatBox("Lifetime Steps", "%,d".format(viewModel.lifetimeSteps), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, color = Color(0xFFA0A0A0), fontSize = 11.sp)
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ACHIEVEMENTS SECTION
@Composable
fun AchievementsScreen(viewModel: MyFitViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Achievements", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "Milestones unlocked along your journey.", color = Color(0xFFA0A0A0), fontSize = 12.sp)
        }

        items(viewModel.achievements) { ach ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = ach.icon, fontSize = 24.sp)
                        Column {
                            Text(text = ach.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = ach.description, color = Color(0xFFA0A0A0), fontSize = 11.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00FF94).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "UNLOCKED", color = Color(0xFF00FF94), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// PROFILE PAGE
@Composable
fun ProfileScreen(viewModel: MyFitViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👤", fontSize = 28.sp)
                    }
                    Text(text = "Sujal", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Daily Goal: %,d Steps".format(viewModel.dailyGoal), color = Color(0xFFA0A0A0), fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileRow("Total Lifetime Steps", "%,d".format(viewModel.lifetimeSteps))
                    ProfileRow("Longest Streak", "${viewModel.bestStreak} Days")
                    ProfileRow("Achievements Earned", "${viewModel.achievements.size}")
                }
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFA0A0A0), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFFA0A0A0), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// BOTTOM NAVIGATION
@Composable
fun CleanBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CleanNavItem("Home", Icons.Default.Home, selectedTab == 0) { onTabSelected(0) }
            CleanNavItem("Stats", Icons.Default.BarChart, selectedTab == 1) { onTabSelected(1) }
            CleanNavItem("Achievements", Icons.Default.EmojiEvents, selectedTab == 2) { onTabSelected(2) }
            CleanNavItem("Profile", Icons.Default.Person, selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun CleanNavItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFFA0A0A0)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            if (isSelected) {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
