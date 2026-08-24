package com.example.liturgicalwallpaper.domain

import com.example.liturgicalwallpaper.calendar
import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiturgicalStateCalculatorTest {
    private val zone = ZoneId.of("America/Bogota")
    private val calculator = LiturgicalStateCalculator()
    private val date = LocalDate.of(2026, 8, 22)
    private val calendar = calendar(date, 3, setOf(date), listOf(LiturgicalColor.WHITE, LiturgicalColor.GREEN, LiturgicalColor.RED))
    private fun state(day: LocalDate, time: LocalTime, source: com.example.liturgicalwallpaper.domain.model.LiturgicalCalendar = calendar,
        vespers: LocalTime = LocalTime.of(18, 0)) = calculator.calculate(source, day.atTime(time).atZone(zone).toInstant(), vespers)!!

    @Test fun `1759 with transition stays today`() = assertEquals(date.toString(), state(date, LocalTime.of(17,59)).effectiveDay.date)
    @Test fun `1800 with transition uses tomorrow`() = assertTrue(state(date, LocalTime.of(18,0)).isFirstVespers)
    @Test fun `1801 with transition uses tomorrow`() = assertEquals(LiturgicalColor.GREEN, state(date, LocalTime.of(18,1)).effectiveDay.primaryColor)
    @Test fun `2359 with transition uses tomorrow`() = assertEquals(date.plusDays(1).toString(), state(date, LocalTime.of(23,59)).effectiveDay.date)
    @Test fun `1800 without transition stays today`() {
        val noTransition = calendar(date, 2)
        assertFalse(state(date, LocalTime.of(18,0), noTransition).isFirstVespers)
    }
    @Test fun `midnight selects new civil day`() = assertEquals(date.plusDays(1).toString(), state(date.plusDays(1), LocalTime.MIDNIGHT).effectiveDay.date)
    @Test fun `custom 1700 boundary`() {
        assertFalse(state(date, LocalTime.of(16,59), vespers = LocalTime.of(17,0)).isFirstVespers)
        assertTrue(state(date, LocalTime.of(17,0), vespers = LocalTime.of(17,0)).isFirstVespers)
    }
    @Test fun `month year and leap boundaries`() {
        listOf(LocalDate.of(2026,1,31), LocalDate.of(2026,12,31), LocalDate.of(2028,2,29)).forEach { boundary ->
            val c = calendar(boundary, 2, setOf(boundary))
            assertEquals(boundary.plusDays(1).toString(), state(boundary, LocalTime.of(18,0), c).effectiveDay.date)
        }
    }
}
