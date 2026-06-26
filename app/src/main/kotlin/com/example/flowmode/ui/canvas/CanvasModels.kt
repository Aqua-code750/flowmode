package com.example.flowmode.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.example.flowmode.data.model.ActionNode
import com.example.flowmode.data.model.TriggerNode

data class NodeUI(
    val id: String,
    val name: String,
    var position: Offset,
    val type: NodeType,
    val data: Any // Either TriggerNode or ActionNode
)

enum class NodeType {
    TRIGGER, ACTION
}

data class WireUI(
    val fromId: String,
    val toId: String
)
