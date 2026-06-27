package com.example.flowmode.ui.canvas

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.flowmode.ui.theme.FlowTheme
import com.example.flowmode.data.model.*
import kotlin.math.roundToInt

import androidx.compose.ui.tooling.preview.Preview
import com.example.flowmode.data.model.*
import com.example.flowmode.ui.theme.FlowModeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowCanvas(viewModel: CanvasViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val flowManager = remember { com.example.flowmode.engine.FlowManager(context) }
    
    FlowCanvasContent(
        nodes = viewModel.nodes,
        wires = viewModel.wires,
        connectionSource = viewModel.connectionSource.value,
        selectedNode = viewModel.selectedNode.value,
        onPositionChange = { id, pos -> viewModel.updateNodePosition(id, pos) },
        onConnectClick = { id -> viewModel.startConnection(id) },
        onConfigClick = { node -> viewModel.selectedNode.value = node },
        onNodeClick = { node -> viewModel.onNodeClicked(node) },
        onUpdateConfig = { config -> viewModel.updateSelectedNodeConfig(config) },
        onSaveFlow = { name -> viewModel.saveFlow(name) },
        onDeleteNode = { viewModel.deleteSelectedNode() },
        onRunTest = {
            viewModel.selectedNode.value?.let { node ->
                if (node.type == NodeType.ACTION) {
                    flowManager.executeAction(node.data as ActionNode)
                }
            }
        },
        onAddTrigger = { viewModel.addTrigger(it) },
        onAddAction = { viewModel.addAction(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowCanvasContent(
    nodes: List<NodeUI>,
    wires: List<WireUI>,
    connectionSource: String?,
    selectedNode: NodeUI?,
    onPositionChange: (String, Offset) -> Unit,
    onConnectClick: (String) -> Unit,
    onConfigClick: (NodeUI) -> Unit,
    onNodeClick: (NodeUI) -> Unit,
    onUpdateConfig: (Map<String, Any>) -> Unit,
    onSaveFlow: (String) -> Unit,
    onDeleteNode: () -> Unit,
    onRunTest: () -> Unit,
    onAddTrigger: (TriggerType) -> Unit,
    onAddAction: (ActionType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var showNodeLibrary by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val nodeWidthPx = with(density) { 180.dp.toPx() }
    val nodeHeightPx = with(density) { 80.dp.toPx() } // Adjusted for n8n style
    val canvasBg = FlowTheme.colors.canvasBackground
    val wireColor = FlowTheme.colors.wire

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBg)
    ) {
        // Draw Grid Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 32.dp.toPx()
            val dotSize = 1.dp.toPx()
            val gridColor = wireColor.copy(alpha = 0.05f)
            
            for (x in 0..(size.width / gridSize).toInt()) {
                for (y in 0..(size.height / gridSize).toInt()) {
                    drawCircle(
                        color = gridColor,
                        radius = dotSize,
                        center = Offset(x * gridSize, y * gridSize)
                    )
                }
            }
        }

        // Draw Wires
        Canvas(modifier = Modifier.fillMaxSize()) {
            wires.forEach { wire ->
                val fromNode = nodes.find { it.id == wire.fromId }
                val toNode = nodes.find { it.id == wire.toId }

                if (fromNode != null && toNode != null) {
                    val start = fromNode.position + Offset(200.dp.toPx(), 60.dp.toPx()) // Updated for new node size
                    val end = toNode.position + Offset(0f, 60.dp.toPx())

                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        val deltaX = Math.abs(end.x - start.x)
                        val controlX = deltaX / 2f
                        cubicTo(
                            start.x + controlX, start.y,
                            end.x - controlX, end.y,
                            end.x, end.y
                        )
                    }
                    drawPath(
                        path = path, 
                        color = wireColor, 
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Draw Nodes
        nodes.forEach { node ->
            NodeComposable(
                node = node,
                isConnecting = connectionSource == node.id,
                onPositionChange = { onPositionChange(node.id, it) },
                onConnectClick = { onConnectClick(node.id) },
                onConfigClick = { 
                    onConfigClick(node)
                    showSheet = true
                },
                onNodeClick = { onNodeClick(node) }
            )
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showNodeLibrary = true },
                containerColor = FlowTheme.colors.action,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Node")
            }

            if (selectedNode?.type == NodeType.ACTION) {
                ExtendedFloatingActionButton(
                    onClick = onRunTest,
                    icon = { Icon(Icons.Default.PlayArrow, "Run Test") },
                    text = { Text("Run Test") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }

        // Node Library Dialog
        if (showNodeLibrary) {
            AlertDialog(
                onDismissRequest = { showNodeLibrary = false },
                title = { Text("Node Library") },
                text = {
                    var search by remember { mutableStateOf("") }
                    Column(modifier = Modifier.height(400.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Search nodes...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Text("Triggers", style = MaterialTheme.typography.labelLarge, color = FlowTheme.colors.trigger) }
                            items(TriggerType.entries.filter { it.name.contains(search, ignoreCase = true) }) { type ->
                                LibraryItem(type.name, FlowTheme.colors.trigger) {
                                    onAddTrigger(type)
                                    showNodeLibrary = false
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                            item { Text("Actions", style = MaterialTheme.typography.labelLarge, color = FlowTheme.colors.action) }
                            items(ActionType.entries.filter { it.name.contains(search, ignoreCase = true) }) { type ->
                                LibraryItem(type.name, FlowTheme.colors.action) {
                                    onAddAction(type)
                                    showNodeLibrary = false
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNodeLibrary = false }) { Text("Close") }
                }
            )
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                NodeConfigSheet(
                    node = selectedNode,
                    onUpdateConfig = onUpdateConfig,
                    onSaveFlow = { name ->
                        onSaveFlow(name)
                        showSheet = false
                    },
                    onDeleteNode = {
                        onDeleteNode()
                        showSheet = false
                    },
                    onDone = { showSheet = false }
                )
            }
        }
    }
}

@Composable
fun NodeComposable(
    node: NodeUI,
    isConnecting: Boolean,
    onPositionChange: (Offset) -> Unit,
    onConnectClick: () -> Unit,
    onConfigClick: () -> Unit,
    onNodeClick: () -> Unit
) {
    val colors = FlowTheme.colors
    val accentColor = if (node.type == NodeType.TRIGGER) colors.trigger else colors.action
    val isSelected = false // Could be passed in
    val borderColor = if (isConnecting) accentColor else if (isSelected) accentColor else colors.nodeBorder

    val animatedOffset by animateOffsetAsState(
        targetValue = node.position,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "NodeOffset"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt()) }
            .zIndex(if (isConnecting) 2f else 1f)
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .wrapContentHeight()
                .pointerInput(node.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPositionChange(node.position + dragAmount)
                    }
                }
                .clickable { onNodeClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isConnecting) 12.dp else 4.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(if (isConnecting) 2.dp else 1.dp, borderColor)
        ) {
            Column {
                // Node Header (Professional Darker Look)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.nodeHeader)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(node.name) {
                                "Notification" -> Icons.Default.Notifications
                                "WiFi", "WiFi Connect", "WiFi Disconnect" -> Icons.Default.Wifi
                                "DND" -> Icons.Default.DoNotDisturb
                                "Brightness" -> Icons.Default.Brightness6
                                "Flashlight" -> Icons.Default.FlashlightOn
                                "Battery Low", "Battery Full" -> Icons.Default.BatteryChargingFull
                                "Phone Unlock" -> Icons.Default.LockOpen
                                "Screen Off" -> Icons.Default.ScreenLockPortrait
                                "Headphones Plugged" -> Icons.Default.Headset
                                "Open App" -> Icons.Default.Launch
                                "Log Event" -> Icons.Default.List
                                "Send SMS" -> Icons.Default.Sms
                                "Wait Delay" -> Icons.Default.Timer
                                "Speak Text" -> Icons.Default.RecordVoiceOver
                                "Play Sound" -> Icons.Default.VolumeUp
                                "Vibrate" -> Icons.Default.Vibration
                                "Bluetooth" -> Icons.Default.Bluetooth
                                else -> if (node.type == NodeType.TRIGGER) Icons.Default.Bolt else Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = node.name.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onConfigClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }

                Divider(color = colors.nodeBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Node Body (Status/Brief Info)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (node.type == NodeType.TRIGGER) "EVENT TRIGGER" else "ACTION BLOCK",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = accentColor.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }

        // Output Port
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 6.dp)
                .background(Color.White, CircleShape)
                .border(BorderStroke(2.dp, accentColor), CircleShape)
                .clickable { onConnectClick() }
                .zIndex(3f)
        )

        // Input Port
        if (node.type == NodeType.ACTION) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-6).dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(2.dp, accentColor), CircleShape)
                    .zIndex(3f)
            )
        }
    }
}

@Composable
fun LibraryItem(name: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(name.replace("_", " ").lowercase().capitalize(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Preview(showBackground = true)
@Composable
fun FlowCanvasPreview() {
    val nodes = listOf(
        NodeUI("1", "Phone Unlock", Offset(100f, 100f), NodeType.TRIGGER, TriggerNode(TriggerType.PHONE_UNLOCK)),
        NodeUI("2", "Show Notification", Offset(600f, 250f), NodeType.ACTION, ActionNode(ActionType.NOTIFICATION))
    )
    val wires = listOf(WireUI("1", "2"))
    
    FlowModeTheme {
        FlowCanvasContent(
            nodes = nodes,
            wires = wires,
            connectionSource = null,
            selectedNode = null,
            onPositionChange = { _, _ -> },
            onConnectClick = { _ -> },
            onConfigClick = { _ -> },
            onNodeClick = { _ -> },
            onUpdateConfig = {},
            onSaveFlow = { _ -> },
            onDeleteNode = {},
            onRunTest = {},
            onAddTrigger = {},
            onAddAction = {}
        )
    }
}
