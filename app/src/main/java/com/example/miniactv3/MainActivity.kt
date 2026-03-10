package com.example.miniactv3

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.miniactv3.ui.theme.MiniActv3Theme

class MainActivity : ComponentActivity() {

    private var boundService: ForegroundLocationService? = null
    private var isBound = false
    var foreGroundOnlyLocationServiceBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as ForegroundLocationService.LocalBinder
            boundService = localBinder.service
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniActv3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    /** Llamado desde el botón "Iniciar servicio" */
    private fun startAndBindService() {
        Intent(this, ForegroundLocationService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            //boundService.startLocationUpdates()
        }
    }

    /** Llamado desde el botón "Parar servicio" */
    private fun stopMyService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
            boundService = null
        }
    }



    override fun onStart() {
        super.onStart()
        //bindService(serviceIntent, foreground, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (foreGroundOnlyLocationServiceBound) {
            unbindService(connection)
            foreGroundOnlyLocationServiceBound = false
        }
    }

    fun subscribeToLocationUpdates() {

    }

    fun unsubscribeToLocationUpdates() {

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