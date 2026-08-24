package com.example.liturgicalwallpaper.domain.model

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
data class LiturgicalCalendar(
    val calendar: String,
    val timezone: String,
    val fetchedAt: String,
    val validFrom: String,
    val validThrough: String,
    val stale: Boolean,
    val days: List<LiturgicalDay>,
) {
    fun day(date: LocalDate): LiturgicalDay? = days.firstOrNull { it.date == date.toString() }
    fun zoneId(): ZoneId = ZoneId.of(timezone)
}
