package com.michalmarcin.namelist.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Keeps person photos in app-private storage (docs/architecture.md, section 8).
 * Each photo is copied, turned upright from its EXIF data, and scaled to [MAX_SIDE] px.
 * Only the file name goes into the database.
 */
class PhotoStore(private val context: Context) {

    val directory: File = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(name: String): File = File(directory, name)

    /** Copies the image at [uri] into the store. Returns the new file name. */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeUpright(uri) ?: throw IOException("Cannot read image $uri")
        val scaled = scaleDown(bitmap, MAX_SIDE)
        val name = "${UUID.randomUUID()}.jpg"
        file(name).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        name
    }

    fun delete(name: String?) {
        if (!name.isNullOrBlank()) file(name).delete()
    }

    /** A content URI the camera app can write one photo to. */
    fun newCaptureUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val target = File(dir, "capture.jpg").apply { delete() }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    }

    private fun decodeUpright(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Decode at no less than MAX_SIDE to keep quality, but avoid loading a full 50 MP image.
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = orientationMatrix(orientation) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    companion object {
        const val MAX_SIDE = 1024
        private const val JPEG_QUALITY = 88

        fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
            val longest = max(bitmap.width, bitmap.height)
            if (longest <= maxSide) return bitmap
            val ratio = maxSide.toFloat() / longest
            return Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
                (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
                true,
            )
        }

        internal fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { postRotate(270f); postScale(-1f, 1f) }
            else -> null
        }
    }
}
