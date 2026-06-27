package com.example.flowmode.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                val repository = FlowRepository.getInstance()
                repository.getEnabledFlows()
                    .filter { it.trigger.type == TriggerType.TIME }
                    .forEach { scheduler.scheduleTimeTrigger(it) }
            }
            "com.example.flowmode.ACTION_TIME_TRIGGER" -> {
                val flowId = intent.getStringExtra("flowId")
                Log.d("FlowReceiver", "Time trigger for flow: $flowId")
                flowManager.handleTrigger(TriggerType.TIME, mapOf("flowId" to (flowId ?: "")))
                // Reschedule for next day if it's a recurring alarm
                val flow = FlowRepository.getInstance().getEnabledFlows().find { it.id == flowId }
                flow?.let { scheduler.scheduleTimeTrigger(it) }
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("FlowReceiver", "Phone unlocked")
                flowManager.handleTrigger(TriggerType.PHONE_UNLOCK)
            }
            Intent.ACTION_BATTERY_LOW -> {
                Log.d("FlowReceiver", "Battery low")
                flowManager.handleTrigger(TriggerType.BATTERY_LOW)
            }
            Intent.ACTION_BATTERY_OKAY -> {
                Log.d("FlowReceiver", "Battery full/okay")
                flowManager.handleTrigger(TriggerType.BATTERY_FULL)
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
                flowManager.handleTrigger(TriggerType.SMS_RECEIVED)
            }
            android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                if (state == android.telephony.TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER)
                    Log.d("FlowReceiver", "Incoming call from: $incomingNumber")
                    flowManager.handleTrigger(TriggerType.INCOMING_CALL, mapOf("number" to (incomingNumber ?: "")))
                }
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
}
