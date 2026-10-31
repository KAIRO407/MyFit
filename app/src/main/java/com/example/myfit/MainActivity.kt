package com.example.myfit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.service.StepTrackingService
import com.example.myfit.ui.MyFitViewModel
import com.example.myfit.ui.RequestActivityPermission
import com.example.myfit.ui.StepVersePreset
import com.example.myfit.ui.UiStyle
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
        enableEdgeToEdge()
        
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

            MyFitStepVerseApp(viewModel = viewModel)
        }
    }

    private fun startStepService() {
        try {
            val intent = Intent(this, StepTrackingService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyFitStepVerseApp(viewModel: MyFitViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 }) // 0: Home, 1: Stats, 2: Achievements, 3: Profile
    val coroutineScope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    val primaryColor = viewModel.selectedTheme.primary
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // APP HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MyFit",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Every Step Matters.",
                            color = Color(0xFFA0A0A0),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF151515))
                                .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "🔥", fontSize = 13.sp)
                                Text(text = "${viewModel.currentStreak}d Streak", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = { showSettings = !showSettings },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF151515))
                                .border(1.dp, if (showSettings) primaryColor else Color(0xFF202020), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = if (showSettings) Icons.Default.Close else Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (showSettings) primaryColor else Color(0xFFA0A0A0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (showSettings) {
                    // SETTINGS & CUSTOMIZATION SCREEN
                    SettingsScreen(viewModel = viewModel)
                } else {
                    // SWIPEABLE HORIZONTAL PAGER
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen(viewModel = viewModel, progress = progress)
                            1 -> StatisticsScreen(viewModel = viewModel)
                            2 -> RichAchievementsScreen(viewModel = viewModel)
                            3 -> ProfileScreen(viewModel = viewModel)
                        }
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
                    BottomNavBar(
                        selectedTab = pagerState.currentPage,
                        onTabSelected = { tab ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab)
                            }
                        },
                        primaryColor = primaryColor
                    )
                }
                Text(text = "MyFit • by Sujal", color = Color(0xFFA0A0A0), fontSize = 10.sp)
            }
        }
    }
}

// HOME SCREEN
@Composable
fun HomeScreen(viewModel: MyFitViewModel, progress: Float) {
    val primaryColor = viewModel.selectedTheme.primary
    val activeMinutes = viewModel.currentSteps / 100
    val hours = activeMinutes / 60
    val mins = activeMinutes % 60
    val timeActiveStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    val distanceKm = viewModel.currentSteps * 0.76 / 1000.0
    val distanceStr = String.format(Locale.getDefault(), "%.1f km", distanceKm)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
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
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF202020),
                            strokeWidth = 14.dp
                        )
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            color = primaryColor,
                            strokeWidth = 14.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%,d".format(viewModel.currentSteps),
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "TODAY'S STEPS",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem("Distance", distanceStr)
                        MetricItem("Calories", "${(viewModel.currentSteps * 0.04).toInt()} kcal")
                        MetricItem("Active", timeActiveStr)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
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
                        Text(text = "Goal: %,d Steps".format(viewModel.dailyGoal), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${(progress * 100).toInt()}% Completed", color = Color(0xFF00FF94), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00FF94),
                        trackColor = Color(0xFF202020)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
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
                            Text(text = "${viewModel.currentStreak} Day Streak", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Best Streak: ${viewModel.bestStreak} Days", color = Color(0xFFA0A0A0), fontSize = 11.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "ACTIVE", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// STATISTICS SCREEN
@Composable
fun StatisticsScreen(viewModel: MyFitViewModel) {
    val primaryColor = viewModel.selectedTheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(text = "Walking Statistics", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Detailed analysis of your progress.", color = Color(0xFFA0A0A0), fontSize = 12.sp)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Daily Steps", "%,d".format(viewModel.currentSteps), primaryColor, Modifier.weight(1f))
                StatCard("Weekly Average", "%,d".format(viewModel.weeklyAverage), primaryColor, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Monthly Average", "%,d".format(viewModel.monthlyAverage), primaryColor, Modifier.weight(1f))
                StatCard("Lifetime Steps", "%,d".format(viewModel.lifetimeSteps), primaryColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, primaryColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, color = Color(0xFFA0A0A0), fontSize = 11.sp)
            Text(text = value, color = primaryColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// RICH ACHIEVEMENTS SCREEN WITH PROGRESS BARS
@Composable
fun RichAchievementsScreen(viewModel: MyFitViewModel) {
    val primaryColor = viewModel.selectedTheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Achievements & Badges", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Unlock milestones as you walk further.", color = Color(0xFFA0A0A0), fontSize = 12.sp)
        }

        items(viewModel.richAchievements) { ach ->
            val isUnlocked = when (ach.type) {
                "steps" -> viewModel.currentSteps >= ach.target || viewModel.lifetimeSteps >= ach.target
                "streak" -> viewModel.currentStreak >= ach.target || viewModel.bestStreak >= ach.target
                "lifetime" -> viewModel.lifetimeSteps >= ach.target
                else -> viewModel.currentSteps >= ach.target
            }
            val progress = when (ach.type) {
                "streak" -> (viewModel.currentStreak.toFloat() / ach.target.toFloat()).coerceIn(0f, 1f)
                "lifetime" -> (viewModel.lifetimeSteps.toFloat() / ach.target.toFloat()).coerceIn(0f, 1f)
                else -> (viewModel.currentSteps.toFloat() / ach.target.toFloat()).coerceIn(0f, 1f)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = ach.icon, fontSize = 26.sp)
                            Column {
                                Text(text = ach.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = ach.description, color = Color(0xFFA0A0A0), fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isUnlocked) "UNLOCKED" else "${(progress * 100).toInt()}%",
                                color = if (isUnlocked) primaryColor else Color(0xFFA0A0A0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isUnlocked) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = primaryColor,
                            trackColor = Color(0xFF202020)
                        )
                    }
                }
            }
        }
    }
}

// PROFILE SCREEN WITH LOCAL ACCOUNT SYSTEM & DELETION CONFIRMATION DIALOG
@Composable
fun ProfileScreen(viewModel: MyFitViewModel) {
    val primaryColor = viewModel.selectedTheme.primary
    val distanceKm = viewModel.lifetimeSteps * 0.76 / 1000.0
    val unlockedCount = viewModel.richAchievements.count {
        when (it.type) {
            "steps" -> viewModel.currentSteps >= it.target || viewModel.lifetimeSteps >= it.target
            "streak" -> viewModel.currentStreak >= it.target || viewModel.bestStreak >= it.target
            "lifetime" -> viewModel.lifetimeSteps >= it.target
            else -> viewModel.currentSteps >= it.target
        }
    }

    var usernameInput by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("🦊") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val avatars = listOf("👤", "🦊", "🦁", "🦄", "🤖", "🧙‍♂️", "⚡", "🚀", "🐅", "🐉", "🦅", "💎")

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val account = viewModel.userAccount
        if (account == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(text = "Create Local Account", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Save your step progress and profile locally on your device.", color = Color(0xFFA0A0A0), fontSize = 12.sp, textAlign = TextAlign.Center)

                        Text(text = "Choose Your Avatar", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(avatars) { av ->
                                val isSelected = selectedAvatar == av
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) primaryColor.copy(alpha = 0.3f) else Color(0xFF202020))
                                        .border(2.dp, if (isSelected) primaryColor else Color.Transparent, CircleShape)
                                        .clickable { selectedAvatar = av },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = av, fontSize = 22.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Enter Your Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF303030),
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = Color(0xFFA0A0A0),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank()) {
                                    viewModel.createAccount(usernameInput.trim(), selectedAvatar)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = usernameInput.isNotBlank()
                        ) {
                            Text(text = "Create Account", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.2f))
                                .border(2.dp, primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = account.avatar, fontSize = 32.sp)
                        }
                        Text(text = account.username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF7B61FF).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Local Explorer • Coins: ${account.coins}", color = Color(0xFF7B61FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Delete Account", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileRow("Current Goal", "%,d Steps".format(viewModel.dailyGoal))
                    ProfileRow("Lifetime Steps", "%,d".format(viewModel.lifetimeSteps))
                    ProfileRow("Longest Streak", "${viewModel.bestStreak} Days")
                    ProfileRow("Distance Walked", String.format(Locale.getDefault(), "%.1f km", distanceKm))
                    ProfileRow("Achievements Earned", "$unlockedCount / ${viewModel.richAchievements.size}")
                }
            }
        }
    }

    // Account Deletion Verification Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Local Account?", color = Color.White) },
            text = { Text("Are you sure you want to delete your local account? All saved profile data on this device will be permanently removed.", color = Color(0xFFA0A0A0)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.signOutAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Yes, Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            },
            containerColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// SETTINGS & CUSTOMIZATION SCREEN
@Composable
fun SettingsScreen(viewModel: MyFitViewModel) {
    val theme = viewModel.selectedTheme

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(text = "Settings & Customization", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Personalize your MyFit experience with rich theme options.", color = Color(0xFFA0A0A0), fontSize = 12.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Expanded Theme & Accent Colors", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    val rows = StepVersePreset.entries.chunked(4)
                    rows.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowPresets.forEach { preset ->
                                val isSelected = theme == preset
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.updateTheme(preset) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(preset.primary)
                                            .border(
                                                2.5.dp,
                                                if (isSelected) Color.White else Color.Transparent,
                                                RoundedCornerShape(14.dp)
                                            ),
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.label.split(" ")[0],
                                        color = if (isSelected) Color.White else Color(0xFFA0A0A0),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
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
                        Text(text = "Step Goals", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "%,d steps".format(viewModel.dailyGoal), color = theme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = viewModel.dailyGoal.toFloat(),
                        onValueChange = { viewModel.updateGoal((it / 500).toInt() * 500) },
                        valueRange = 2000f..30000f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primary,
                            activeTrackColor = theme.primary,
                            inactiveTrackColor = Color(0xFF202020)
                        )
                    )
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

// BOTTOM NAVIGATION (Glowing Active Tab)
@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit, primaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem("Home", Icons.Default.Home, selectedTab == 0, primaryColor) { onTabSelected(0) }
            NavItem("Statistics", Icons.Default.BarChart, selectedTab == 1, primaryColor) { onTabSelected(1) }
            NavItem("Achievements", Icons.Default.EmojiEvents, selectedTab == 2, primaryColor) { onTabSelected(2) }
            NavItem("Profile", Icons.Default.Person, selectedTab == 3, primaryColor) { onTabSelected(3) }
        }
    }
}

@Composable
fun NavItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, primaryColor: Color, onClick: () -> Unit) {
    val bg = if (isSelected) primaryColor.copy(alpha = 0.2f) else Color.Transparent
    val contentColor = if (isSelected) primaryColor else Color(0xFFA0A0A0)
    val glowModifier = if (isSelected) Modifier.border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)) else Modifier

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(glowModifier)
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
                modifier = Modifier.size(20.dp)
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
