package com.example.flowmode.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flowmode.data.model.TriggerType
import com.example.flowmode.data.repository.FlowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class FlowEngineService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var flowManager: FlowManager

    private val dynamicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            Log.d("FlowEngine", "Engine Detected Signal: $action")
            
            when (action) {
                Intent.ACTION_SCREEN_OFF -> flowManager.handleTrigger(TriggerType.SCREEN_OFF)
                Intent.ACTION_SCREEN_ON -> Log.d("FlowEngine", "Screen Turned On")
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) flowManager.handleTrigger(TriggerType.HEADPHONES_PLUGGED)
                }
                android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> flowManager.handleTrigger(TriggerType.BLUETOOTH_CONNECT)
                android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED -> flowManager.handleTrigger(TriggerType.BLUETOOTH_DISCONNECT)
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                    if (info?.isConnected == true) {
                        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val ssid = wm.connectionInfo.ssid.replace("\"", "")
                        flowManager.handleTrigger(TriggerType.WIFI_CONNECT, mapOf("ssid" to ssid))
                    } else if (info?.isConnected == false) {
                        flowManager.handleTrigger(TriggerType.WIFI_DISCONNECT)
                    }
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "flow_engine_service"
        private const val NOTIFICATION_ID = 99
    }

    override fun onCreate() {
        super.onCreate()
        flowManager = FlowManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Sensor setup
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
        
        // Register Dynamic Broadcasts (Those that CANNOT be in Manifest)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        registerReceiver(dynamicReceiver, filter)
        
        serviceScope.launch {
            FlowRepository.getInstance(this@FlowEngineService).fetchFlows()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "FlowMode Masterpiece", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlowMode Masterpiece")
            .setContentText("Listening for real-time user actions...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta
        if (acceleration > 10) {
            Log.d("FlowEngine", "Shake detected!")
            flowManager.handleTrigger(TriggerType.SHAKE_DEVICE)
            acceleration = 0f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dynamicReceiver)
        sensorManager.unregisterListener(this)
    }
}
