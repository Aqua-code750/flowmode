package com.example.flowmode.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flowmode.data.model.*
import com.example.flowmode.data.repository.FlowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlowManager(private val context: Context) {
    private val repository = FlowRepository.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val RUNNING_CHANNEL_ID = "flow_running_notifications"
    }

    fun handleTrigger(triggerType: TriggerType, data: Map<String, Any> = emptyMap()) {
        val enabledFlows = repository.getEnabledFlows()
        Log.d("FlowManager", "Trigger detected: $triggerType. Checking ${enabledFlows.size} active flows.")

        enabledFlows.filter { it.trigger.type == triggerType }.forEach { flow ->
            if (isTriggerConditionMet(flow.trigger, data)) {
                Log.d("FlowManager", "Match found! Executing Flow: ${flow.name}")
                executeFlow(flow)
            }
        }
    }

    private fun isTriggerConditionMet(trigger: TriggerNode, data: Map<String, Any>): Boolean {
        return when (trigger.type) {
            TriggerType.WIFI_CONNECT -> {
                val targetSsid = trigger.config["ssid"] as? String
                val actualSsid = data["ssid"] as? String
                targetSsid == null || targetSsid.isBlank() || actualSsid?.contains(targetSsid, ignoreCase = true) == true
            }
            TriggerType.BATTERY_LOW -> {
                val threshold = (trigger.config["threshold"] as? Number)?.toInt() ?: 15
                val currentLevel = data["level"] as? Int ?: 100
                currentLevel <= threshold
            }
            TriggerType.BATTERY_FULL -> {
                val currentLevel = data["level"] as? Int ?: 0
                currentLevel >= 95
            }
            TriggerType.SMS_RECEIVED -> {
                val targetSender = trigger.config["sender"] as? String
                val targetText = trigger.config["text"] as? String
                val actualSender = data["sender"] as? String
                val actualText = data["text"] as? String

                (targetSender == null || targetSender.isBlank() || actualSender?.contains(targetSender, ignoreCase = true) == true) &&
                        (targetText == null || targetText.isBlank() || actualText?.contains(targetText, ignoreCase = true) == true)
            }
            TriggerType.INCOMING_CALL -> {
                val targetNumber = trigger.config["number"] as? String
                val actualNumber = data["number"] as? String
                targetNumber == null || targetNumber.isBlank() || actualNumber?.contains(targetNumber) == true
            }
            TriggerType.POWER_CONNECTED, TriggerType.POWER_DISCONNECTED -> true
            TriggerType.PHONE_UNLOCK, TriggerType.SCREEN_OFF, TriggerType.SHAKE_DEVICE, TriggerType.NFC_TAG -> true
            else -> true
        }
    }

    fun executeFlow(flow: Flow) {
        showRunningNotification(flow.name)
        scope.launch {
            flow.actions.forEach { action ->
                executeAction(action)
            }
        }
    }

    private fun showRunningNotification(flowName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(RUNNING_CHANNEL_ID, "Automation Running", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FlowMode")
            .setContentText("Your automation '$flowName' is running...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .build()

        notificationManager.notify(flowName.hashCode(), notification)
    }

    fun executeAction(action: ActionNode) {
        Log.d("FlowManager", "Running Action: ${action.type}")
        when (action.type) {
            ActionType.NOTIFICATION -> FlowActions.showNotification(context, action.config)
            ActionType.TOGGLE_DND -> FlowActions.toggleDnd(context, action.config)
            ActionType.TOGGLE_WIFI -> FlowActions.toggleWifi(context, action.config)
            ActionType.SET_BRIGHTNESS -> FlowActions.setBrightness(context, action.config)
            ActionType.TOGGLE_FLASHLIGHT -> FlowActions.toggleFlashlight(context, action.config)
            ActionType.OPEN_APP -> FlowActions.openApp(context, action.config)
            ActionType.LOG_EVENT -> FlowActions.logEvent(action.config)
            ActionType.PLAY_SOUND -> FlowActions.playSound(context)
            ActionType.VIBRATE -> FlowActions.vibrate(context, action.config)
            ActionType.SPEAK_TEXT -> FlowActions.speakText(context, action.config)
            ActionType.TOGGLE_BLUETOOTH -> FlowActions.toggleBluetooth(context, action.config)
            ActionType.SEND_SMS -> FlowActions.sendSms(action.config)
            ActionType.WAIT_DELAY -> FlowActions.waitDelay(action.config)
            ActionType.HTTP_REQUEST -> FlowActions.makeHttpRequest(action.config)
            ActionType.SCREENSHOT -> FlowActions.takeScreenshot(context)
            else -> Log.w("FlowManager", "Action type ${action.type} not yet implemented")
        }
    }
}
