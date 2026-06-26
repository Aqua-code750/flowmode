package com.example.flowmode.ui.marketplace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.ui.tooling.preview.Preview
import com.example.flowmode.data.model.GeneratedNode
import com.example.flowmode.data.model.Mechanic
import com.example.flowmode.data.model.MechanicType
import com.example.flowmode.ui.theme.FlowModeTheme

import com.example.flowmode.data.model.ActionType
import com.example.flowmode.data.model.TriggerType
import com.example.flowmode.ui.theme.ActionColor
import com.example.flowmode.ui.theme.TriggerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(viewModel: MarketplaceViewModel) {
    MarketplaceContent(
        unlockedNodes = viewModel.unlockedNodes,
        currentGeneratedNode = viewModel.currentGeneratedNode.value,
        onGenerateClick = { viewModel.generateNewNode() },
        onGenerateFromPrompt = { viewModel.generateFromPrompt(it) },
        onAcceptNode = { viewModel.acceptNode() },
        onReroll = { viewModel.reroll() },
        onDismissDialog = { viewModel.currentGeneratedNode.value = null }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceContent(
    unlockedNodes: List<GeneratedNode>,
    currentGeneratedNode: GeneratedNode?,
    onGenerateClick: () -> Unit,
    onGenerateFromPrompt: (String) -> Unit,
    onAcceptNode: () -> Unit,
    onReroll: () -> Unit,
    onDismissDialog: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Marketplace & Library", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // n8n Style Prompt Bar
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Search or Create with AI...") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Vibrate when I get a text") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ActionColor,
                unfocusedBorderColor = ActionColor.copy(alpha = 0.3f)
            ),
            trailingIcon = {
                IconButton(onClick = { 
                    if (prompt.isNotEmpty()) onGenerateFromPrompt(prompt) else onGenerateClick()
                }) {
                    Icon(
                        Icons.Default.AutoAwesome, 
                        contentDescription = "Generate",
                        tint = ActionColor
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter
        ScrollableTabRow(
            selectedTabIndex = listOf("All", "Triggers", "Actions", "Collection").indexOf(selectedCategory),
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent,
            indicator = {}
        ) {
            listOf("All", "Triggers", "Actions", "Collection").forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (selectedCategory == "All" || selectedCategory == "Triggers") {
                item { Text("Standard Triggers", style = MaterialTheme.typography.labelLarge, color = TriggerColor) }
                items(TriggerType.entries) { type ->
                    NodeLibraryCard(
                        name = type.name.replace("_", " ").lowercase().capitalize(),
                        isTrigger = true,
                        color = TriggerColor
                    )
                }
            }

            if (selectedCategory == "All" || selectedCategory == "Actions") {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text("Standard Actions", style = MaterialTheme.typography.labelLarge, color = ActionColor) }
                items(ActionType.entries) { type ->
                    NodeLibraryCard(
                        name = type.name.replace("_", " ").lowercase().capitalize(),
                        isTrigger = false,
                        color = ActionColor
                    )
                }
            }

            if (selectedCategory == "Collection") {
                item { Text("Your Generated Nodes", style = MaterialTheme.typography.labelLarge) }
                if (unlockedNodes.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No custom nodes yet. Use AI to create some!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
                items(unlockedNodes) { node ->
                    NodeLibraryCard(
                        name = node.name,
                        isTrigger = false, // Generated nodes usually act as complex actions
                        color = ActionColor,
                        description = node.description
                    )
                }
            }
        }

        currentGeneratedNode?.let { node ->
            AlertDialog(
                onDismissRequest = onDismissDialog,
                confirmButton = {
                    Button(
                        onClick = onAcceptNode,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Collection")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onReroll) {
                        Text("Try Another", color = Color.Gray)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = ActionColor)
                        Spacer(Modifier.width(8.dp))
                        Text("Node Synthesized!")
                    }
                },
                text = {
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ActionColor.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(node.name, style = MaterialTheme.typography.titleMedium, color = ActionColor)
                                Spacer(Modifier.height(4.dp))
                                Text(node.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("This custom node has been built using ${node.mechanics.size} mechanics including:", style = MaterialTheme.typography.labelSmall)
                        node.mechanics.forEach {
                            Text("• ${it.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun NodeLibraryCard(name: String, isTrigger: Boolean, color: Color, description: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.05f)
            ) {
                Icon(
                    imageVector = if (isTrigger) Icons.Default.Bolt else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description ?: (if (isTrigger) "Trigger" else "Action"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            IconButton(onClick = { /* Add logic */ }) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = color.copy(alpha = 0.3f))
            }
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MarketplacePreview() {
    val mockNodes = listOf(
        GeneratedNode("1", "Night Owl", "When it's night, vibrate.", listOf(Mechanic("m1", "Night", "", MechanicType.SENSOR))),
        GeneratedNode("2", "Safe Home", "When home, turn on WiFi.", listOf(Mechanic("m2", "Home", "", MechanicType.SENSOR)))
    )
    FlowModeTheme {
        MarketplaceContent(
            unlockedNodes = mockNodes,
            currentGeneratedNode = null,
            onGenerateClick = {},
            onGenerateFromPrompt = {},
            onAcceptNode = {},
            onReroll = {},
            onDismissDialog = {}
        )
    }
}
