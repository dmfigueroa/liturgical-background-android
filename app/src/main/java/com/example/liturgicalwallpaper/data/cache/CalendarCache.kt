package com.example.liturgicalwallpaper.data.cache

import android.content.Context
import com.example.liturgicalwallpaper.domain.model.LiturgicalCalendar
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedCalendar(
    val calendar: LiturgicalCalendar,
    val etag: String? = null,
    val lastSuccessfulCheck: String,
    val lastSuccessfulUpdate: String,
)

interface CalendarStore {
    suspend fun load(): CachedCalendar?
    suspend fun save(value: CachedCalendar)
}

class CalendarCache(context: Context, private val json: Json = Json) : CalendarStore {
    private val directory = File(context.filesDir, "calendar")
    private val target = File(directory, "calendar-cache.json")

    override suspend fun load(): CachedCalendar? = withContext(Dispatchers.IO) {
        if (!target.isFile) return@withContext null
        CalendarCacheCodec(json).decode(target.readText())
    }

    override suspend fun save(value: CachedCalendar) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val temporary = File(directory, "calendar-cache.tmp")
        temporary.writeText(CalendarCacheCodec(json).encode(value))
        check(temporary.renameTo(target) || run { temporary.copyTo(target, overwrite = true); temporary.delete() }) {
            "Unable to replace calendar cache"
        }
    }
}

class CalendarCacheCodec(private val json: Json = Json) {
    fun encode(value: CachedCalendar): String = json.encodeToString(CachedCalendar.serializer(), value)
    fun decode(text: String): CachedCalendar? = runCatching {
        val cached = json.decodeFromString<CachedCalendar>(text)
        val from = java.time.LocalDate.parse(cached.calendar.validFrom)
        val through = java.time.LocalDate.parse(cached.calendar.validThrough)
        require(!through.isBefore(from) && cached.calendar.days.isNotEmpty())
        cached.calendar.days.forEach { java.time.LocalDate.parse(it.date) }
        cached.calendar.zoneId()
        cached
    }.getOrNull()
}
