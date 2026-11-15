package com.michalmarcin.namelist.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.testutil.SampleData
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class WorldMapTest {

    @get:Rule
    val compose = createComposeRule()

    private val lisbon = MapMarker.of(10, "Ana Alves", lat = 38.72, lon = -9.14)
    private val porto = MapMarker.of(11, "Rui Costa", lat = 41.15, lon = -8.61)
    private val tokyo = MapMarker.of(12, "Yuki Sato", lat = 35.68, lon = 139.69)

    @Test
    fun closeMarkersCluster() {
        val clusters = clusterMarkers(
            listOf(lisbon to Offset(100f, 100f), porto to Offset(110f, 95f), tokyo to Offset(600f, 120f)),
            radiusPx = 50f,
        )
        assertEquals(listOf(2, 1), clusters.map { it.markers.size })
        assertEquals(Offset(105f, 97.5f), clusters[0].position)
    }

    @Test
    fun startViewFitsMarkersAndCoversTheWidth() {
        val (scale, offset) = initialView(listOf(lisbon, tokyo), focus = null, focusZoom = 6f, w = 1000f, h = 2000f)
        assertTrue(scale >= 1f)
        assertTrue("map covers the width", offset.x <= 0f && offset.x + 1000f * scale >= 1000f)
        val (focusScale, _) = initialView(emptyList(), focus = lisbon, focusZoom = 6f, w = 1000f, h = 2000f)
        assertEquals(6f, focusScale)
    }

    @Test
    fun clampKeepsTheMapInView() {
        val o = clampOffset(Offset(500f, 500f), scale = 3f, w = 1000f, h = 2000f)
        assertEquals(0f, o.x) // cannot drag the left edge into view
        assertEquals(0f, o.y)
    }

    @Test
    fun mapScreenShowsInitialsAndOpensPerson() {
        var opened = -1L
        compose.setContent {
            NameListTheme {
                MapScreen(MapUiState(markers = listOf(lisbon, tokyo), isLoading = false), {}, { opened = it })
            }
        }
        compose.onNodeWithText("Map: 2 people").assertIsDisplayed()
        compose.onNodeWithText("AA").assertIsDisplayed()
        compose.onNodeWithTag("marker-12").performClick()
        assertEquals(12L, opened)
    }

    @Test
    fun emptyMapExplainsTheGpsButton() {
        compose.setContent {
            NameListTheme { MapScreen(MapUiState(isLoading = false), {}, {}) }
        }
        compose.onNodeWithTag("mapEmpty").assertIsDisplayed()
    }

    @Test
    fun onlyPeopleWithGpsAndNotMeAreMarkers() {
        val withGps = SampleData.anna.copy(metLat = 49.19, metLon = 16.6)
        assertEquals(withGps.id, withGps.toMarker()?.id)
        assertEquals(null, SampleData.anna.copy(metLat = null).toMarker())
        assertEquals(null, withGps.copy(isSelf = true).toMarker())
    }
}
