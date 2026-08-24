package com.example.liturgicalwallpaper.data

import com.example.liturgicalwallpaper.calendar
import com.example.liturgicalwallpaper.data.api.ApiResult
import com.example.liturgicalwallpaper.data.api.CalendarRemoteSource
import com.example.liturgicalwallpaper.data.cache.CachedCalendar
import com.example.liturgicalwallpaper.data.cache.CalendarCacheCodec
import com.example.liturgicalwallpaper.data.cache.CalendarStore
import com.example.liturgicalwallpaper.data.repository.CalendarRepository
import com.example.liturgicalwallpaper.data.repository.RefreshResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRepositoryTest {
    private val now = Instant.parse("2026-08-22T13:00:00Z")
    private val validJson = """{
      "date":"2026-08-22",
      "today":{"date":"2026-08-22","season":"Ordinary","celebration":{"rank":null,"names":["Mary"]},
      "colors":{"primary":"white","alternatives":[],"sourceLabel":"Blanco"},"evening":{"transitionsToNextDay":true,"reason":"first-vespers"}},
      "tomorrow":{"date":"2026-08-23","season":"Ordinary","celebration":{"rank":"Sunday","names":["Sunday"]},
      "colors":{"primary":"green","alternatives":[],"sourceLabel":"Verde"},"evening":{"transitionsToNextDay":false}}
      } """

    @Test fun `valid cache loads and invalid cache is rejected`() {
        val cached = CachedCalendar(calendar(LocalDate.of(2026,8,22)), "tag", now.toString(), now.toString())
        val codec = CalendarCacheCodec()
        assertNotNull(codec.decode(codec.encode(cached)))
        assertNull(codec.decode("{not-json"))
    }

    @Test fun `valid response replaces cache and next request sends etag`() = runTest {
        val store = MemoryStore(null)
        val remote = FakeRemote(mutableListOf(ApiResult.Updated(validJson, "etag-1"), ApiResult.NotModified))
        val repository = CalendarRepository(remote, store, clock = Clock.fixed(now, ZoneOffset.UTC))
        assertTrue(repository.refresh() is RefreshResult.Updated)
        assertTrue(repository.refresh() is RefreshResult.Unchanged)
        assertEquals(listOf(null, "etag-1"), remote.etags)
        assertEquals("etag-1", store.value?.etag)
        assertEquals("", store.value?.calendar?.days?.first()?.celebration?.rank)
    }

    @Test fun `failed refresh preserves cache`() = runTest {
        val old = CachedCalendar(calendar(LocalDate.of(2026,8,22)), "old", now.toString(), now.toString())
        val store = MemoryStore(old)
        val result = CalendarRepository(FakeRemote(mutableListOf(ApiResult.Failed("offline"))), store).refresh()
        assertTrue(result is RefreshResult.Failed)
        assertSame(old, store.value)
    }

    @Test fun `malformed response preserves cache`() = runTest {
        val old = CachedCalendar(calendar(LocalDate.of(2026,8,22)), null, now.toString(), now.toString())
        val store = MemoryStore(old)
        CalendarRepository(FakeRemote(mutableListOf(ApiResult.Updated("{}", "new"))), store).refresh()
        assertSame(old, store.value)
    }

    private class MemoryStore(var value: CachedCalendar?) : CalendarStore {
        override suspend fun load() = value
        override suspend fun save(value: CachedCalendar) { this.value = value }
    }
    private class FakeRemote(private val results: MutableList<ApiResult>) : CalendarRemoteSource {
        val etags = mutableListOf<String?>()
        override suspend fun fetch(etag: String?): ApiResult { etags += etag; return results.removeAt(0) }
    }
}
