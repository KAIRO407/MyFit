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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.myfit.ui.UiStyle
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyFitApp(viewModel: MyFitViewModel) {
    val theme = viewModel.selectedTheme
    val uiStyle = viewModel.selectedUiStyle
    val coroutineScope = rememberCoroutineScope()
    
    var showSettings by remember { mutableStateOf(false) }
    var showBadge by remember { mutableStateOf(false) }
    var celebratedToday by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 3 }) // 0: Dashboard, 1: History, 2: Badges

    val progress = if (viewModel.dailyGoal > 0) {
        (viewModel.currentSteps.toFloat() / viewModel.dailyGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    if (viewModel.currentSteps >= viewModel.dailyGoal && !celebratedToday && viewModel.dailyGoal > 0) {
        showBadge = true
        celebratedToday = true
    }

    val backgroundBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF010308),
            theme.primary.copy(alpha = 0.20f),
            Color(0xFF0A0E1A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(theme.primary)
                    )
                    Text(
                        text = "MyFit v1.1",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Top Tab Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("Dashboard", "History", "Badges")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index && !showSettings
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) theme.primary.copy(alpha = 0.25f) else Color.Transparent)
                                .border(1.dp, if (isSelected) theme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .clickable {
                                    showSettings = false
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (showSettings) {
                // Settings Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                            Text(text = "APP UI STYLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            UiStyle.entries.forEach { style ->
                                val isSelected = uiStyle == style
                                Button(
                                    onClick = { viewModel.updateUiStyle(style) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) theme.primary else Color.White.copy(alpha = 0.1f)
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

                    HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                            Text(text = "THEME PALETTE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val rows = ColorPreset.entries.chunked(4)
                            rows.forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowPresets.forEach { preset ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(preset.primary)
                                                .border(
                                                    2.dp,
                                                    if (theme == preset) Color.White else Color.Transparent,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.updateTheme(preset) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (theme == preset) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
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

                    HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                                Text(text = "DAILY GOAL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(text = "${viewModel.dailyGoal} steps", color = theme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = viewModel.dailyGoal.toFloat(),
                            onValueChange = { viewModel.updateGoal((it / 500).toInt() * 500) },
                            valueRange = 2000f..30000f,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.primary,
                                activeTrackColor = theme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    if (uiStyle != UiStyle.OPAQUE) {
                        HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                                    Text(text = "TRANSLUCENCY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(text = "${(viewModel.selectedCardAlpha * 100).toInt()}%", color = theme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = viewModel.selectedCardAlpha,
                                onValueChange = { viewModel.updateTransparency(it) },
                                valueRange = 0.05f..0.45f,
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.primary,
                                    activeTrackColor = theme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            } else {
                // Swipeable Pager for Dashboard (0), History (1), Badges (2)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> {
                            // Page 0: Dashboard
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = theme.primary, modifier = Modifier.size(16.dp))
                                                Text(text = "TODAY'S STEPS", color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${viewModel.currentSteps}",
                                                color = Color.White,
                                                fontSize = 46.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.size(72.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                progress = 1f,
                                                modifier = Modifier.fillMaxSize(),
                                                color = Color.White.copy(alpha = 0.08f),
                                                strokeWidth = 6.dp,
                                            )
                                            CircularProgressIndicator(
                                                progress = progress,
                                                modifier = Modifier.fillMaxSize(),
                                                color = theme.primary,
                                                strokeWidth = 6.dp,
                                            )
                                            Text(
                                                text = "${(progress * 100).toInt()}%",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = theme.primary,
                                        trackColor = Color.White.copy(alpha = 0.08f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "Goal: ${viewModel.dailyGoal}", color = Color.Gray, fontSize = 10.sp)
                                        Text(text = "${(viewModel.dailyGoal - viewModel.currentSteps).coerceAtLeast(0)} remaining", color = Color.Gray, fontSize = 10.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        HumanMetricItem(label = "CALORIES", value = "${(viewModel.currentSteps * 0.04).toInt()} kcal", color = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle, icon = Icons.Default.Favorite)
                                        HumanMetricItem(label = "DISTANCE", value = String.format(Locale.getDefault(), "%.2f km", viewModel.currentSteps * 0.76 / 1000), color = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle, icon = Icons.Default.List)
                                        HumanMetricItem(label = "ACTIVE", value = "${viewModel.currentSteps / 100} min", color = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle, icon = Icons.Default.Star)
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Page 1: History Log
                            val historyList by viewModel.stepHistory.collectAsState()
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "STEP HISTORY LOGS",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))

                                if (historyList.isEmpty()) {
                                    HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                                        Text(text = "No past history records yet. Keep walking!", color = Color.Gray, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(historyList.reversed()) { entity ->
                                            HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = entity.date, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "Daily Step Log", color = Color.Gray, fontSize = 11.sp)
                                                    }
                                                    Text(
                                                        text = "${entity.steps} steps",
                                                        color = theme.primary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Page 2: Badges & Achievements
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "FITNESS MILESTONES",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(viewModel.achievements) { achievement ->
                                        val unlocked = viewModel.currentSteps >= achievement.threshold
                                        val achievementProgress = (viewModel.currentSteps.toFloat() / achievement.threshold.toFloat()).coerceIn(0f, 1f)

                                        HumanCard(primaryColor = theme.primary, alpha = viewModel.selectedCardAlpha, uiStyle = uiStyle) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(CircleShape)
                                                            .background(if (unlocked) theme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                                            .border(1.5.dp, if (unlocked) theme.primary else Color.Transparent, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (unlocked) Icons.Default.Star else Icons.Default.Lock,
                                                            contentDescription = null,
                                                            tint = if (unlocked) theme.primary else Color.Gray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            text = achievement.title,
                                                            color = if (unlocked) Color.White else Color.Gray,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = achievement.description,
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            fontSize = 11.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        LinearProgressIndicator(
                                                            progress = achievementProgress,
                                                            modifier = Modifier
                                                                .width(120.dp)
                                                                .height(4.dp)
                                                                .clip(RoundedCornerShape(2.dp)),
                                                            color = if (unlocked) theme.primary else theme.primary.copy(alpha = 0.5f),
                                                            trackColor = Color.White.copy(alpha = 0.08f)
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (unlocked) theme.primary else Color.White.copy(alpha = 0.1f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (unlocked) "WON" else "${(achievementProgress * 100).toInt()}%",
                                                        color = if (unlocked) Color.Black else Color.White,
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
                    }
                }
            }
        }

        // ==========================================
        // SETTINGS & ACHIEVEMENTS ICONS: BOTTOM LEFT VERTICALLY
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, start = 4.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Achievements Button (Bottom Left)
                IconButton(
                    onClick = {
                        showSettings = false
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(theme.primary.copy(alpha = 0.2f))
                        .border(1.5.dp, theme.primary, CircleShape)
                        .shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Milestones",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings Button (Bottom Left, stacked vertically above or below)
                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (showSettings) theme.primary else Color.White.copy(alpha = 0.12f))
                        .border(1.5.dp, if (showSettings) Color.White else theme.primary.copy(alpha = 0.6f), CircleShape)
                        .shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = if (showSettings) Icons.Default.Home else Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (showSettings) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Goal Achieved Modal Dialog
        AnimatedVisibility(
            visible = showBadge,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showBadge = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
                            )
                        )
                        .border(1.5.dp, theme.primary, RoundedCornerShape(28.dp))
                        .shadow(16.dp, RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = theme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GOAL ACHIEVED!",
                            color = theme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Amazing work! You reached your daily target of ${viewModel.dailyGoal} steps.",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showBadge = false },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Awesome", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(showBadge) {
        if (showBadge) {
            delay(4000)
            showBadge = false
        }
    }
}

@Composable
fun HumanCard(primaryColor: Color, alpha: Float, uiStyle: UiStyle, content: @Composable ColumnScope.() -> Unit) {
    val cardBackground = when (uiStyle) {
        UiStyle.GLASSY -> Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha + 0.06f), Color.White.copy(alpha = alpha)))
        UiStyle.TRANSLUCENT -> Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = alpha * 0.5f)))
        UiStyle.OPAQUE -> Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF0F172A)))
    }

    val borderBrush = when (uiStyle) {
        UiStyle.GLASSY -> Brush.linearGradient(listOf(primaryColor, Color.White.copy(alpha = 0.5f)))
        UiStyle.TRANSLUCENT -> Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f)))
        UiStyle.OPAQUE -> Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBackground)
            .border(if (uiStyle == UiStyle.OPAQUE) 1.dp else 1.5.dp, borderBrush, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun HumanMetricItem(label: String, value: String, color: Color, alpha: Float, uiStyle: UiStyle, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val bg = when (uiStyle) {
        UiStyle.GLASSY -> Color.White.copy(alpha = alpha * 0.8f)
        UiStyle.TRANSLUCENT -> Color.White.copy(alpha = alpha * 0.4f)
        UiStyle.OPAQUE -> Color(0xFF1F2937)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
