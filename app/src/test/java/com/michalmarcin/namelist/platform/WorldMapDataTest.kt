package com.michalmarcin.namelist.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WorldMapDataTest {

    @Test
    fun mercatorKnownPoints() {
        assertEquals(0f, Mercator.x(-180.0), 1e-6f)
        assertEquals(0.5f, Mercator.x(0.0), 1e-6f)
        assertEquals(1f, Mercator.x(180.0), 1e-6f)
        assertEquals(0.5f, Mercator.y(0.0), 1e-6f)
        assertTrue("north is up", Mercator.y(60.0) < Mercator.y(0.0))
        assertEquals(Mercator.y(85.0), Mercator.y(89.9), 1e-6f) // clamped near the poles
    }

    @Test
    fun bundledBordersLoad() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shapes = context.assets.open(WorldMapData.ASSET).use(WorldMapData::read)
        assertTrue("rings: ${shapes.rings.size}", shapes.rings.size > 250)
        assertTrue("points: ${shapes.pointCount}", shapes.pointCount > 9_000)
        // Every point is inside the projected world, give or take the copies across the 180 degree line.
        shapes.rings.forEach { ring ->
            for (i in ring.indices step 2) {
                assertTrue(ring[i] in -1f..2f)
                assertTrue(ring[i + 1] in 0f..1f)
            }
        }
    }

    @Test(expected = IOException::class)
    fun otherFileIsRejected() {
        WorldMapData.read(ByteArrayInputStream("NOPE....".toByteArray()))
    }
}
