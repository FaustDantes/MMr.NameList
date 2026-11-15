package com.michalmarcin.namelist.ui.person

import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private const val DAY_MILLIS = 86_400_000L

/** "15 Nov 2026" in the user's language. The epoch day is a calendar date, so the formatter uses UTC. */
fun formatEpochDay(epochDay: Long, locale: Locale = Locale.getDefault()): String {
    val format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return format.format(Date(epochDay * DAY_MILLIS))
}

/** Date and time of a timestamp in the device time zone and language, e.g. "15 Nov 2026, 18:57". */
fun formatDateTime(millis: Long, locale: Locale = Locale.getDefault()): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(millis))

/** The date picker works in UTC milliseconds at midnight. */
fun epochDayToPickerMillis(epochDay: Long): Long = epochDay * DAY_MILLIS

fun pickerMillisToEpochDay(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** The calendar day of a timestamp in the device time zone, as an epoch day. */
fun millisToEpochDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

