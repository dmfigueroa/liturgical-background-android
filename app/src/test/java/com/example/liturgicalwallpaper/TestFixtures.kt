package com.example.liturgicalwallpaper

import com.example.liturgicalwallpaper.domain.model.Celebration
import com.example.liturgicalwallpaper.domain.model.Evening
import com.example.liturgicalwallpaper.domain.model.LiturgicalCalendar
import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import com.example.liturgicalwallpaper.domain.model.LiturgicalDay
import java.time.LocalDate

fun calendar(
    start: LocalDate,
    days: Int = 3,
    transitionDates: Set<LocalDate> = emptySet(),
    colors: List<LiturgicalColor> = List(days) { LiturgicalColor.GREEN },
): LiturgicalCalendar = LiturgicalCalendar(
    calendar = "colombia-cec", timezone = "America/Bogota",
    fetchedAt = "2026-08-22T08:00:00-05:00", validFrom = start.toString(),
    validThrough = start.plusDays(days.toLong() - 1).toString(), stale = false,
    days = (0 until days).map { offset ->
        val date = start.plusDays(offset.toLong())
        LiturgicalDay(date.toString(), "Season", Celebration("Rank", listOf("Day $date")),
            colors[offset], evening = Evening(date in transitionDates,
                if (date in transitionDates) "first-vespers" else null))
    },
)
