package com.example.liturgicalwallpaper.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LiturgicalColor {
    @SerialName("green") GREEN,
    @SerialName("white") WHITE,
    @SerialName("red") RED,
    @SerialName("violet") VIOLET,
    @SerialName("rose") ROSE,
    @SerialName("unknown") UNKNOWN;

    val wireName: String get() = name.lowercase()
    val displayName: String get() = wireName.replaceFirstChar(Char::uppercase)

    companion object {
        fun fromWire(value: String): LiturgicalColor = entries.firstOrNull {
            it.wireName == value.lowercase()
        } ?: UNKNOWN
    }
}
