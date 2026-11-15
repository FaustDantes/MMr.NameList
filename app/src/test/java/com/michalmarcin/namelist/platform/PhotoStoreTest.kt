package com.michalmarcin.namelist.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PhotoStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = PhotoStore(context)

    private fun jpeg(width: Int, height: Int, orientation: Int? = null): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(0, 105, 92)) }
        val file = File(context.cacheDir, "source-${width}x$height-$orientation.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (orientation != null) {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return Uri.fromFile(file)
    }

    private fun size(name: String): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(store.file(name).path, options)
        return options.outWidth to options.outHeight
    }

    @Test
    fun largePhotoIsScaledTo1024AndKeepsAspect() = runBlocking {
        val name = store.import(jpeg(3000, 2000))
        assertTrue(store.file(name).exists())
        assertEquals(1024 to 683, size(name))
    }

    @Test
    fun smallPhotoIsNotEnlarged() = runBlocking {
        assertEquals(400 to 300, size(store.import(jpeg(400, 300))))
    }

    @Test
    fun exifRotationIsApplied() = runBlocking {
        val name = store.import(jpeg(1600, 1200, ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(768 to 1024, size(name))
    }

    @Test
    fun deleteRemovesTheFile() = runBlocking {
        val name = store.import(jpeg(200, 200))
        store.delete(name)
        assertFalse(store.file(name).exists())
    }

    @Test
    fun scaleDownMath() {
        val bitmap = Bitmap.createBitmap(2048, 512, Bitmap.Config.ARGB_8888)
        val scaled = PhotoStore.scaleDown(bitmap, 1024)
        assertEquals(1024, scaled.width)
        assertEquals(256, scaled.height)
    }
}
