package com.example.flowmode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowmode.data.repository.FlowRepository

@Composable
fun FlowListScreen() {
    val repository = FlowRepository.getInstance()
    val flows by repository.flows.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Flows", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (flows.isEmpty()) {
            Text("No flows yet. Go to Editor to create one!")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(flows) { flow ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(flow.name, style = MaterialTheme.typography.titleMedium)
                            Text("${flow.actions.size} actions", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = flow.enabled, onCheckedChange = { 
                            repository.updateFlow(flow.copy(enabled = it))
                        })
                    }
                }
            }
        }
    }
}
