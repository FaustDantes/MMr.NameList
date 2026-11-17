package com.michalmarcin.namelist.platform.widget

import com.michalmarcin.namelist.data.db.Person

/** The three widget layouts (F20). The launcher resizes one widget between them. */
enum class WidgetSize {
    /** 1 x 1: the add button only. */
    ICON,

    /** 4 x 1: add button and the newest person. */
    STRIP,

    /** 4 x 2: add button, hint and the names of the three newest people (ADR-038). */
    CARD,
    ;

    companion object {
        /** Picks the layout from the size the launcher gives the widget, in dp. */
        fun forDp(widthDp: Int, heightDp: Int): WidgetSize = when {
            heightDp >= CARD_MIN_HEIGHT_DP -> CARD
            widthDp >= STRIP_MIN_WIDTH_DP -> STRIP
            else -> ICON
        }

        private const val CARD_MIN_HEIGHT_DP = 110
        private const val STRIP_MIN_WIDTH_DP = 110
    }
}

/** What the widget shows, without any Android type. */
data class WidgetContent(
    /** Name of the newest person, or null when there is nobody. */
    val lastAdded: String?,
    /** Names of up to [MAX_RECENT] newest people, newest first. */
    val recentNames: List<String>,
) {
    companion object {
        const val MAX_RECENT = 3

        /** [people] must not contain "Me" or archived people. Newest = highest `createdAt`, then `id`. */
        fun from(people: List<Person>): WidgetContent {
            val newest = people
                .filter { !it.isSelf && it.archivedAt == null }
                .sortedWith(compareByDescending<Person> { it.createdAt }.thenByDescending { it.id })
                .take(MAX_RECENT)
            return WidgetContent(
                lastAdded = newest.firstOrNull()?.name?.trim()?.takeIf { it.isNotEmpty() },
                recentNames = newest.map { it.name.trim() },
            )
        }
    }
}
