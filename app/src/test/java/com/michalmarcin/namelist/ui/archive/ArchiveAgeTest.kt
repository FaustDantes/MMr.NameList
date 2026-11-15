package com.michalmarcin.namelist.ui.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveAgeTest {

    private val day = 86_400_000L

    private fun age(days: Long, extra: Long = 0) = ArchiveAge.of(archivedAt = 1_000, now = 1_000 + days * day + extra)

    @Test
    fun underADayIsLessThanADay() {
        assertEquals(ArchiveAge.LessThanDay, age(0, extra = day - 1))
    }

    @Test
    fun aClockSetBackCountsAsLessThanADay() {
        assertEquals(ArchiveAge.LessThanDay, ArchiveAge.of(archivedAt = 5_000, now = 1_000))
    }

    @Test
    fun daysUpToFiftyNine() {
        assertEquals(ArchiveAge.Days(1), age(1))
        assertEquals(ArchiveAge.Days(59), age(59))
    }

    @Test
    fun monthsFromSixtyDaysUpToTwoYears() {
        assertEquals(ArchiveAge.Months(2), age(60))
        assertEquals(ArchiveAge.Months(6), age(190))
        assertEquals(ArchiveAge.Months(24), age(729))
    }

    @Test
    fun yearsFromTwoYears() {
        assertEquals(ArchiveAge.Years(2), age(730))
        assertEquals(ArchiveAge.Years(5), age(5 * 365 + 40))
    }
}
