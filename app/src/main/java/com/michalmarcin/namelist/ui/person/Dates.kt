package com.michalmarcin.namelist.ui.person

import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

private const val DAY_MILLIS = 86_400_000L

/** "15 Nov 2026" in the user's language. The epoch day is a calendar date, so the formatter uses UTC. */
fun formatEpochDay(epochDay: Long, locale: Locale = Locale.getDefault()): String {
    val format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return format.format(Date(epochDay * DAY_MILLIS))
}

/** The date picker works in UTC milliseconds at midnight. */
fun epochDayToPickerMillis(epochDay: Long): Long = epochDay * DAY_MILLIS

fun pickerMillisToEpochDay(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

