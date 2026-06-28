package com.example.flowmode.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.flowmode.data.model.TriggerType
import com.example.flowmode.data.repository.FlowRepository

class FlowBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val flowManager = FlowManager(context)
        val scheduler = FlowScheduler(context)
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("FlowReceiver", "Boot completed - Rescheduling alarms")
                val repository = FlowRepository.getInstance(context)
                repository.getEnabledFlows()
                    .filter { it.trigger.type == TriggerType.TIME }
                    .forEach { scheduler.scheduleTimeTrigger(it) }
            }
            "com.example.flowmode.ACTION_TIME_TRIGGER" -> {
                val flowId = intent.getStringExtra("flowId")
                Log.d("FlowReceiver", "Time trigger for flow: $flowId")
                flowManager.handleTrigger(TriggerType.TIME, mapOf("flowId" to (flowId ?: "")))
                // Reschedule for next day if it's a recurring alarm
                val flow = FlowRepository.getInstance(context).getEnabledFlows().find { it.id == flowId }
                flow?.let { scheduler.scheduleTimeTrigger(it) }
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("FlowReceiver", "Phone unlocked")
                flowManager.handleTrigger(TriggerType.PHONE_UNLOCK)
            }
            Intent.ACTION_BATTERY_LOW -> {
                Log.d("FlowReceiver", "Battery low")
                val level = getBatteryLevel(context)
                flowManager.handleTrigger(TriggerType.BATTERY_LOW, mapOf("level" to level))
            }
            Intent.ACTION_BATTERY_OKAY -> {
                Log.d("FlowReceiver", "Battery full/okay")
                val level = getBatteryLevel(context)
                flowManager.handleTrigger(TriggerType.BATTERY_FULL, mapOf("level" to level))
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("FlowReceiver", "Screen off")
                flowManager.handleTrigger(TriggerType.SCREEN_OFF)
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) {
                    Log.d("FlowReceiver", "Headphones plugged")
                    flowManager.handleTrigger(TriggerType.HEADPHONES_PLUGGED)
                }
            }
            "android.provider.Telephony.SMS_RECEIVED" -> {
                Log.d("FlowReceiver", "SMS Received")
                val bundle = intent.extras
                val pdus = bundle?.get("pdus") as? Array<*>
                if (pdus != null) {
                    val messages = pdus.map { 
                        @Suppress("DEPRECATION")
                        android.telephony.SmsMessage.createFromPdu(it as ByteArray) 
                    }
                    val sender = messages[0].displayOriginatingAddress
                    val body = messages.joinToString("") { it.displayMessageBody }
                    flowManager.handleTrigger(TriggerType.SMS_RECEIVED, mapOf("sender" to sender, "text" to body))
                }
            }
            android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                if (state == android.telephony.TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER)
                    Log.d("FlowReceiver", "Incoming call from: $incomingNumber")
                    flowManager.handleTrigger(TriggerType.INCOMING_CALL, mapOf("number" to (incomingNumber ?: "")))
                }
            }
            android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d("FlowReceiver", "Bluetooth connected")
                flowManager.handleTrigger(TriggerType.BLUETOOTH_CONNECT)
            }
            android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.d("FlowReceiver", "Bluetooth disconnected")
                flowManager.handleTrigger(TriggerType.BLUETOOTH_DISCONNECT)
            }
            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                if (info?.isConnected == true) {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
                    Log.d("FlowReceiver", "WiFi connected: $ssid")
                    flowManager.handleTrigger(TriggerType.WIFI_CONNECT, mapOf("ssid" to ssid))
                } else if (info?.isConnected == false) {
                    Log.d("FlowReceiver", "WiFi disconnected")
                    flowManager.handleTrigger(TriggerType.WIFI_DISCONNECT)
                }
            }
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level * 100 / scale.toFloat()).toInt()
    }
}
