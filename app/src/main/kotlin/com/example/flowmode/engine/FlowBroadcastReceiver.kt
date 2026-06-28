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
        val flowManager = FlowManager(context)
        val scheduler = FlowScheduler(context)
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("FlowReceiver", "Boot completed - Rescheduling alarms")
                val intentService = Intent(context, FlowEngineService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intentService)
                } else {
                    context.startService(intentService)
                }
                
                val repository = FlowRepository.getInstance(context)
                repository.getEnabledFlows()
                    .filter { it.trigger.type == TriggerType.TIME }
                    .forEach { scheduler.scheduleTimeTrigger(it) }
            }
            "com.example.flowmode.ACTION_TIME_TRIGGER" -> {
                val flowId = intent.getStringExtra("flowId")
                flowManager.handleTrigger(TriggerType.TIME, mapOf("flowId" to (flowId ?: "")))
                val flow = FlowRepository.getInstance(context).getEnabledFlows().find { it.id == flowId }
                flow?.let { scheduler.scheduleTimeTrigger(it) }
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
                    flowManager.handleTrigger(TriggerType.INCOMING_CALL, mapOf("number" to (incomingNumber ?: "")))
                }
            }
        }
    }
}
