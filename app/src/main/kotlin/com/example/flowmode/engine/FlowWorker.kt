package com.example.flowmode.engine

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.flowmode.data.model.TriggerType

class FlowWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val triggerTypeStr = inputData.getString("trigger_type") ?: return Result.failure()
        val triggerType = TriggerType.valueOf(triggerTypeStr)
        
        val flowManager = FlowManager(applicationContext)
        flowManager.handleTrigger(triggerType)
        
        return Result.success()
    }
}
