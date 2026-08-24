package com.example.liturgicalwallpaper.wallpaper

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperMapping {
    fun <T> resolve(color: LiturgicalColor, configured: Map<LiturgicalColor, T>): T? = configured[color]
}

open class WallpaperRepository(private val context: Context) {
    private val directory = File(context.filesDir, "wallpapers")

    open fun fileFor(color: LiturgicalColor): File? = WallpaperMapping.resolve(color,
        directory.listFiles()?.filter(File::isFile)?.associateBy {
            LiturgicalColor.fromWire(it.nameWithoutExtension)
        }.orEmpty())

    suspend fun import(color: LiturgicalColor, uri: Uri): File = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val mime = context.contentResolver.getType(uri)
        val extension = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val temporary = File(directory, "${color.wireName}.tmp")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected image" }
            temporary.outputStream().use { input.copyTo(it) }
        }
        require(temporary.length() > 0) { "Selected image is empty" }
        val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        BitmapFactory.decodeFile(temporary.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selected file is not a supported image" }
        directory.listFiles()?.filter { it.nameWithoutExtension == color.wireName && it != temporary }?.forEach(File::delete)
        val target = File(directory, "${color.wireName}.$extension")
        check(temporary.renameTo(target) || run { temporary.copyTo(target, overwrite = true); temporary.delete() })
        target
    }

    open fun identity(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
