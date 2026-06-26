package com.example.flowmode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.flowmode.ui.auth.AuthViewModel
import com.example.flowmode.util.PermissionHandler

@Composable
fun SettingsScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    var hasDndAccess by remember { mutableStateOf(PermissionHandler.checkNotificationPolicyAccess(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Permissions", style = MaterialTheme.typography.titleLarge)
        ListItem(
            headlineContent = { Text("Do Not Disturb Access") },
            supportingContent = { Text("Required for Silent/DND toggle action") },
            trailingContent = {
                if (hasDndAccess) {
                    Icon(Icons.Default.Check, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = { PermissionHandler.openNotificationPolicySettings(context) }) {
                        Text("Grant")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { authViewModel.signOut() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Sign Out")
        }
    }
}
