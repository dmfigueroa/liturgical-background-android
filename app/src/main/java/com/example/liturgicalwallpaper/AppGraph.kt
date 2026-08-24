package com.example.liturgicalwallpaper

import android.content.Context
import com.example.liturgicalwallpaper.data.api.CalendarApiClient
import com.example.liturgicalwallpaper.data.api.CalendarJson
import com.example.liturgicalwallpaper.data.cache.CalendarCache
import com.example.liturgicalwallpaper.data.repository.CalendarRepository
import com.example.liturgicalwallpaper.domain.LiturgicalStateCalculator
import com.example.liturgicalwallpaper.scheduling.TransitionScheduler
import com.example.liturgicalwallpaper.settings.SettingsRepository
import com.example.liturgicalwallpaper.wallpaper.WallpaperCoordinator
import com.example.liturgicalwallpaper.wallpaper.WallpaperRepository
import com.example.liturgicalwallpaper.wallpaper.WallpaperService

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsRepository(appContext)
    val cache = CalendarCache(appContext)
    val calendarRepository = CalendarRepository(
        CalendarApiClient(BuildConfig.LITURGICAL_API_URL), cache, CalendarJson(),
    )
    val calculator = LiturgicalStateCalculator()
    val wallpaperRepository = WallpaperRepository(appContext)
    val wallpaperCoordinator = WallpaperCoordinator(
        wallpaperRepository, WallpaperService(appContext), settings,
    )
    val transitionScheduler = TransitionScheduler(appContext)
}
