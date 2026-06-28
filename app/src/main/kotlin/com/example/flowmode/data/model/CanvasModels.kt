package com.example.flowmode.data.model

data class NodeUI(
    val id: String = "",
    val name: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val type: String = "TRIGGER", // "TRIGGER" or "ACTION"
    val dataJson: String = "" // Serialized TriggerNode or ActionNode
)

data class WireUI(
    val fromId: String = "",
    val toId: String = ""
)
