package com.michalmarcin.namelist.platform

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

/** Web Mercator in unit space: x and y are 0..1 over the whole world (north at y = 0). */
object Mercator {
    private const val MAX_LAT = 85.0

    fun x(lon: Double): Float = ((lon + 180.0) / 360.0).toFloat()

    fun y(lat: Double): Float {
        val phi = Math.toRadians(lat.coerceIn(-MAX_LAT, MAX_LAT))
        return ((1.0 - ln(tan(phi) + 1.0 / cos(phi)) / PI) / 2.0).toFloat()
    }
}

/** Country border rings, already projected: each FloatArray is x0, y0, x1, y1, ... in Mercator unit space. */
class WorldShapes(val rings: List<FloatArray>) {
    val pointCount: Int get() = rings.sumOf { it.size / 2 }
}

/** Reads assets/world_110m.bin (Natural Earth 1:110m, public domain; tools/map/README.md). */
object WorldMapData {
    const val ASSET = "world_110m.bin"

    fun read(input: InputStream): WorldShapes {
        val data = DataInputStream(input.buffered())
        val magic = ByteArray(4).also { data.readFully(it) }
        if (String(magic, Charsets.US_ASCII) != "WMAP") throw IOException("Not a world map file")
        val version = data.readInt()
        if (version != 1) throw IOException("Unsupported world map version $version")
        val rings = List(data.readInt()) {
            val count = data.readInt()
            FloatArray(count * 2).also { ring ->
                for (i in 0 until count) {
                    val lon = data.readFloat().toDouble()
                    val lat = data.readFloat().toDouble()
                    ring[2 * i] = Mercator.x(lon)
                    ring[2 * i + 1] = Mercator.y(lat)
                }
            }
        }
        return WorldShapes(rings)
    }
}
