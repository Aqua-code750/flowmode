package com.example.flowmode

import android.app.Application
import com.google.firebase.FirebaseApp

class FlowModeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
