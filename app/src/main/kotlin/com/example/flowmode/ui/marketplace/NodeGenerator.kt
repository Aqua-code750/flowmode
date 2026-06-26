package com.example.flowmode.ui.marketplace

import com.example.flowmode.data.model.GeneratedNode
import com.example.flowmode.data.model.Mechanic
import com.example.flowmode.data.model.MechanicType
import java.util.UUID

object NodeGenerator {
    private val sensorPool = listOf(
        Mechanic("m1", "Time Window", "the clock strikes the scheduled time", MechanicType.SENSOR),
        Mechanic("m2", "Motion Detected", "your device is moved", MechanicType.SENSOR),
        Mechanic("m3", "Headphones Connected", "headphones are plugged in", MechanicType.SENSOR),
        Mechanic("m4", "Screen On/Off", "your screen state changes", MechanicType.SENSOR),
        Mechanic("m11", "Battery Low", "battery level drops below 15%", MechanicType.SENSOR),
        Mechanic("m12", "Bluetooth Connected", "a Bluetooth device is connected", MechanicType.SENSOR),
        Mechanic("m13", "Charging", "the device is plugged into power", MechanicType.SENSOR),
        Mechanic("m14", "Quiet Place", "ambient noise levels are low", MechanicType.SENSOR),
        Mechanic("m21", "WiFi Joined", "connected to a specific network", MechanicType.SENSOR),
        Mechanic("m22", "SMS Received", "a message arrives from a contact", MechanicType.SENSOR),
        Mechanic("m23", "Arrive at Work", "location changes to workplace", MechanicType.SENSOR),
        Mechanic("m24", "Shake", "the device is shaken physically", MechanicType.SENSOR),
        Mechanic("m25", "NFC Tag Scanned", "an NFC tag is detected", MechanicType.SENSOR),
        Mechanic("m26", "App Opened", "a specific app is launched", MechanicType.SENSOR)
    )

    private val outputPool = listOf(
        Mechanic("m5", "Vibrate", "vibrate the phone", MechanicType.OUTPUT),
        Mechanic("m6", "Play Chime", "play a system sound", MechanicType.OUTPUT),
        Mechanic("m7", "Log Event", "save a log entry", MechanicType.OUTPUT),
        Mechanic("m8", "Open App", "open a pre-configured app", MechanicType.OUTPUT),
        Mechanic("m15", "Flashlight", "toggle the flashlight", MechanicType.OUTPUT),
        Mechanic("m16", "Speak Text", "read a message out loud", MechanicType.OUTPUT),
        Mechanic("m17", "Silent Mode", "enable Do Not Disturb", MechanicType.OUTPUT),
        Mechanic("m18", "Brightness", "adjust the screen brightness", MechanicType.OUTPUT),
        Mechanic("m27", "HTTP Request", "ping a web service URL", MechanicType.OUTPUT),
        Mechanic("m28", "Send SMS", "dispatch a text message", MechanicType.OUTPUT),
        Mechanic("m29", "Toggle WiFi", "switch WiFi on or off", MechanicType.OUTPUT),
        Mechanic("m30", "Screenshot", "capture the current screen", MechanicType.OUTPUT),
        Mechanic("m31", "Stop Music", "pause media playback", MechanicType.OUTPUT),
        Mechanic("m32", "Show Toast", "display a popup message", MechanicType.OUTPUT)
    )

    private val usefulPairs = listOf(
        Pair("m1", "m17"), // Time -> Silent Mode
        Pair("m11", "m18"), // Battery Low -> Low Brightness
        Pair("m3", "m31"), // Headphones -> Stop Music
        Pair("m22", "m16"), // SMS -> Speak Text
        Pair("m23", "m29"), // Arrive Work -> Toggle WiFi
        Pair("m24", "m15"), // Shake -> Flashlight
        Pair("m13", "m6"), // Charging -> Play Chime
        Pair("m26", "m17")  // Open App -> Silent Mode
    )

    fun generateRandomNode(): GeneratedNode {
        val pair = usefulPairs.random()
        val sensor = sensorPool.find { it.id == pair.first } ?: sensorPool.random()
        val output = outputPool.find { it.id == pair.second } ?: outputPool.random()
        
        val mechanics = mutableListOf(sensor, output)
        
        val name = when(pair.first) {
            "m24" -> "Flashlight Shake"
            "m26" -> "Gaming Mode"
            "m23" -> "Office Mode"
            "m11" -> "Power Saver"
            else -> "${sensor.name} ${output.name}"
        }

        return GeneratedNode(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "When ${sensor.description.lowercase()}, then ${output.description.lowercase()}.",
            mechanics = mechanics
        )
    }

    fun generateFromDescription(userInput: String): GeneratedNode? {
        val input = userInput.lowercase()
        
        val sensor = sensorPool.maxByOrNull { m ->
            val keywords = m.name.lowercase().split(" ") + m.description.lowercase().split(" ")
            keywords.count { input.contains(it) }
        } ?: sensorPool.random()
            
        val output = outputPool.maxByOrNull { m ->
            val keywords = m.name.lowercase().split(" ") + m.description.lowercase().split(" ")
            keywords.count { input.contains(it) }
        } ?: outputPool.random()

        val mechanics = mutableListOf(sensor, output)

        val name = if (userInput.length in 3..25) userInput.capitalize() else "${sensor.name} ${output.name}"
        val desc = "Automated: ${sensor.name} trigger linked to ${output.name} action."

        return GeneratedNode(
            id = UUID.randomUUID().toString(),
            name = name,
            description = desc,
            mechanics = mechanics
        )
    }

    private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
