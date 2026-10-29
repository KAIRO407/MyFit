package com.example.myfit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.myfit.data.AppDatabase
import com.example.myfit.data.StepEntity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StepTrackingService : Service(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var initialSensorSteps = -1f
    private var baselineSavedSteps = 0
    private var currentSteps = 0
    private var lastSavedSteps = 0
    private var currentDateStr = ""
    private val channelId = "MyFitFitnessChannel"

    private var lastValidStepTime = 0L
    private val minStepIntervalMs = 380L

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(1, createNotification("MyFit step tracking active"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyFit::StepTrackingWakeLock").apply {
                acquire(10 * 60 * 1000L)
            }
        } catch (_: Exception) {}

        try {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        } catch (_: Exception) {}

        currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        loadTodayStepsAndRegisterSensors()
    }

    private fun loadTodayStepsAndRegisterSensors() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).stepDao()
                val existing = dao.getStepEntityForDate(dateStr)
                if (existing != null) {
                    currentSteps = existing.steps
                    baselineSavedSteps = existing.steps
                    lastSavedSteps = existing.steps
                } else {
                    dao.insertOrUpdateStep(StepEntity(dateStr, 0))
                    currentSteps = 0
                    baselineSavedSteps = 0
                    lastSavedSteps = 0
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                try {
                    stepCounterSensor?.let { sensor ->
                        sensorManager?.registerListener(this@StepTrackingService, sensor, SensorManager.SENSOR_DELAY_UI)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L)
            }
        } catch (_: Exception) {}
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = System.currentTimeMillis()

        if (todayStr != currentDateStr) {
            currentDateStr = todayStr
            currentSteps = 0
            baselineSavedSteps = 0
            lastSavedSteps = 0
            initialSensorSteps = -1f
            serviceScope.launch {
                try {
                    val dao = AppDatabase.getDatabase(applicationContext).stepDao()
                    dao.insertOrUpdateStep(StepEntity(todayStr, 0))
                } catch (_: Exception) {}
            }
        }

        val totalBootSteps = event.values[0]
        if (initialSensorSteps < 0f || totalBootSteps < initialSensorSteps) {
            initialSensorSteps = totalBootSteps - baselineSavedSteps
        }

        val calculated = (totalBootSteps - initialSensorSteps).toInt()
        if (calculated > currentSteps) {
            val stepDelta = calculated - currentSteps
            if (stepDelta > 0) {
                if (currentTime - lastValidStepTime >= minStepIntervalMs || stepDelta == 1) {
                    currentSteps = calculated.coerceAtLeast(baselineSavedSteps)
                    lastValidStepTime = currentTime
                } else {
                    initialSensorSteps += stepDelta
                }
            }
        }

        if (kotlin.math.abs(currentSteps - lastSavedSteps) >= 1) {
            lastSavedSteps = currentSteps
            serviceScope.launch {
                try {
                    val dao = AppDatabase.getDatabase(applicationContext).stepDao()
                    dao.insertOrUpdateStep(StepEntity(todayStr, currentSteps))
                } catch (_: Exception) {}
            }
            updateNotification("Today's Steps: $currentSteps")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        try {
            val channel = NotificationChannel(
                channelId,
                "MyFit Step Counter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background step tracking for MyFit"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        } catch (_: Exception) {}
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyFit StepVerse")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(1, createNotification(text))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            sensorManager?.unregisterListener(this)
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            serviceJob.cancel()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            runBlocking {
                try {
                    val dao = AppDatabase.getDatabase(applicationContext).stepDao()
                    dao.insertOrUpdateStep(StepEntity(dateStr, currentSteps))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
