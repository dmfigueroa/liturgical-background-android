package com.example.liturgicalwallpaper.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun appTitleIsVisible() { compose.onNodeWithText("Liturgical Wallpaper").assertIsDisplayed() }
}
