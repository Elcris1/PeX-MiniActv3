package com.example.miniactv3

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.jvm.java

class MyService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var count = 0
    private var isForeground = false
    private var isCountingActive = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        // StateFlow que guarda el valor del contador
        private val _countState = MutableStateFlow(0)
        val countState: StateFlow<Int> = _countState.asStateFlow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.MOVE_FOREGROUND.toString() -> startForegroundMode()
            Actions.MOVE_BACKGROUND.toString() -> stopForegroundMode()
            Actions.STOP.toString() -> stop()
        }
        return START_STICKY
    }

    private fun start(){
        isCountingActive = true
        runCounter()
    }

    private fun stop() {
        isCountingActive = false
        stopSelf()
    }

    private fun startForegroundMode() {
        isForeground = true
        startForeground(this, 1, buildNotification(_countState.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    }

    private fun stopForegroundMode() {
        isForeground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun runCounter() {
        serviceScope.launch {
            while (isCountingActive) {
                delay(1000) // Wait 1 second
                _countState.value++
                if (isForeground) updateNotification( _countState.value)
                Log.d("CounterService", "Count: ${_countState.value}")
            }
        }
    }

    private fun buildNotification(currentCount: Int): Notification {
        val notification = NotificationCompat.Builder(this, "running-channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Running service")
            .setContentText("Elapsed time: $currentCount")
            .build()

        return notification
    }

    private fun updateNotification(currentCount: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, buildNotification(currentCount))
    }

    enum class Actions {
        START, STOP, MOVE_FOREGROUND, MOVE_BACKGROUND
    }
}