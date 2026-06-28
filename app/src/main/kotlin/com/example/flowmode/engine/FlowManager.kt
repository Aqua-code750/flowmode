package com.example.flowmode.engine

import android.content.Context
import android.util.Log
import com.example.flowmode.data.model.*
import com.example.flowmode.data.repository.FlowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlowManager(private val context: Context) {
    private val repository = FlowRepository.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    fun handleTrigger(triggerType: TriggerType, data: Map<String, Any> = emptyMap()) {
        // Use a background scope to ensure zero delay in the BroadcastReceiver
        scope.launch {
            val enabledFlows = repository.getEnabledFlows()
            Log.d("FlowManager", "Trigger: $triggerType. Enabled flows: ${enabledFlows.size}")

            enabledFlows.filter { it.trigger.type == triggerType }.forEach { flow ->
                if (isTriggerConditionMet(flow.trigger, data)) {
                    Log.d("FlowManager", "Match found! Running flow: ${flow.name}")
                    executeFlow(flow)
                }
            }
        }
    }

    private fun isTriggerConditionMet(trigger: TriggerNode, data: Map<String, Any>): Boolean {
        return when (trigger.type) {
            TriggerType.SMS_RECEIVED -> {
                val targetSender = trigger.config["sender"] as? String
                val actualSender = data["sender"] as? String
                targetSender == null || targetSender.isBlank() || actualSender?.contains(targetSender, ignoreCase = true) == true
            }
            TriggerType.INCOMING_CALL -> {
                val targetNumber = trigger.config["number"] as? String
                val actualNumber = data["number"] as? String
                targetNumber == null || targetNumber.isBlank() || actualNumber?.contains(targetNumber) == true
            }
            else -> true // Most triggers are simple events (Unlock, Power, etc)
        }
    }

    fun executeFlow(flow: Flow) {
        flow.actions.forEach { action ->
            executeAction(action)
        }
    }

    fun executeAction(action: ActionNode) {
        Log.d("FlowManager", "Action: ${action.type}")
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
            else -> {}
        }
    }
}
