package com.example.flowmode.data.repository

import com.example.flowmode.data.model.Flow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FlowRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    
    private val _flows = MutableStateFlow<List<Flow>>(emptyList())
    val flows = _flows.asStateFlow()

    private val userFlowsCollection
        get() = auth.currentUser?.let { 
            db.collection("users").document(it.uid).collection("flows") 
        }

    suspend fun fetchFlows() {
        userFlowsCollection?.get()?.await()?.let { snapshot ->
            val fetchedFlows = snapshot.toObjects(Flow::class.java)
            _flows.value = fetchedFlows
        }
    }

    fun addFlow(flow: Flow) {
        val docRef = userFlowsCollection?.document() ?: return
        val newFlow = flow.copy(id = docRef.id)
        docRef.set(newFlow)
        _flows.value = _flows.value + newFlow
    }

    fun updateFlow(updatedFlow: Flow) {
        userFlowsCollection?.document(updatedFlow.id)?.set(updatedFlow)
        _flows.value = _flows.value.map { if (it.id == updatedFlow.id) updatedFlow else it }
    }

    fun deleteFlow(flowId: String) {
        userFlowsCollection?.document(flowId)?.delete()
        _flows.value = _flows.value.filter { it.id != flowId }
    }

    fun getEnabledFlows(): List<Flow> {
        return _flows.value.filter { it.enabled }
    }
    
    companion object {
        private var instance: FlowRepository? = null
        fun getInstance(): FlowRepository {
            if (instance == null) {
                instance = FlowRepository()
            }
            return instance!!
        }
    }
}
