package com.example.flowmode.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flowmode.MainActivity
import java.util.Locale
import android.bluetooth.BluetoothManager
import android.telephony.SmsManager
import java.net.HttpURLConnection
import java.net.URL

object FlowActions {

    private const val CHANNEL_ID = "flow_notifications"
    private var tts: TextToSpeech? = null

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
            }
        }
    }

    fun toggleWifi(context: Context, config: Map<String, Any>) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val enable = config["enable"] as? Boolean ?: true
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

    fun openApp(context: Context, config: Map<String, Any>) {
        val packageName = config["packageName"] as? String ?: return
        Log.d("FlowActions", "Opening app: $packageName")
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        } ?: Log.e("FlowActions", "App not found: $packageName")
    }

    fun logEvent(config: Map<String, Any>) {
        val message = config["message"] as? String ?: "Event Logged"
        Log.i("FlowEvent", message)
    }

    fun playSound(context: Context) {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrate(context: Context, config: Map<String, Any>) {
        val duration = (config["duration"] as? Number)?.toLong() ?: 500L
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun speakText(context: Context, config: Map<String, Any>) {
        val text = config["text"] as? String ?: "Hello from FlowMode"
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun toggleBluetooth(context: Context, config: Map<String, Any>) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val enable = config["enable"] as? Boolean ?: true
        if (enable) {
            @Suppress("DEPRECATION")
            adapter?.enable()
        } else {
            @Suppress("DEPRECATION")
            adapter?.disable()
        }
    }

    fun sendSms(context: Context, config: Map<String, Any>) {
        val phoneNumber = config["phoneNumber"] as? String ?: return
        val message = config["message"] as? String ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager?.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun waitDelay(config: Map<String, Any>) {
        val seconds = (config["seconds"] as? Number)?.toLong() ?: 1L
        try {
            Thread.sleep(seconds * 1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun makeHttpRequest(config: Map<String, Any>) {
        val urlString = config["url"] as? String ?: return
        val method = config["method"] as? String ?: "GET"
        
        Thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                val responseCode = connection.responseCode
                Log.d("FlowActions", "HTTP $method to $urlString returned $responseCode")
            } catch (e: Exception) {
                Log.e("FlowActions", "HTTP Request failed", e)
            }
        }.start()
    }

    fun takeScreenshot(context: Context) {
        Log.i("FlowActions", "Screenshot action triggered (Requires System Permissions)")
    }
}
