package com.example.liturgicalwallpaper.scheduling

import com.example.liturgicalwallpaper.calendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NextTransitionCalculatorTest {
    private val zone = ZoneId.of("America/Bogota")
    @Test fun `schedules first vespers when it is the next transition`() {
        val date = LocalDate.of(2026,8,22)
        val c = calendar(date, 2, setOf(date))
        val now = date.atTime(12,0).atZone(zone).toInstant()
        assertEquals(date.atTime(18,0).atZone(zone).toInstant(), NextTransitionCalculator.next(c, now, LocalTime.of(18,0)))
    }
    @Test fun `schedules midnight after vespers or when no transition`() {
        val date = LocalDate.of(2026,12,31)
        val c = calendar(date, 2)
        val now = date.atTime(12,0).atZone(zone).toInstant()
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), NextTransitionCalculator.next(c, now, LocalTime.of(18,0)))
    }
}
