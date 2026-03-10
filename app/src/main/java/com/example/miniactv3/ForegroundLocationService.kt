package com.example.miniactv3

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.navigationevent.NavigationEventDispatcher
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import java.text.DateFormat
import java.util.Date

class ForegroundLocationService : Service() {

    //Binder
    inner class LocalBinder: Binder() {
        internal val service: ForegroundLocationService get() = this@ForegroundLocationService
    }
    private val localBinder = LocalBinder()


    private var isChangingConfiguration = false
    private var isForeground: Boolean = false


    //LOCATION Requirements
    private lateinit var fusedClient: FusedLocationProviderClient

    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mSettingsClient: SettingsClient
    private var arePeriodicalUpdates = false
    private var location: Location? = null

    //notification things
    private val NOTIFICATION_CHANNEL_ID = "miniactv3_notification_channel"
    private val NOTIFICATION_ID = 1
    private val EXTRA_CANCEL_LOCATION_TRACKING = "cancel_location_tracking"

    //LIFECYCLE Methods

    override fun onCreate() {
        super.onCreate()

        //LocationServices
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        createLocationCallback()
        createLocationRequest()
        buildSettingsRequest()

        //Notification
        createNotificationChannel()
    }



    override fun onBind(intent: Intent): IBinder {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!isChangingConfiguration) {
            startForeground(NOTIFICATION_ID, generateNotification())
            isForeground = true
        }
        return super.onUnbind(intent)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val cancelTracking = intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING, false)
        if (cancelTracking == true) {
            stopLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY

    }

    fun startForegroundMode() {
        startForeground(NOTIFICATION_ID, generateNotification())

    }

    fun stopForegroundMode() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }


    //NOTIFICATIONS
    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Notification for foreground service" }

            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun generateNotification(): Notification{
        Log.d("app", "GenerateNotification()")
        val mainNotificationText = location.toString() ?: "No current location"
        val title = "Location in Android"

        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, title, NotificationManager.IMPORTANCE_DEFAULT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(title)

        val launchActivityIntent = Intent(this, MainActivity::class.java)
        val cancelIntent = Intent(this, ForegroundLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notificationCompatBuilder = NotificationCompat.Builder(
            applicationContext, NOTIFICATION_CHANNEL_ID
        )

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(title)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_launcher_foreground, "launch", activityPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "sstop", servicePendingIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, generateNotification())
    }


    //Location things
    private fun createLocationCallback() {
        mLocationCallback = object: LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                location = p0.lastLocation
                //mLastUpdateTime.value = DateFormat.getTimeInstance().format(Date())
            }
        }
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(5000)
            .build()
    }

    private fun buildSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startLocationUpdates() {
        if(arePeriodicalUpdates) return

        fusedClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.getMainLooper())

        arePeriodicalUpdates = true
    }

    fun stopLocationUpdates() {
        if(!arePeriodicalUpdates) return
        fusedClient.removeLocationUpdates(mLocationCallback)
    }




}