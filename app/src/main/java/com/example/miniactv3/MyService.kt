package com.example.miniactv3

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.jvm.java

class MyService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    //Service state
    private var isForeground = false
    private var isCountingActive = false
    private var isActive = false //Remplazar por isCountingActive

    //notification things
    private val CHANNEL_ID = "miniactv3_notification_channel"
    private val NOTIFICATION_ID = 1
    private val EXTRA_CANCEL_LOCATION_TRACKING = "cancel_location_tracking"

    //Location things
    private lateinit var fusedClient: FusedLocationProviderClient

    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        internal val service: MyService get() = this@MyService
    }

    override fun onBind(intent: Intent): IBinder? {
        if (isForeground) stopForegroundMode()
        isForeground = false
        return binder
    }

    override fun onRebind(intent: Intent?) {
        if (isForeground) stopForegroundMode()
        isForeground = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if(!isForeground) startForegroundMode()
        return true
    }

    override fun onCreate() {
        super.onCreate()

        //LocationServices
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        createLocationCallback()
        createLocationRequest()
        buildSettingsRequest()
    }

    companion object {
        // StateFlow que guarda el valor del contador


        private val _locationsState = MutableStateFlow<List<String>>(emptyList())

        val locationsState: StateFlow<List<String>> = _locationsState.asStateFlow()

        lateinit var mLocationRequest: LocationRequest
        lateinit var mSettingsClient: SettingsClient


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.MOVE_FOREGROUND.toString() -> startForegroundMode()
            Actions.MOVE_BACKGROUND.toString() -> stopForegroundMode()
            Actions.STOP.toString() -> stop()
        }
        val cancelTracking = intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING, false)
        if (cancelTracking == true) stop()

        return START_STICKY
    }

    private fun start(){
        subscribeToLocationUpdates()
        //añadir add location updates
    }

    private fun stop() {
        Log.d("SErvice", "stop")
        unsubscribeToLocationUpdates()
        if(isForeground) stopForegroundMode()
        stopSelf()
    }

    private fun startForegroundMode() {
        Log.d("SERVICE", "going foreground")
        if(isForeground)  return

        startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("TESTING TEXT"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        isForeground = true



    }

    private fun stopForegroundMode() {
        isForeground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }


     fun subscribeToLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        if(isActive) return
        fusedClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.getMainLooper())
        isActive = true
    }

    fun unsubscribeToLocationUpdates() {
        if (!isActive) return
        fusedClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener {
            isActive = false
        } .addOnFailureListener {
            unsubscribeToLocationUpdates()
        }
    }

    private fun buildNotification(text: String): Notification {
        Log.d("SERVICE", "BUILDING NOTIFICATION")
        val title = "Location Updates Service in Android"


        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(text)
            .setBigContentTitle(title)

        val launchActivityIntent = Intent(this, MainActivity::class.java)
        val cancelIntent = Intent(this, MyService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notificationCompatBuilder = NotificationCompat.Builder(
            this, CHANNEL_ID
        )
        Log.d("BUILDING NOT", "DO WE REACH THIS=?")

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_launcher_foreground, "Launch app", activityPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop sending updates", servicePendingIntent)
            .build()

    }
    private fun createText(location: Location?, time: String): String {
        if (location != null) {
            return "Location: lat ${location.latitude} long: ${location.longitude}. Time: $time"
        }
        return "Location not available at $time"
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    //Location things
    private fun createLocationCallback() {
        mLocationCallback = object: LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                Log.d("SERVICE", "LOCATION RECEIVED")
                super.onLocationResult(p0)
                val time = DateFormat.getTimeInstance().format(Date())
                val text = createText(p0.lastLocation, time )

                _locationsState.value += text
                if (isForeground) updateNotification(text)
            }
        }
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(3000)
            .build()
    }

    private fun buildSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }

    enum class Actions {
        START, STOP, MOVE_FOREGROUND, MOVE_BACKGROUND
    }
}