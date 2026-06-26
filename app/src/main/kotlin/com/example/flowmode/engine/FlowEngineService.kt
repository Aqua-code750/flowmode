package com.example.flowmode.engine

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FlowEngineService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle background tasks if needed
        return START_STICKY
    }
}
