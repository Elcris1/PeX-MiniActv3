package com.example.miniactv3

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.miniactv3.ui.theme.MiniActv3Theme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private var myService: MyService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MyService.LocalBinder
            myService = binder.service
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            myService = null
        }
    }
    private var isStarted = false
    private val CHANNEL_ID = "miniactv3_notification_channel"

    private val permissions=arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                0
            )
        }

        //Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel (
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_HIGH
            )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        enableEdgeToEdge()
        setContent {
            MiniActv3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val locations by MyService.locationsState.collectAsStateWithLifecycle()
                    Column(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                        ) {

                        Button(onClick = {
                            if(isStarted) {
                                stopMyService()
                            } else {
                                startService()
                            }
                        }) {
                            Text("Start/stop Service")
                        }

                        Text("Ubicaciones recibidas:", style = MaterialTheme.typography.titleSmall)
                        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (locations.isEmpty()) {
                                    item { Text("No hay localizaciones", color = MaterialTheme.colorScheme.outline) }
                                }
                                items(locations.reversed()) { log ->
                                    Text(log, style = MaterialTheme.typography.bodySmall)
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun startService() {
        //I could check if there is some permission here before starting the location updates
        Intent(applicationContext, MyService::class.java).also {
            it.action = MyService.Actions.START.toString()
            startService(it)
            isStarted = true

        }
    }

    private fun stopMyService() {
        Intent(applicationContext, MyService::class.java).also {
            it.action = MyService.Actions.STOP.toString()
            startService(it)
            isStarted = false
        }
    }



    override fun onStart() {
        Log.d("MAINacTIVITY", "OnStart")
        super.onStart()

        Intent(this, MyService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

    }

    override fun onResume() {
        Log.d("MAINacTIVITY", "OnResume")
        super.onResume()

    }

    override fun onPause() {
        Log.d("MainACtivity", "OnPause $isBound")
        if (isBound) {
            Log.d("MainACtivity", "Unbinding service")
            unbindService(connection) // Just disconnects the pipe; Service stays alive!
            isBound = false
        }

        super.onPause()
    }

    override fun onStop() {
        Log.d("MainACtivity", "OnStop $isBound")
        if (isBound) {
            Log.d("MainACtivity", "Unbinding service")
            unbindService(connection) // Just disconnects the pipe; Service stays alive!
            isBound = false
        }
        super.onStop()
    }

}




@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MiniActv3Theme {
        Greeting("Android")
    }
}