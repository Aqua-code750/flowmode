package com.example.flowmode.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flowmode.data.model.TriggerType
import com.example.flowmode.data.repository.FlowRepository

class FlowBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("FlowReceiver", "Signal Received: $action")
        
        val flowManager = FlowManager(context)
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val intentService = Intent(context, FlowEngineService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intentService)
                } else {
                    context.startService(intentService)
                }
            }
            "android.provider.Telephony.SMS_RECEIVED" -> {
                val bundle = intent.extras
                val pdus = bundle?.get("pdus") as? Array<*>
                if (pdus != null) {
                    val messages = pdus.map { pdu ->
                        @Suppress("DEPRECATION")
                        android.telephony.SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    val sender = messages[0].displayOriginatingAddress ?: ""
                    val body = messages.joinToString("") { it.displayMessageBody }
                    flowManager.handleTrigger(TriggerType.SMS_RECEIVED, mapOf("sender" to sender, "text" to body))
                }
            }
            android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                if (state == android.telephony.TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
                    flowManager.handleTrigger(TriggerType.INCOMING_CALL, mapOf("number" to incomingNumber))
                }
            }
            // Moving other triggers to the Dynamic Receiver in FlowEngineService for higher reliability
        }
    }
}
