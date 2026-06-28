package com.example.flowmode.data.repository

import android.content.Context
import com.example.flowmode.data.model.Flow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.File

class FlowRepository(private val context: Context) {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val gson = Gson()
    private val localFile = File(context.filesDir, "flows_cache.json")
    
    private val _flows = MutableStateFlow<List<Flow>>(loadLocalFlows())
    val flows = _flows.asStateFlow()

    private val userFlowsCollection
        get() = auth.currentUser?.let { 
            db.collection("users").document(it.uid).collection("flows") 
        }

    private fun loadLocalFlows(): List<Flow> {
        return try {
            if (localFile.exists()) {
                val json = localFile.readText()
                val type = object : TypeToken<List<Flow>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalFlows(flows: List<Flow>) {
        try {
            localFile.writeText(gson.toJson(flows))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchFlows() {
        userFlowsCollection?.get()?.await()?.let { snapshot ->
            val fetchedFlows = snapshot.toObjects(Flow::class.java)
            _flows.value = fetchedFlows
            saveLocalFlows(fetchedFlows)
        }
    }

    fun addFlow(flow: Flow) {
        val docRef = userFlowsCollection?.document() ?: return
        val newFlow = flow.copy(id = docRef.id)
        docRef.set(newFlow)
        val updatedList = _flows.value + newFlow
        _flows.value = updatedList
        saveLocalFlows(updatedList)
    }

    fun updateFlow(updatedFlow: Flow) {
        userFlowsCollection?.document(updatedFlow.id)?.set(updatedFlow)
        val updatedList = _flows.value.map { if (it.id == updatedFlow.id) updatedFlow else it }
        _flows.value = updatedList
        saveLocalFlows(updatedList)
    }

    fun deleteFlow(flowId: String) {
        userFlowsCollection?.document(flowId)?.delete()
        val updatedList = _flows.value.filter { it.id != flowId }
        _flows.value = updatedList
        saveLocalFlows(updatedList)
    }

    fun getEnabledFlows(): List<Flow> {
        return _flows.value.filter { it.enabled }
    }
    
    companion object {
        private var instance: FlowRepository? = null
        fun getInstance(context: Context): FlowRepository {
            if (instance == null) {
                instance = FlowRepository(context.applicationContext)
            }
            return instance!!
        }
    }
}
