package com.example.flowmode.ui.canvas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flowmode.data.model.*

@Composable
fun NodeConfigSheet(
    node: NodeUI?,
    onUpdateConfig: (Map<String, Any>) -> Unit,
    onSaveFlow: (String) -> Unit,
    onDeleteNode: () -> Unit,
    onDone: () -> Unit
) {
    if (node == null) return
    var flowName by remember { mutableStateOf("New Flow") }

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(text = "Configure ${node.name}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = flowName,
            onValueChange = { flowName = it },
            label = { Text("Flow Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Node Settings", style = MaterialTheme.typography.titleMedium)
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Dynamic Configuration based on node type
        when (val data = node.data) {
            is TriggerNode -> TriggerConfig(data) { newConfig ->
                onUpdateConfig(newConfig)
            }
            is ActionNode -> ActionConfig(data) { newConfig ->
                onUpdateConfig(newConfig)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSaveFlow(flowName) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Save & Enable Flow")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDeleteNode,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Node")
        }

        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
fun TriggerConfig(trigger: TriggerNode, onConfigChange: (Map<String, Any>) -> Unit) {
    when (trigger.type) {
        TriggerType.TIME -> {
            var hour by remember { mutableStateOf((trigger.config["hour"] as? Number)?.toString() ?: "12") }
            var minute by remember { mutableStateOf((trigger.config["minute"] as? Number)?.toString() ?: "00") }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { 
                        hour = it
                        onConfigChange(trigger.config + mapOf("hour" to (it.toIntOrNull() ?: 0)))
                    },
                    label = { Text("Hour (0-23)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = minute,
                    onValueChange = { 
                        minute = it
                        onConfigChange(trigger.config + mapOf("minute" to (it.toIntOrNull() ?: 0)))
                    },
                    label = { Text("Minute (0-59)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
        TriggerType.WIFI_CONNECT -> {
            var ssid by remember { mutableStateOf((trigger.config["ssid"] as? String) ?: "") }
            OutlinedTextField(
                value = ssid,
                onValueChange = { 
                    ssid = it
                    onConfigChange(trigger.config + mapOf("ssid" to it))
                },
                label = { Text("WiFi SSID") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
        TriggerType.BATTERY_LOW -> {
            var threshold by remember { mutableStateOf((trigger.config["threshold"] as? Number)?.toString() ?: "15") }
            OutlinedTextField(
                value = threshold,
                onValueChange = { 
                    threshold = it
                    onConfigChange(trigger.config + mapOf("threshold" to (it.toIntOrNull() ?: 15)))
                },
                label = { Text("Battery Threshold (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )
        }
        TriggerType.WIFI_DISCONNECT, TriggerType.BATTERY_FULL, TriggerType.PHONE_UNLOCK, TriggerType.SCREEN_OFF, TriggerType.HEADPHONES_PLUGGED -> {
            Text("This trigger requires no extra configuration.", style = MaterialTheme.typography.bodyMedium)
        }
        else -> Text("This trigger has no extra configuration.")
    }
}

@Composable
fun ActionConfig(action: ActionNode, onConfigChange: (Map<String, Any>) -> Unit) {
    when (action.type) {
        ActionType.NOTIFICATION -> {
            var title by remember { mutableStateOf((action.config["title"] as? String) ?: "FlowMode") }
            var message by remember { mutableStateOf((action.config["message"] as? String) ?: "Action fired!") }
            
            OutlinedTextField(
                value = title, 
                onValueChange = { 
                    title = it
                    onConfigChange(action.config + mapOf("title" to it, "message" to message))
                }, 
                label = { Text("Title") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = message, 
                onValueChange = { 
                    message = it
                    onConfigChange(action.config + mapOf("title" to title, "message" to it))
                }, 
                label = { Text("Message") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
        ActionType.SET_BRIGHTNESS -> {
            var level by remember { mutableStateOf((action.config["level"] as? Number)?.toFloat() ?: 0.5f) }
            Text("Brightness Level: ${(level * 100).toInt()}%")
            Slider(
                value = level, 
                onValueChange = { 
                    level = it
                    onConfigChange(action.config + mapOf("level" to it))
                }
            )
        }
        ActionType.TOGGLE_WIFI, ActionType.TOGGLE_FLASHLIGHT, ActionType.TOGGLE_DND, ActionType.TOGGLE_BLUETOOTH -> {
            var enable by remember { mutableStateOf((action.config["enable"] as? Boolean) ?: true) }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (enable) "Turn ON" else "Turn OFF")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = enable, 
                    onCheckedChange = { 
                        enable = it
                        onConfigChange(action.config + mapOf("enable" to it))
                    }
                )
            }
        }
        ActionType.SPEAK_TEXT -> {
            var text by remember { mutableStateOf((action.config["text"] as? String) ?: "") }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onConfigChange(action.config + mapOf("text" to it))
                },
                label = { Text("Text to Speak") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
        ActionType.VIBRATE -> {
            var duration by remember { mutableStateOf((action.config["duration"] as? Number)?.toString() ?: "500") }
            OutlinedTextField(
                value = duration,
                onValueChange = {
                    duration = it
                    onConfigChange(action.config + mapOf("duration" to (it.toLongOrNull() ?: 500L)))
                },
                label = { Text("Duration (ms)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )
        }
        ActionType.PLAY_SOUND -> {
            Text("Plays the default notification sound.", style = MaterialTheme.typography.bodyMedium)
        }
        ActionType.OPEN_APP -> {
            var packageName by remember { mutableStateOf((action.config["packageName"] as? String) ?: "") }
            OutlinedTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    onConfigChange(action.config + mapOf("packageName" to it))
                },
                label = { Text("Package Name (e.g. com.spotify.music)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
        ActionType.LOG_EVENT -> {
            var message by remember { mutableStateOf((action.config["message"] as? String) ?: "") }
            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    onConfigChange(action.config + mapOf("message" to it))
                },
                label = { Text("Log Message") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
        ActionType.SEND_SMS -> {
            var phoneNumber by remember { mutableStateOf((action.config["phoneNumber"] as? String) ?: "") }
            var message by remember { mutableStateOf((action.config["message"] as? String) ?: "") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        onConfigChange(action.config + mapOf("phoneNumber" to it, "message" to message))
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                        onConfigChange(action.config + mapOf("phoneNumber" to phoneNumber, "message" to it))
                    },
                    label = { Text("SMS Message") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
        ActionType.WAIT_DELAY -> {
            var seconds by remember { mutableStateOf((action.config["seconds"] as? Number)?.toString() ?: "1") }
            OutlinedTextField(
                value = seconds,
                onValueChange = {
                    seconds = it
                    onConfigChange(action.config + mapOf("seconds" to (it.toLongOrNull() ?: 1L)))
                },
                label = { Text("Delay (seconds)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )
        }
        else -> {
            Text("No configuration available for ${action.type}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
