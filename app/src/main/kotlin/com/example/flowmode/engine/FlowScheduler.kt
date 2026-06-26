package com.example.flowmode.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowmode.data.model.Flow
import com.example.flowmode.data.model.TriggerType
import java.util.*

class FlowScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTimeTrigger(flow: Flow) {
        if (flow.trigger.type != TriggerType.TIME) return

        val hour = (flow.trigger.config["hour"] as? Number)?.toInt() ?: 0
        val minute = (flow.trigger.config["minute"] as? Number)?.toInt() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, FlowBroadcastReceiver::class.java).apply {
            action = "com.example.flowmode.ACTION_TIME_TRIGGER"
            putExtra("flowId", flow.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flow.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelTimeTrigger(flowId: String) {
        val intent = Intent(context, FlowBroadcastReceiver::class.java).apply {
            action = "com.example.flowmode.ACTION_TIME_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flowId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
