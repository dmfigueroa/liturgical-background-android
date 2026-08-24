package com.example.liturgicalwallpaper.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class WallpaperTarget { HOME, HOME_AND_LOCK }

@Serializable
data class AppliedWallpaperState(
    val effectiveDate: String,
    val color: String,
    val fileIdentity: String,
    val appliedAt: String,
)

data class AppSettings(
    val automatic: Boolean = false,
    val vespersTime: LocalTime = LocalTime.of(18, 0),
    val target: WallpaperTarget = WallpaperTarget.HOME,
    val lastApplied: AppliedWallpaperState? = null,
)

class SettingsRepository(private val context: Context, private val json: Json = Json) {
    private object Keys {
        val automatic = booleanPreferencesKey("automatic")
        val vespersHour = intPreferencesKey("vespers_hour")
        val vespersMinute = intPreferencesKey("vespers_minute")
        val target = stringPreferencesKey("wallpaper_target")
        val lastApplied = stringPreferencesKey("last_applied")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map(::decode)
    suspend fun current(): AppSettings = settings.first()
    suspend fun setAutomatic(value: Boolean) = context.dataStore.edit { it[Keys.automatic] = value }
    suspend fun setVespersTime(value: LocalTime) = context.dataStore.edit {
        it[Keys.vespersHour] = value.hour; it[Keys.vespersMinute] = value.minute
    }
    suspend fun setTarget(value: WallpaperTarget) = context.dataStore.edit { it[Keys.target] = value.name }
    suspend fun setLastApplied(value: AppliedWallpaperState) = context.dataStore.edit {
        it[Keys.lastApplied] = json.encodeToString(AppliedWallpaperState.serializer(), value)
    }

    private fun decode(preferences: Preferences): AppSettings = AppSettings(
        automatic = preferences[Keys.automatic] ?: false,
        vespersTime = LocalTime.of(preferences[Keys.vespersHour] ?: 18, preferences[Keys.vespersMinute] ?: 0),
        target = preferences[Keys.target]?.let { runCatching { WallpaperTarget.valueOf(it) }.getOrNull() }
            ?: WallpaperTarget.HOME,
        lastApplied = preferences[Keys.lastApplied]?.let {
            runCatching { json.decodeFromString<AppliedWallpaperState>(it) }.getOrNull()
        },
    )
}
