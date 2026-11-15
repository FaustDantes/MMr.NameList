package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

enum class SortMode { MANUAL, A_TO_Z }

class PeopleRepository(
    private val dao: PersonDao,
    private val deletePhoto: (String) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
    private val locale: () -> Locale = Locale::getDefault,
) {

    /**
     * People except "Me", filtered by [query] and sorted by [sort].
     * Search and sort run in Kotlin, not SQL: SQLite LIKE and NOCASE ignore case only for ASCII,
     * and names such as "Šimon" or "Čeněk" must match "simon" and sort by the user's language (ADR-011).
     */
    fun observePeople(query: String, sort: SortMode, tabId: Long? = null): Flow<List<Person>> =
        dao.observeOthers().map { people ->
            val needle = normalize(query)
            // Search is global (F7): a search query ignores the selected tab.
            val found = when {
                needle.isNotEmpty() -> people.filter { it.matches(needle) }
                tabId != null -> people.filter { it.tabId == tabId }
                else -> people
            }
            when (sort) {
                SortMode.MANUAL -> found
                SortMode.A_TO_Z -> {
                    val collator = Collator.getInstance(locale()).apply { strength = Collator.SECONDARY }
                    found.sortedWith { a, b -> collator.compare(a.name, b.name) }
                }
            }
        }

    fun observePerson(id: Long): Flow<Person?> = dao.observe(id)

    suspend fun getPerson(id: Long): Person? = dao.get(id)

    suspend fun getSelf(): Person? = dao.getSelf()

    /**
     * Inserts a new person when [id] is 0, else updates. Returns the id.
     * A replaced or removed photo file is deleted from storage.
     */
    suspend fun save(id: Long, name: String, notes: String, photoFile: String? = null, tabId: Long? = null): Long {
        require(name.isNotBlank()) { "Name must not be blank" }
        val existing = if (id == 0L) null else dao.get(id)
        return if (existing == null) {
            dao.insert(
                Person(
                    name = name.trim(),
                    notes = notes.trim(),
                    photoFile = photoFile,
                    tabId = tabId,
                    sortIndex = dao.maxSortIndex() + 1,
                    createdAt = clock(),
                ),
            )
        } else {
            dao.update(existing.copy(name = name.trim(), notes = notes.trim(), photoFile = photoFile, tabId = tabId))
            existing.photoFile?.takeIf { it != photoFile }?.let(deletePhoto)
            existing.id
        }
    }

    /** Drag and drop result: puts the shown people in this order (F5). */
    suspend fun reorder(idsInNewOrder: List<Long>) = dao.reorder(idsInNewOrder)

    /** Deletes the person and their photo file. "Me" cannot be deleted. */
    suspend fun delete(id: Long): Boolean {
        val photo = dao.get(id)?.photoFile
        val deleted = dao.deleteById(id) > 0
        if (deleted) photo?.let(deletePhoto)
        return deleted
    }

    private fun Person.matches(needle: String): Boolean =
        listOfNotNull(name, notes, metCity).any { normalize(it).contains(needle) }

    companion object {
        private val marks = "\\p{Mn}+".toRegex()

        /** Lower case, no diacritics, trimmed: "  Šimon " -> "simon". */
        fun normalize(text: String): String =
            Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marks, "")
    }
}
