package com.michalmarcin.namelist.testutil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.michalmarcin.namelist.ui.components.LocalPhotoDirectory
import java.io.File

/**
 * Generated "photos" (abstract landscapes, no real people) for screenshots.
 * Coil loads images on a background thread; the preview handler makes it synchronous.
 */
object FakePhotos {

    private val palettes = mapOf(
        "anna.jpg" to intArrayOf(0xFFF6B48F.toInt(), 0xFF7A4E8C.toInt(), 0xFF2E5E4E.toInt()),
        "jana.jpg" to intArrayOf(0xFF8EC5FC.toInt(), 0xFFE0C3FC.toInt(), 0xFF3B6E8F.toInt()),
    )

    private val transparent: Bitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }

    fun bitmap(name: String): Bitmap? {
        val colors = palettes[name] ?: return null
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val sky = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, size.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), sky)
        canvas.drawCircle(size * 0.7f, size * 0.35f, size * 0.12f, Paint().apply { color = 0xFFFFF4D6.toInt() })
        val hill = Path().apply {
            moveTo(0f, size * 0.75f)
            quadTo(size * 0.35f, size * 0.5f, size * 0.7f, size * 0.72f)
            quadTo(size * 0.85f, size * 0.8f, size.toFloat(), size * 0.68f)
            lineTo(size.toFloat(), size.toFloat())
            lineTo(0f, size.toFloat())
            close()
        }
        canvas.drawPath(hill, Paint().apply { color = colors[2]; isAntiAlias = true })
        return bitmap
    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalInspectionMode provides true,
            LocalPhotoDirectory provides File("photos"),
            LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { request ->
                // Unknown file: a transparent image, so the initials under it stay visible (as on a phone).
                ((request.data as? File)?.let { bitmap(it.name) } ?: transparent).asImage()
            },
        ) { content() }
    }
}
