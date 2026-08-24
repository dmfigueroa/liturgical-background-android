package com.example.liturgicalwallpaper.data.repository

import com.example.liturgicalwallpaper.data.api.ApiResult
import com.example.liturgicalwallpaper.data.api.CalendarJson
import com.example.liturgicalwallpaper.data.api.CalendarRemoteSource
import com.example.liturgicalwallpaper.data.cache.CachedCalendar
import com.example.liturgicalwallpaper.data.cache.CalendarStore
import java.time.Clock

sealed interface RefreshResult {
    data class Updated(val cache: CachedCalendar) : RefreshResult
    data class Unchanged(val cache: CachedCalendar) : RefreshResult
    data class Failed(val message: String, val retained: CachedCalendar?) : RefreshResult
}

class CalendarRepository(
    private val remote: CalendarRemoteSource,
    private val store: CalendarStore,
    private val parser: CalendarJson = CalendarJson(),
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun cached(): CachedCalendar? = store.load()

    suspend fun refresh(): RefreshResult {
        val old = store.load()
        return when (val response = remote.fetch(old?.etag)) {
            is ApiResult.NotModified -> {
                if (old == null) RefreshResult.Failed("Server returned 304 without a cache", null)
                else {
                    val checked = old.copy(lastSuccessfulCheck = clock.instant().toString())
                    store.save(checked)
                    RefreshResult.Unchanged(checked)
                }
            }
            is ApiResult.Updated -> runCatching {
                val now = clock.instant().toString()
                val parsed = parser.parseAndValidate(response.body)
                val updated = CachedCalendar(parsed, response.etag, now, now)
                store.save(updated)
                RefreshResult.Updated(updated)
            }.getOrElse { RefreshResult.Failed(it.message ?: "Invalid calendar response", old) }
            is ApiResult.Failed -> RefreshResult.Failed(response.message, old)
        }
    }
}
