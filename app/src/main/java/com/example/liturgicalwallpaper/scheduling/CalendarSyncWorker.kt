package com.example.liturgicalwallpaper.scheduling

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.liturgicalwallpaper.appGraph
import com.example.liturgicalwallpaper.data.repository.RefreshResult

class CalendarSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return when (applicationContext.appGraph.calendarRepository.refresh()) {
            is RefreshResult.Failed -> Result.retry()
            else -> { TransitionHandler.run(applicationContext); Result.success() }
        }
    }
}
