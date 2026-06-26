package com.example.flowmode.ui.canvas

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.flowmode.ui.theme.FlowTheme
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
        // Draw Wires
        Canvas(modifier = Modifier.fillMaxSize()) {
            wires.forEach { wire ->
                val fromNode = nodes.find { it.id == wire.fromId }
                val toNode = nodes.find { it.id == wire.toId }

                if (fromNode != null && toNode != null) {
                    val start = fromNode.position + Offset(nodeWidthPx, nodeHeightPx / 2)
                    val end = toNode.position + Offset(0f, nodeHeightPx / 2)

                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        cubicTo(
                            start.x + 80f, start.y,
                            end.x - 80f, end.y,
                            end.x, end.y
                        )
                    }
                    drawPath(path, color = wireColor.copy(alpha = 0.6f), style = Stroke(width = 3f))
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
    val borderColor = if (isConnecting) Color.Yellow else accentColor.copy(alpha = 0.5f)

    val animatedOffset by animateOffsetAsState(
        targetValue = node.position,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "NodeOffset"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt()) }
            .zIndex(1f)
    ) {
        // Output Port (right)
        Surface(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 7.dp)
                .clickable { onConnectClick() }
                .zIndex(2f),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(2.dp, accentColor)
        ) {}

        // Input Port (left) - only for Actions
        if (node.type == NodeType.ACTION) {
            Surface(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-7).dp)
                    .zIndex(2f),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, accentColor)
            ) {}
        }

        Card(
            modifier = Modifier
                .width(180.dp)
                .height(80.dp)
                .pointerInput(node.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPositionChange(node.position + dragAmount)
                    }
                }
                .clickable { onNodeClick() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = if (node.type == NodeType.TRIGGER) Icons.Default.Bolt else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        color = Color.Black,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                    Text(
                        text = if (node.type == NodeType.TRIGGER) "Trigger" else "Action",
                        color = accentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(onClick = onConfigClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray)
                }
            }
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
