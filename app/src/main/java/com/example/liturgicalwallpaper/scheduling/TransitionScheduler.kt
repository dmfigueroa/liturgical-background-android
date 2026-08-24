package com.example.liturgicalwallpaper.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.liturgicalwallpaper.domain.model.LiturgicalCalendar
import java.time.Instant
import java.time.LocalTime

object NextTransitionCalculator {
    fun next(calendar: LiturgicalCalendar, now: Instant, vespersTime: LocalTime): Instant? {
        val zonedNow = now.atZone(calendar.zoneId())
        val today = calendar.day(zonedNow.toLocalDate()) ?: return null
        val vespers = zonedNow.toLocalDate().atTime(vespersTime).atZone(calendar.zoneId())
        if (today.evening.transitionsToNextDay && vespers.toInstant().isAfter(now)) return vespers.toInstant()
        return zonedNow.toLocalDate().plusDays(1).atStartOfDay(calendar.zoneId()).toInstant()
    }
}

class TransitionScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val pendingIntent: PendingIntent get() = PendingIntent.getBroadcast(
        context, 42, Intent(context, TransitionReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(instant: Instant) {
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, instant.toEpochMilli(), 15 * 60 * 1000L, pendingIntent)
    }
    fun cancel() = alarmManager.cancel(pendingIntent)
}
