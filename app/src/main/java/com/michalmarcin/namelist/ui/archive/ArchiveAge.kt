package com.michalmarcin.namelist.ui.archive

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.michalmarcin.namelist.R

private const val DAY_MILLIS = 86_400_000L

/** How long a person has been in the archive, in the unit that reads best (F18). */
sealed interface ArchiveAge {
    data object LessThanDay : ArchiveAge
    data class Days(val count: Int) : ArchiveAge
    data class Months(val count: Int) : ArchiveAge
    data class Years(val count: Int) : ArchiveAge

    companion object {
        /** Under a day, then days up to 59, then months up to 23, then years. A clock set back counts as under a day. */
        fun of(archivedAt: Long, now: Long): ArchiveAge {
            val days = ((now - archivedAt) / DAY_MILLIS).coerceAtLeast(0)
            return when {
                days < 1 -> LessThanDay
                days < 60 -> Days(days.toInt())
                days < 730 -> Months((days / 30).toInt())
                else -> Years((days / 365).toInt())
            }
        }
    }
}

@Composable
fun ArchiveAge.text(): String = when (this) {
    ArchiveAge.LessThanDay -> stringResource(R.string.age_less_than_day)
    is ArchiveAge.Days -> pluralStringResource(R.plurals.age_days, count, count)
    is ArchiveAge.Months -> pluralStringResource(R.plurals.age_months, count, count)
    is ArchiveAge.Years -> pluralStringResource(R.plurals.age_years, count, count)
}
