package com.michalmarcin.namelist.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michalmarcin.namelist.platform.Mercator
import com.michalmarcin.namelist.platform.WorldShapes
import com.michalmarcin.namelist.ui.components.AvatarColors
import com.michalmarcin.namelist.ui.components.initials
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Country borders, loaded once in MainActivity. Null until loaded (and in most tests). */
val LocalWorldShapes = compositionLocalOf<WorldShapes?> { null }

/** A person on the map, position in Mercator unit space. */
data class MapMarker(val id: Long, val name: String, val x: Float, val y: Float) {
    companion object {
        fun of(id: Long, name: String, lat: Double, lon: Double) = MapMarker(id, name, Mercator.x(lon), Mercator.y(lat))
    }
}

/** One circle on screen: one person, or several close together. */
data class MarkerCluster(val markers: List<MapMarker>, val position: Offset)

/**
 * Groups markers whose screen positions are closer than [radiusPx], so circles do not cover each other.
 * Greedy: each marker joins the first cluster near it. Fine for hundreds of people.
 */
fun clusterMarkers(positioned: List<Pair<MapMarker, Offset>>, radiusPx: Float): List<MarkerCluster> {
    val clusters = mutableListOf<MutableList<Pair<MapMarker, Offset>>>()
    for (item in positioned) {
        val near = clusters.firstOrNull { (it.first().second - item.second).getDistance() < radiusPx }
        if (near != null) near += item else clusters += mutableListOf(item)
    }
    return clusters.map { group ->
        val center = Offset(
            group.map { it.second.x }.average().toFloat(),
            group.map { it.second.y }.average().toFloat(),
        )
        MarkerCluster(group.map { it.first }, center)
    }
}

private const val MAX_ZOOM = 40f

/**
 * Simple world map (M9): country borders (Natural Earth 1:110m) and people as circles with initials.
 * Pinch to zoom, drag to move, double tap to zoom in. Works offline; nothing is downloaded.
 *
 * [focus]: start centered on this marker at [focusZoom]. Else the view fits all markers.
 */
@Composable
fun WorldMap(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    focus: MapMarker? = null,
    focusZoom: Float = 6f,
    interactive: Boolean = true,
    onMarkerClick: (Long) -> Unit = {},
) {
    val shapes = LocalWorldShapes.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val ocean = if (dark) Color(0xFF0B1520) else Color(0xFFEAF4FF)
    val land = if (dark) scheme.surfaceContainerHigh else Color.White
    val borders = if (dark) Color(0xFF4A525A) else Color(0xFF9AA4AE)
    val path = remember(shapes) { shapes?.toPath() ?: Path() }
    val density = LocalDensity.current
    val markerRadius = with(density) { 18.dp.toPx() }

    BoxWithConstraints(
        modifier
            .clipToBounds()
            .background(ocean)
            .testTag("worldMap"),
    ) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val start = remember(w, h, focus) { initialView(markers, focus, focusZoom, w, h) }
        var scale by remember(start) { mutableFloatStateOf(start.first) }
        var offset by remember(start) { mutableStateOf(start.second) }

        fun zoomAt(point: Offset, factor: Float, pan: Offset = Offset.Zero) {
            val newScale = (scale * factor).coerceIn(1f, MAX_ZOOM)
            val ratio = newScale / scale
            offset = clampOffset((offset - point) * ratio + point + pan, newScale, w, h)
            scale = newScale
        }

        val gestures = if (interactive) {
            Modifier
                .pointerInput(w, h) {
                    detectTransformGestures { centroid, pan, zoom, _ -> zoomAt(centroid, zoom, pan) }
                }
                .pointerInput(w, h) {
                    detectTapGestures(onDoubleTap = { zoomAt(it, 2f) })
                }
        } else {
            Modifier
        }

        Canvas(Modifier.fillMaxSize().then(gestures)) {
            val world = w * scale
            withTransform({
                translate(offset.x, offset.y)
                scale(world, world, pivot = Offset.Zero)
            }) {
                drawPath(path, land)
                drawPath(path, borders, style = Stroke(width = 1f / world))
            }
        }

        val world = w * scale
        val onScreen = markers
            .map { it to Offset(offset.x + it.x * world, offset.y + it.y * world) }
            .filter { (_, p) -> p.x in -markerRadius..(w + markerRadius) && p.y in -markerRadius..(h + markerRadius) }
        clusterMarkers(onScreen, markerRadius * 1.8f).forEach { cluster ->
            val position = Modifier.offset {
                IntOffset((cluster.position.x - markerRadius).roundToInt(), (cluster.position.y - markerRadius).roundToInt())
            }
            if (cluster.markers.size == 1) {
                val marker = cluster.markers.first()
                InitialsCircle(
                    name = marker.name,
                    dark = dark,
                    modifier = position
                        .testTag("marker-${marker.id}")
                        .then(if (interactive) Modifier.clickable { onMarkerClick(marker.id) } else Modifier),
                )
            } else {
                CountCircle(
                    count = cluster.markers.size,
                    modifier = position
                        .testTag("cluster-${cluster.markers.size}")
                        .then(if (interactive) Modifier.clickable { zoomAt(cluster.position, 2.5f) } else Modifier),
                )
            }
        }
    }
}

@Composable
private fun InitialsCircle(name: String, dark: Boolean, modifier: Modifier = Modifier) {
    val (background, content) = AvatarColors.forName(name, dark)
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(3.dp, CircleShape)
            .background(background, CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CountCircle(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(3.dp, CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun WorldShapes.toPath(): Path = Path().apply {
    rings.forEach { ring ->
        moveTo(ring[0], ring[1])
        var i = 2
        while (i < ring.size) {
            lineTo(ring[i], ring[i + 1])
            i += 2
        }
        close()
    }
}

/** The map always covers the width; vertically it is centered when it is smaller than the view. */
internal fun clampOffset(offset: Offset, scale: Float, w: Float, h: Float): Offset {
    val world = w * scale
    val x = if (world <= w) (w - world) / 2 else offset.x.coerceIn(w - world, 0f)
    val y = if (world <= h) (h - world) / 2 else offset.y.coerceIn(h - world, 0f)
    return Offset(x, y)
}

/** Start view: centered on [focus], or fitted to all markers, or the whole world. */
internal fun initialView(markers: List<MapMarker>, focus: MapMarker?, focusZoom: Float, w: Float, h: Float): Pair<Float, Offset> {
    val (scale, cx, cy) = when {
        focus != null -> Triple(focusZoom, focus.x, focus.y)
        markers.isNotEmpty() -> {
            val minX = markers.minOf { it.x }
            val maxX = markers.maxOf { it.x }
            val minY = markers.minOf { it.y }
            val maxY = markers.maxOf { it.y }
            val spanX = max(maxX - minX, 0.02f) * 1.6f
            val spanY = max(maxY - minY, 0.02f) * 1.6f
            Triple(min(1f / spanX, h / (w * spanY)).coerceIn(1f, 8f), (minX + maxX) / 2, (minY + maxY) / 2)
        }
        else -> Triple(1f, 0.5f, 0.45f)
    }
    val zoom = scale.coerceIn(1f, MAX_ZOOM)
    val world = w * zoom
    return zoom to clampOffset(Offset(w / 2 - cx * world, h / 2 - cy * world), zoom, w, h)
}
