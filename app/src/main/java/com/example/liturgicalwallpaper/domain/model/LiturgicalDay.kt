package com.example.liturgicalwallpaper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Celebration(val rank: String, val names: List<String>)

@Serializable
data class Evening(val transitionsToNextDay: Boolean, val reason: String? = null)

@Serializable
data class LiturgicalDay(
    val date: String,
    val season: String,
    val celebration: Celebration,
    val primaryColor: LiturgicalColor,
    val alternativeColors: List<LiturgicalColor> = emptyList(),
    val sourceColorLabel: String = primaryColor.displayName,
    val evening: Evening,
)
