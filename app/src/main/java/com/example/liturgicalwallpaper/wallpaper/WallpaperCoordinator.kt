package com.example.liturgicalwallpaper.wallpaper

import com.example.liturgicalwallpaper.domain.LiturgicalState
import com.example.liturgicalwallpaper.settings.AppliedWallpaperState
import com.example.liturgicalwallpaper.settings.SettingsRepository
import java.time.Clock

sealed interface ApplyResult {
    data object Applied : ApplyResult
    data object AlreadyCurrent : ApplyResult
    data class Missing(val colorName: String) : ApplyResult
    data class Failed(val message: String) : ApplyResult
}

class WallpaperCoordinator(
    private val files: WallpaperRepository,
    private val service: WallpaperService,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun apply(state: LiturgicalState, force: Boolean = false): ApplyResult {
        val color = state.effectiveDay.primaryColor
        val file = files.fileFor(color) ?: return ApplyResult.Missing(color.displayName)
        val identity = files.identity(file)
        val preferences = settings.current()
        val old = preferences.lastApplied
        if (!force && old?.effectiveDate == state.effectiveDay.date && old.color == color.wireName &&
            old.fileIdentity == identity) return ApplyResult.AlreadyCurrent
        return service.apply(file, preferences.target).fold(
            onSuccess = {
                settings.setLastApplied(AppliedWallpaperState(
                    state.effectiveDay.date, color.wireName, identity, clock.instant().toString(),
                ))
                ApplyResult.Applied
            },
            onFailure = { ApplyResult.Failed(it.message ?: "Wallpaper update failed") },
        )
    }
}
