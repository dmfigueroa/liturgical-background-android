package com.example.liturgicalwallpaper.domain

import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import org.junit.Assert.assertEquals
import org.junit.Test

class LiturgicalColorTest {
    @Test fun `all supported wire colors parse`() {
        listOf("green","white","red","violet","rose","unknown").forEach { value ->
            assertEquals(value, LiturgicalColor.fromWire(value).wireName)
        }
    }
    @Test fun `unexpected server color normalizes to unknown`() = assertEquals(LiturgicalColor.UNKNOWN, LiturgicalColor.fromWire("gold"))
    @Test fun `black normalizes to unknown`() = assertEquals(LiturgicalColor.UNKNOWN, LiturgicalColor.fromWire("black"))
}
