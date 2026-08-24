package com.example.liturgicalwallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import com.example.liturgicalwallpaper.settings.WallpaperTarget
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class WallpaperService(context: Context) {
    private val manager = WallpaperManager.getInstance(context)

    open suspend fun apply(file: File, target: WallpaperTarget): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
            var sample = 1
            val maxDimension = 4096
            while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
            val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().also { it.inSampleSize = sample })
                ?: error("Unable to decode image")
            try {
                val flags = WallpaperManager.FLAG_SYSTEM or
                    if (target == WallpaperTarget.HOME_AND_LOCK) WallpaperManager.FLAG_LOCK else 0
                manager.setBitmap(bitmap, null, true, flags)
                Unit
            } finally { bitmap.recycle() }
        }
    }
}
