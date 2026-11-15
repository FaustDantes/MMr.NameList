package com.michalmarcin.namelist.ui.person

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DatesTest {

    @Test
    fun epochDayRoundTripsThroughThePickerMillis() {
        listOf(0L, 19_000L, 20_407L, -1L).forEach {
            assertEquals(it, pickerMillisToEpochDay(epochDayToPickerMillis(it)))
        }
    }

    @Test
    fun pickerMillisInsideADayMapToThatDay() {
        assertEquals(20_407L, pickerMillisToEpochDay(20_407L * 86_400_000 + 86_399_999))
        assertEquals(-1L, pickerMillisToEpochDay(-1))
    }

    @Test
    fun formatShowsTheCalendarDateWhateverTheDeviceTimeZone() {
        val day = LocalDate.of(2026, 11, 15).toEpochDay()
        assertEquals("Nov 15, 2026", formatEpochDay(day, Locale.US))
    }
}
