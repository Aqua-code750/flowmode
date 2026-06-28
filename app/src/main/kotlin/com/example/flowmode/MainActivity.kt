package com.example.flowmode

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flowmode.data.model.ActionType
import com.example.flowmode.data.model.TriggerType
import com.example.flowmode.ui.FlowNavigation
import com.example.flowmode.ui.Screen
import com.example.flowmode.ui.auth.AuthViewModel
import com.example.flowmode.ui.canvas.CanvasViewModel
import com.example.flowmode.ui.marketplace.MarketplaceViewModel
import com.example.flowmode.ui.theme.FlowModeTheme
import com.example.flowmode.util.PermissionHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the background engine service
        val intent = Intent(this, com.example.flowmode.engine.FlowEngineService::class.java)
        startService(intent)
        
        // Full screen edge-to-edge experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request basic permissions on startup
        if (!PermissionHandler.hasBasicPermissions(this)) {
            PermissionHandler.requestBasicPermissions(this)
        }

        setContent {
            FlowModeTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val canvasViewModel: CanvasViewModel = viewModel()
    val marketplaceViewModel: MarketplaceViewModel = viewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = authViewModel.user.value != null && currentRoute != Screen.Auth.route
    var showNodePicker by remember { mutableStateOf(false) }

    fun String.capitalizeCustom() = this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = { Text("Flows") },
                        selected = currentRoute == Screen.FlowList.route,
                        onClick = { navController.navigate(Screen.FlowList.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        label = { Text("Editor") },
                        selected = currentRoute == Screen.FlowEditor.route,
                        onClick = { navController.navigate(Screen.FlowEditor.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Shop, contentDescription = null) },
                        label = { Text("Market") },
                        selected = currentRoute == Screen.Marketplace.route,
                        onClick = { navController.navigate(Screen.Marketplace.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.FlowEditor.route) {
                FloatingActionButton(onClick = { showNodePicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Node")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            FlowNavigation(
                navController = navController,
                authViewModel = authViewModel,
                canvasViewModel = canvasViewModel,
                marketplaceViewModel = marketplaceViewModel
            )

            if (showNodePicker) {
                AlertDialog(
                    onDismissRequest = { showNodePicker = false },
                    title = { Text("Add Node") },
                    text = {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Triggers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            TriggerType.entries.forEach { type ->
                                Button(
                                    onClick = { 
                                        canvasViewModel.addTrigger(type)
                                        showNodePicker = false 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text(type.name.replace("_", " ").capitalizeCustom())
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Actions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                            ActionType.entries.forEach { type ->
                                Button(
                                    onClick = { 
                                        canvasViewModel.addAction(type)
                                        showNodePicker = false 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text(type.name.replace("_", " ").capitalizeCustom())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showNodePicker = false }) { Text("Close") }
                    }
                )
            }
        }
    }

    LaunchedEffect(authViewModel.user.value) {
        if (authViewModel.user.value == null) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0)
            }
        } else if (currentRoute == Screen.Auth.route) {
            navController.navigate(Screen.FlowList.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }
}
