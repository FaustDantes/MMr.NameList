package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

enum class SortMode { MANUAL, A_TO_Z }

/** What the edit screen saves. */
data class PersonDraft(
    val name: String,
    val notes: String = "",
    val photoFile: String? = null,
    val tabId: Long? = null,
    /** "City, state" as the user typed it (F10). */
    val metPlace: String? = null,
    val metLat: Double? = null,
    val metLon: Double? = null,
)

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
    suspend fun save(id: Long, draft: PersonDraft): Long {
        require(draft.name.isNotBlank()) { "Name must not be blank" }
        val existing = if (id == 0L) null else dao.get(id)
        val place = draft.metPlace?.trim()?.takeIf { it.isNotEmpty() }
        // Coordinates are stored as a pair or not at all.
        val hasCoordinates = draft.metLat != null && draft.metLon != null
        val base = existing ?: Person(name = "", sortIndex = dao.maxSortIndex() + 1, createdAt = clock())
        val person = base.copy(
            name = draft.name.trim(),
            notes = draft.notes.trim(),
            photoFile = draft.photoFile,
            tabId = draft.tabId,
            metCity = place,
            metLat = if (hasCoordinates) draft.metLat else null,
            metLon = if (hasCoordinates) draft.metLon else null,
        )
        return if (existing == null) {
            dao.insert(person)
        } else {
            dao.update(person)
            existing.photoFile?.takeIf { it != draft.photoFile }?.let(deletePhoto)
            existing.id
        }
    }

    /** Short form used by tests and simple callers. Keeps the place of meeting of an existing person. */
    suspend fun save(id: Long, name: String, notes: String, photoFile: String? = null, tabId: Long? = null): Long {
        val existing = if (id == 0L) null else dao.get(id)
        return save(
            id,
            PersonDraft(name, notes, photoFile, tabId, existing?.metCity, existing?.metLat, existing?.metLon),
        )
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
