package com.example.flowmode.data.model

enum class MechanicType {
    SENSOR, OUTPUT, MODIFIER
}

data class Mechanic(
    val id: String,
    val name: String,
    val description: String,
    val type: MechanicType,
    val compatibleWith: List<MechanicType> = emptyList()
)

data class GeneratedNode(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val mechanics: List<Mechanic> = emptyList(),
    val config: Map<String, Any> = emptyMap()
)
