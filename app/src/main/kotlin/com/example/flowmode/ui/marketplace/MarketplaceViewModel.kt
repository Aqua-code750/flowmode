package com.example.flowmode.ui.marketplace

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmode.data.model.GeneratedNode
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MarketplaceViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    
    private val _unlockedNodes = mutableStateListOf<GeneratedNode>()
    val unlockedNodes: List<GeneratedNode> = _unlockedNodes

    val currentGeneratedNode = mutableStateOf<GeneratedNode?>(null)

    private val userNodesCollection
        get() = auth.currentUser?.let { 
            db.collection("users").document(it.uid).collection("unlockedNodes") 
        }

    init {
        fetchUnlockedNodes()
    }

    private fun fetchUnlockedNodes() {
        viewModelScope.launch {
            userNodesCollection?.get()?.await()?.let { snapshot ->
                _unlockedNodes.clear()
                _unlockedNodes.addAll(snapshot.toObjects(GeneratedNode::class.java))
            }
        }
    }

    fun generateNewNode() {
        currentGeneratedNode.value = NodeGenerator.generateRandomNode()
    }

    fun generateFromPrompt(prompt: String) {
        currentGeneratedNode.value = NodeGenerator.generateFromDescription(prompt)
    }

    fun acceptNode() {
        currentGeneratedNode.value?.let { node ->
            val docRef = userNodesCollection?.document() ?: return
            val newNode = node.copy(id = docRef.id)
            docRef.set(newNode)
            _unlockedNodes.add(newNode)
            currentGeneratedNode.value = null
        }
    }

    fun reroll() {
        generateNewNode()
    }
}
