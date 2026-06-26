package com.example.flowmode.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.flowmode.MainActivity

object FlowActions {

    private const val CHANNEL_ID = "flow_notifications"

    fun showNotification(context: Context, config: Map<String, Any>) {
        val title = config["title"] as? String ?: "FlowMode"
        val message = config["message"] as? String ?: "Action triggered!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Flow Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun toggleDnd(context: Context, config: Map<String, Any>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val enable = config["enable"] as? Boolean ?: true
                val filter = if (enable) {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(filter)
            } else {
                // Should prompt for permission in UI
            }
        }
    }

    fun toggleWifi(context: Context, config: Map<String, Any>) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val enable = config["enable"] as? Boolean ?: true
        // Note: setWifiEnabled is deprecated for apps targeting API 29+ except for system apps
        // but we'll try for now or use the suggestion if it's a legacy requirement.
        // For Android 10+, we should use Settings Panel or similar, but for automation
        // it's tricky. Let's stick to the basic for now.
        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = enable
    }

    fun setBrightness(context: Context, config: Map<String, Any>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                val level = (config["level"] as? Number)?.toInt() ?: 128
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, level)
            }
        }
    }

    fun toggleFlashlight(context: Context, config: Map<String, Any>) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val enable = config["enable"] as? Boolean ?: true
            cameraManager.setTorchMode(cameraId, enable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
