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
import kotlin.math.sqrt

class StepTrackingService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var initialSensorSteps = -1f
    private var baselineSavedSteps = 0
    private var currentSteps = 0
    private var lastSavedSteps = 0
    private val channelId = "MyFitFitnessChannel"

    private var lastStepCounterEventTime = 0L
    private var lastMagnitude = 0.0f
    private var lastStepTimestamp = 0L
    private val minStepIntervalMs = 400L // 400ms between steps = max 150 steps/min

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("MyFit step tracking active"))

        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyFit::StepTrackingWakeLock").apply {
                acquire(10 * 60 * 1000L)
            }
        } catch (_: Exception) {}

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
                stepCounterSensor?.let {
                    sensorManager.registerListener(this@StepTrackingService, it, SensorManager.SENSOR_DELAY_UI)
                }
                accelerometerSensor?.let {
                    sensorManager.registerListener(this@StepTrackingService, it, SensorManager.SENSOR_DELAY_UI)
                }
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
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                lastStepCounterEventTime = currentTime
                val totalBootSteps = event.values[0]
                if (initialSensorSteps < 0f) {
                    initialSensorSteps = totalBootSteps - baselineSavedSteps
                }
                val calculated = (totalBootSteps - initialSensorSteps).toInt()
                if (calculated > currentSteps) {
                    currentSteps = calculated.coerceAtLeast(baselineSavedSteps)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Only use accelerometer fallback if step counter sensor is silent (e.g. emulators or unsupported hardware)
                if (currentTime - lastStepCounterEventTime > 4000L) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    
                    val deltaMag = kotlin.math.abs(magnitude - lastMagnitude)
                    lastMagnitude = magnitude

                    if (deltaMag in 3.2f..8.0f) {
                        if (currentTime - lastStepTimestamp >= minStepIntervalMs) {
                            currentSteps += 1
                            lastStepTimestamp = currentTime
                        }
                    }
                }
            }
        }

        if (kotlin.math.abs(currentSteps - lastSavedSteps) >= 1) {
            lastSavedSteps = currentSteps
            serviceScope.launch {
                try {
                    val dao = AppDatabase.getDatabase(applicationContext).stepDao()
                    dao.insertOrUpdateStep(StepEntity(dateStr, currentSteps))
                } catch (_: Exception) {}
            }
            updateNotification("Today's Steps: $currentSteps")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "MyFit Step Counter Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Runs background step tracking for MyFit"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyFit Fitness")
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
            sensorManager.unregisterListener(this)
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
