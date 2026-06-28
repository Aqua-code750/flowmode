package com.example.flowmode.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.flowmode.data.model.TriggerType
import com.example.flowmode.data.repository.FlowRepository

class FlowBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("FlowReceiver", "System Event: $action")
        
        val flowManager = FlowManager(context)
        
        // Match system actions to FlowMode triggers
        val triggerType = when (action) {
            Intent.ACTION_USER_PRESENT -> TriggerType.PHONE_UNLOCK
            Intent.ACTION_SCREEN_OFF -> TriggerType.SCREEN_OFF
            Intent.ACTION_POWER_CONNECTED -> TriggerType.POWER_CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> TriggerType.POWER_DISCONNECTED
            Intent.ACTION_BATTERY_LOW -> TriggerType.BATTERY_LOW
            Intent.ACTION_BATTERY_OKAY -> TriggerType.BATTERY_FULL
            "android.provider.Telephony.SMS_RECEIVED" -> TriggerType.SMS_RECEIVED
            "android.intent.action.PHONE_STATE" -> TriggerType.INCOMING_CALL
            else -> null
        }

        if (triggerType != null) {
            val data = mutableMapOf<String, Any>()
            
            // Extract extra data for specific triggers
            if (action == "android.provider.Telephony.SMS_RECEIVED") {
                val bundle = intent.extras
                val pdus = bundle?.get("pdus") as? Array<*>
                pdus?.let {
                    val messages = it.map { pdu -> 
                        @Suppress("DEPRECATION")
                        android.telephony.SmsMessage.createFromPdu(pdu as ByteArray) 
                    }
                    data["sender"] = messages[0].displayOriginatingAddress ?: ""
                    data["text"] = messages.joinToString("") { m -> it.toString() }
                }
            } else if (action == "android.intent.action.PHONE_STATE") {
                val state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                if (state == android.telephony.TelephonyManager.EXTRA_STATE_RINGING) {
                    data["number"] = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
                } else {
                    return // Ignore non-ringing states
                }
            }
            
            // Fire automation instantly
            flowManager.handleTrigger(triggerType, data)
        }
        
        // Ensure engine service is alive for sensor-based triggers (Shake, etc)
        val serviceIntent = Intent(context, FlowEngineService::class.java)
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            // Foreground start might fail on newer Android if app is in background, 
            // but the BroadcastReceiver handles the critical ones anyway.
        }
    }
}
