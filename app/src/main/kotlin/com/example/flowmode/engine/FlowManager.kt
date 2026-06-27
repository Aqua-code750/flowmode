package com.example.flowmode.engine

import android.content.Context
import android.util.Log
import com.example.flowmode.data.model.*
import com.example.flowmode.data.repository.FlowRepository

class FlowManager(private val context: Context) {
    private val repository = FlowRepository.getInstance()

    fun handleTrigger(triggerType: TriggerType, data: Map<String, Any> = emptyMap()) {
        Log.d("FlowManager", "Trigger fired: $triggerType with data $data")
        val enabledFlows = repository.getEnabledFlows()
        
        enabledFlows.filter { it.trigger.type == triggerType }.forEach { flow ->
            if (isTriggerConditionMet(flow.trigger, data)) {
                executeFlow(flow)
            }
        }
    }

    private fun isTriggerConditionMet(trigger: TriggerNode, data: Map<String, Any>): Boolean {
        return when (trigger.type) {
            TriggerType.WIFI_CONNECT -> {
                val targetSsid = trigger.config["ssid"] as? String
                val actualSsid = data["ssid"] as? String
                targetSsid == null || actualSsid?.contains(targetSsid) == true
            }
            TriggerType.BATTERY_LOW -> {
                val threshold = (trigger.config["threshold"] as? Number)?.toInt() ?: 15
                // In a real broadcast, we might get the level. If not, we assume it's low.
                true 
            }
            else -> true // TIME, PHONE_UNLOCK, etc. usually always true if fired
        }
    }

    fun executeFlow(flow: Flow) {
        Log.d("FlowManager", "Executing flow: ${flow.name}")
        flow.actions.forEach { action ->
            executeAction(action)
        }
    }

    fun executeAction(action: ActionNode) {
        Log.d("FlowManager", "Executing action: ${action.type}")
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
