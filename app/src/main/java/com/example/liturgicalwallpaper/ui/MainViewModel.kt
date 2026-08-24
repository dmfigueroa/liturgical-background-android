package com.example.liturgicalwallpaper.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liturgicalwallpaper.BuildConfig
import com.example.liturgicalwallpaper.appGraph
import com.example.liturgicalwallpaper.data.cache.CachedCalendar
import com.example.liturgicalwallpaper.data.repository.RefreshResult
import com.example.liturgicalwallpaper.domain.LiturgicalState
import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import com.example.liturgicalwallpaper.scheduling.AutomaticWork
import com.example.liturgicalwallpaper.scheduling.NextTransitionCalculator
import com.example.liturgicalwallpaper.scheduling.TransitionHandler
import com.example.liturgicalwallpaper.settings.AppSettings
import com.example.liturgicalwallpaper.settings.WallpaperTarget
import com.example.liturgicalwallpaper.wallpaper.ApplyResult
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val cache: CachedCalendar? = null,
    val liturgicalState: LiturgicalState? = null,
    val settings: AppSettings = AppSettings(),
    val wallpaperFiles: Map<LiturgicalColor, File?> = emptyMap(),
    val refreshing: Boolean = false,
    val degradedMessage: String? = null,
    val simulatedTime: LocalTime? = null,
)

sealed interface UiMessage {
    data object WallpaperUpdated : UiMessage
    data object WallpaperFailed : UiMessage
    data class WallpaperMissing(val color: String) : UiMessage
    data object CalendarUpdated : UiMessage
    data object ServerUnavailable : UiMessage
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.appGraph
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 2)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch { graph.settings.settings.collectLatest { settings ->
            _state.update { it.copy(settings = settings) }
            recalculate()
        } }
        viewModelScope.launch {
            loadCache()
            applyAutomatically()
            refresh()
        }
    }

    private fun now(): Instant {
        val simulated = _state.value.simulatedTime ?: return Clock.systemUTC().instant()
        val zone = _state.value.cache?.calendar?.zoneId() ?: ZoneId.of("America/Bogota")
        return LocalDate.now(zone).atTime(simulated).atZone(zone).toInstant()
    }

    private suspend fun loadCache() {
        val cache = graph.calendarRepository.cached()
        _state.update { it.copy(cache = cache, wallpaperFiles = LiturgicalColor.entries.associateWith(graph.wallpaperRepository::fileFor)) }
        recalculate()
    }

    private fun recalculate() {
        val current = _state.value
        val calculated = current.cache?.let { graph.calculator.calculate(it.calendar, now(), current.settings.vespersTime) }
        _state.update { it.copy(liturgicalState = calculated) }
    }

    fun refresh() = viewModelScope.launch {
        if (_state.value.refreshing) return@launch
        _state.update { it.copy(refreshing = true) }
        when (val result = graph.calendarRepository.refresh()) {
            is RefreshResult.Updated -> {
                _state.update { it.copy(cache = result.cache, degradedMessage = null) }
                _messages.emit(UiMessage.CalendarUpdated)
                recalculate(); applyAutomatically(); reschedule()
            }
            is RefreshResult.Unchanged -> _state.update { it.copy(cache = result.cache, degradedMessage = null) }
            is RefreshResult.Failed -> {
                _state.update { it.copy(cache = result.retained, degradedMessage = result.message) }
                _messages.emit(UiMessage.ServerUnavailable)
            }
        }
        _state.update { it.copy(refreshing = false) }
        recalculate()
    }

    fun setAutomatic(enabled: Boolean) = viewModelScope.launch {
        graph.settings.setAutomatic(enabled)
        _state.update { it.copy(settings = it.settings.copy(automatic = enabled)) }
        if (enabled) { applyAutomatically(force = true); reschedule(); AutomaticWork.schedule(getApplication()) }
        else { graph.transitionScheduler.cancel(); AutomaticWork.cancel(getApplication()) }
    }

    fun setVespersTime(time: LocalTime) = viewModelScope.launch {
        graph.settings.setVespersTime(time)
        _state.update { it.copy(settings = it.settings.copy(vespersTime = time)) }
        recalculate(); applyAutomatically(); reschedule()
    }
    fun setTarget(target: WallpaperTarget) = viewModelScope.launch {
        graph.settings.setTarget(target)
        _state.update { it.copy(settings = it.settings.copy(target = target)) }
    }

    fun importWallpaper(color: LiturgicalColor, uri: Uri) = viewModelScope.launch {
        runCatching { graph.wallpaperRepository.import(color, uri) }.onSuccess { file ->
            _state.update { it.copy(wallpaperFiles = it.wallpaperFiles + (color to file)) }
            if (_state.value.settings.automatic && _state.value.liturgicalState?.effectiveDay?.primaryColor == color) applyAutomatically(true)
        }.onFailure { _messages.emit(UiMessage.WallpaperFailed) }
    }

    fun applyNow() = viewModelScope.launch {
        val current = _state.value.liturgicalState ?: return@launch
        report(graph.wallpaperCoordinator.apply(current, force = true))
    }
    private suspend fun applyAutomatically(force: Boolean = false) {
        if (!_state.value.settings.automatic) return
        _state.value.liturgicalState?.let { report(graph.wallpaperCoordinator.apply(it, force)) }
    }
    private suspend fun report(result: ApplyResult) = when (result) {
        ApplyResult.Applied -> _messages.emit(UiMessage.WallpaperUpdated)
        ApplyResult.AlreadyCurrent -> Unit
        is ApplyResult.Missing -> _messages.emit(UiMessage.WallpaperMissing(result.colorName))
        is ApplyResult.Failed -> _messages.emit(UiMessage.WallpaperFailed)
    }
    private fun reschedule() {
        val current = _state.value
        if (!current.settings.automatic) return
        current.cache?.let { NextTransitionCalculator.next(it.calendar, now(), current.settings.vespersTime) }
            ?.let(graph.transitionScheduler::schedule)
    }

    fun simulate(time: LocalTime?) { if (BuildConfig.DEBUG) { _state.update { it.copy(simulatedTime = time) }; recalculate() } }
    fun runTransitionNow() = viewModelScope.launch { if (BuildConfig.DEBUG) {
        val fixed = Clock.fixed(now(), ZoneId.of("UTC")); TransitionHandler.run(getApplication(), fixed); loadCache()
    } }
}
