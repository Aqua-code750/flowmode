package com.example.flowmode.ui.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    
    val user = mutableStateOf(auth.currentUser)
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun signInWithGoogle(credential: AuthCredential) {
        isLoading.value = true
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    user.value = auth.currentUser
                } else {
                    error.value = task.exception?.message
                }
            }
    }

    fun signIn(email: String, pass: String) {
        isLoading.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    user.value = auth.currentUser
                } else {
                    error.value = task.exception?.message
                }
            }
    }

    fun signUp(email: String, pass: String) {
        isLoading.value = true
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    user.value = auth.currentUser
                } else {
                    error.value = task.exception?.message
                }
            }
    }

    fun signOut() {
        auth.signOut()
        user.value = null
    }
}
