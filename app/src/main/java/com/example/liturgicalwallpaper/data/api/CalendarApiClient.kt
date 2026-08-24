package com.example.liturgicalwallpaper.data.api

import com.example.liturgicalwallpaper.domain.model.Celebration
import com.example.liturgicalwallpaper.domain.model.Evening
import com.example.liturgicalwallpaper.domain.model.LiturgicalCalendar
import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import com.example.liturgicalwallpaper.domain.model.LiturgicalDay
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface ApiResult {
    data class Updated(val body: String, val etag: String?) : ApiResult
    data object NotModified : ApiResult
    data class Failed(val message: String) : ApiResult
}

interface CalendarRemoteSource { suspend fun fetch(etag: String?): ApiResult }

class CalendarApiClient(private val endpointUrl: String) : CalendarRemoteSource {
    override suspend fun fetch(etag: String?): ApiResult = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(endpointUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            etag?.let { connection.setRequestProperty("If-None-Match", it) }
            try {
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> ApiResult.Updated(
                        connection.inputStream.bufferedReader().use { it.readText() },
                        connection.getHeaderField("ETag"),
                    )
                    HttpURLConnection.HTTP_NOT_MODIFIED -> ApiResult.NotModified
                    else -> ApiResult.Failed("HTTP ${connection.responseCode}")
                }
            } finally { connection.disconnect() }
        }.getOrElse { ApiResult.Failed(it.message ?: "Network request failed") }
    }
}

@Serializable private data class TodayResponse(val date: String, val today: ApiDay, val tomorrow: ApiDay)
@Serializable private data class ApiDay(
    val date: String,
    val season: String,
    val celebration: ApiCelebration,
    val colors: ApiColors,
    val evening: Evening,
)
@Serializable private data class ApiCelebration(
    val rank: String? = null,
    val names: List<String> = emptyList(),
)
@Serializable private data class ApiColors(
    val primary: String,
    val alternatives: List<String> = emptyList(),
    val sourceLabel: String,
)

class CalendarJson(private val json: Json = Json { ignoreUnknownKeys = true }) {
    fun parseAndValidate(body: String): LiturgicalCalendar {
        val response = json.decodeFromString<TodayResponse>(body)
        val responseDate = java.time.LocalDate.parse(response.date)
        val from = java.time.LocalDate.parse(response.today.date)
        val through = java.time.LocalDate.parse(response.tomorrow.date)
        require(responseDate == from && through == from.plusDays(1)) {
            "Today response must contain today and the following day"
        }
        val uniqueDays = linkedMapOf<String, LiturgicalDay>()
        listOf(response.today, response.tomorrow).forEach { day ->
            val date = java.time.LocalDate.parse(day.date)
            require(!date.isBefore(from) && !date.isAfter(through)) { "Day outside response range" }
            uniqueDays[day.date] = LiturgicalDay(
                date = day.date,
                season = day.season,
                celebration = Celebration(day.celebration.rank.orEmpty(), day.celebration.names),
                primaryColor = LiturgicalColor.fromWire(day.colors.primary),
                alternativeColors = day.colors.alternatives.map(LiturgicalColor::fromWire),
                sourceColorLabel = day.colors.sourceLabel,
                evening = day.evening,
            )
        }
        require(uniqueDays.size == 2) { "Today response has duplicate dates" }
        return LiturgicalCalendar(
            calendar = "colombia-cec",
            timezone = "America/Bogota",
            fetchedAt = "${response.date}T00:00:00-05:00",
            validFrom = response.today.date,
            validThrough = response.tomorrow.date,
            stale = false,
            days = uniqueDays.values.sortedBy { it.date },
        )
    }
}
