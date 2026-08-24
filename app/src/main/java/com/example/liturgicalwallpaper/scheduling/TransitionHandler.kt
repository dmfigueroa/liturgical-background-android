package com.example.liturgicalwallpaper.scheduling

import android.content.Context
import com.example.liturgicalwallpaper.appGraph
import java.time.Clock

object TransitionHandler {
    suspend fun run(context: Context, clock: Clock = Clock.systemUTC()) {
        val graph = context.appGraph
        val settings = graph.settings.current()
        if (!settings.automatic) { graph.transitionScheduler.cancel(); return }
        val cache = graph.calendarRepository.cached() ?: return
        val state = graph.calculator.calculate(cache.calendar, clock.instant(), settings.vespersTime)
        if (state != null) graph.wallpaperCoordinator.apply(state)
        NextTransitionCalculator.next(cache.calendar, clock.instant(), settings.vespersTime)
            ?.let(graph.transitionScheduler::schedule)
        AutomaticWork.schedule(context)
    }
}
