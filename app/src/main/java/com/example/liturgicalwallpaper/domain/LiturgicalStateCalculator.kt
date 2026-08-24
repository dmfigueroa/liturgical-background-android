package com.example.liturgicalwallpaper.domain

import com.example.liturgicalwallpaper.domain.model.LiturgicalCalendar
import com.example.liturgicalwallpaper.domain.model.LiturgicalDay
import java.time.Instant
import java.time.LocalTime

data class LiturgicalState(
    val civilDay: LiturgicalDay,
    val effectiveDay: LiturgicalDay,
    val isFirstVespers: Boolean,
)

class LiturgicalStateCalculator {
    fun calculate(calendar: LiturgicalCalendar, now: Instant, vespersTime: LocalTime): LiturgicalState? {
        val localNow = now.atZone(calendar.zoneId())
        val today = calendar.day(localNow.toLocalDate()) ?: return null
        val useTomorrow = !localNow.toLocalTime().isBefore(vespersTime) &&
            today.evening.transitionsToNextDay
        val effective = if (useTomorrow) calendar.day(localNow.toLocalDate().plusDays(1)) ?: today else today
        return LiturgicalState(today, effective, useTomorrow && effective !== today)
    }
}
