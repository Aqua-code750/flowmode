package com.example.flowmode.data.model

data class Flow(
    val id: String = "",
    val name: String = "",
    val enabled: Boolean = true,
    val trigger: TriggerNode = TriggerNode(),
    val actions: List<ActionNode> = emptyList(),
    val canvasData: String = "" // Added to store nodes/wires JSON
)

data class TriggerNode(
    val type: TriggerType = TriggerType.TIME,
    val config: Map<String, Any> = emptyMap()
)

data class ActionNode(
    val type: ActionType = ActionType.NOTIFICATION,
    val config: Map<String, Any> = emptyMap()
)

enum class TriggerType {
    TIME,
    WIFI_CONNECT,
    WIFI_DISCONNECT,
    BATTERY_LOW,
    BATTERY_FULL,
    PHONE_UNLOCK,
    SCREEN_OFF,
    BLUETOOTH_CONNECT,
    BLUETOOTH_DISCONNECT,
    HEADPHONES_PLUGGED,
    LOCATION_ENTER,
    LOCATION_EXIT,
    SMS_RECEIVED,
    INCOMING_CALL,
    NFC_TAG,
    SHAKE_DEVICE,
    POWER_CONNECTED,
    POWER_DISCONNECTED
}

enum class ActionType {
    NOTIFICATION,
    TOGGLE_DND,
    TOGGLE_WIFI,
    SET_BRIGHTNESS,
    TOGGLE_FLASHLIGHT,
    PLAY_SOUND,
    VIBRATE,
    SPEAK_TEXT,
    SEND_SMS,
    OPEN_APP,
    TOGGLE_BLUETOOTH,
    LOG_EVENT,
    WAIT_DELAY,
    HTTP_REQUEST,
    SCREENSHOT
}
