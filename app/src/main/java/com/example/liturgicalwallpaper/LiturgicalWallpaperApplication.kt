package com.example.liturgicalwallpaper

import android.app.Application

class LiturgicalWallpaperApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}

val android.content.Context.appGraph: AppGraph
    get() = (applicationContext as LiturgicalWallpaperApplication).graph
