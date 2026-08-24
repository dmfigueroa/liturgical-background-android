package com.example.liturgicalwallpaper.wallpaper

import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallpaperMappingTest {
    @Test fun `each color maps only to its own image`() {
        val configured = LiturgicalColor.entries.associateWith { it.wireName }
        LiturgicalColor.entries.forEach { assertEquals(it.wireName, WallpaperMapping.resolve(it, configured)) }
    }
    @Test fun `unknown uses unknown fallback image`() = assertEquals("fallback", WallpaperMapping.resolve(LiturgicalColor.UNKNOWN, mapOf(LiturgicalColor.UNKNOWN to "fallback")))
    @Test fun `missing normal color does not use fallback`() = assertNull(WallpaperMapping.resolve(LiturgicalColor.RED, mapOf(LiturgicalColor.UNKNOWN to "fallback")))
}
