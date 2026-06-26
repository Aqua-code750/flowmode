package com.example.flowmode.ui.canvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.flowmode.data.model.*
import com.example.flowmode.data.repository.FlowRepository
import java.util.UUID

class CanvasViewModel : ViewModel() {
    val nodes = mutableStateListOf<NodeUI>()
    val wires = mutableStateListOf<WireUI>()
    
    val selectedNode = mutableStateOf<NodeUI?>(null)
    val connectionSource = mutableStateOf<String?>(null)

    fun addTrigger(type: TriggerType) {
        val id = UUID.randomUUID().toString()
        val name = type.name.replace("_", " ").lowercase().capitalize()
        nodes.add(NodeUI(id, name, Offset(100f, 100f), NodeType.TRIGGER, TriggerNode(type)))
    }

    fun addAction(type: ActionType) {
        val id = UUID.randomUUID().toString()
        val name = type.name.replace("_", " ").lowercase().capitalize()
        nodes.add(NodeUI(id, name, Offset(400f, 100f), NodeType.ACTION, ActionNode(type)))
    }

    private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    fun addGeneratedNode(generatedNode: GeneratedNode) {
        val id = UUID.randomUUID().toString()
        nodes.add(NodeUI(id, generatedNode.name, Offset(250f, 250f), NodeType.ACTION, ActionNode(ActionType.NOTIFICATION, mapOf("generated" to generatedNode))))
    }

    fun updateNodePosition(id: String, newPosition: Offset) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index != -1) {
            nodes[index] = nodes[index].copy(position = newPosition)
        }
    }

    fun updateSelectedNodeConfig(newConfig: Map<String, Any>) {
        val node = selectedNode.value ?: return
        val index = nodes.indexOfFirst { it.id == node.id }
        if (index != -1) {
            val updatedData = when (val currentData = node.data) {
                is TriggerNode -> currentData.copy(config = newConfig)
                is ActionNode -> currentData.copy(config = newConfig)
                else -> currentData
            }
            val updatedNode = nodes[index].copy(data = updatedData)
            nodes[index] = updatedNode
            selectedNode.value = updatedNode
        }
    }

    fun connectNodes(fromId: String, toId: String) {
        if (fromId == toId) return
        val fromNode = nodes.find { it.id == fromId }
        val toNode = nodes.find { it.id == toId }
        
        if (fromNode?.type == NodeType.ACTION && toNode?.type == NodeType.TRIGGER) return

        if (!wires.any { it.fromId == fromId && it.toId == toId }) {
            wires.add(WireUI(fromId, toId))
        }
    }

    fun onNodeClicked(node: NodeUI) {
        val source = connectionSource.value
        if (source != null) {
            if (source != node.id) {
                connectNodes(source, node.id)
            }
            connectionSource.value = null
        } else {
            selectedNode.value = node
        }
    }

    fun startConnection(nodeId: String) {
        connectionSource.value = nodeId
    }

    fun deleteSelectedNode() {
        val node = selectedNode.value ?: return
        nodes.removeIf { it.id == node.id }
        wires.removeIf { it.fromId == node.id || it.toId == node.id }
        selectedNode.value = null
    }

    fun saveFlow(name: String) {
        val triggerNodeUI = nodes.find { it.type == NodeType.TRIGGER } ?: return
        
        val actions = mutableListOf<ActionNode>()
        val visited = mutableSetOf<String>()
        val queue = mutableListOf(triggerNodeUI.id)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            if (currentId in visited) continue
            visited.add(currentId)
            
            val nextIds = wires.filter { it.fromId == currentId }.map { it.toId }
            nextIds.forEach { nextId ->
                val nextNode = nodes.find { it.id == nextId }
                if (nextNode != null && nextNode.type == NodeType.ACTION) {
                    if (nextNode.id !in visited) {
                        actions.add(nextNode.data as ActionNode)
                        queue.add(nextId)
                    }
                }
            }
        }

        val flow = Flow(
            name = name,
            trigger = triggerNodeUI.data as TriggerNode,
            actions = actions,
            enabled = true
        )
        FlowRepository.getInstance().addFlow(flow)
    }
}
