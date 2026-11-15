package com.michalmarcin.namelist.ui.person

/** The first [maxWords] words of the first line of a Conversation Spark, then "…" when there is more (F26). */
fun sparkPreview(text: String?, maxWords: Int = 5): String? {
    val words = text?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    if (words.isEmpty()) return null
    return if (words.size <= maxWords) words.joinToString(" ") else words.take(maxWords).joinToString(" ") + "…"
}
