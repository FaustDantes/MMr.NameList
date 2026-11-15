package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
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
    /** Day of the first meeting (epoch day). Null means "not filled": the repository uses today (F16). */
    val firstMetOn: Long? = null,
    /** More details (F19). Blank text counts as empty. */
    val nativeLanguage: String? = null,
    /** Epoch day of the birth date. */
    val birthDate: Long? = null,
    val education: String? = null,
    val currentWork: String? = null,
    val askAbout: String? = null,
)

class PeopleRepository(
    private val dao: PersonDao,
    private val deletePhoto: (String) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
    private val locale: () -> Locale = Locale::getDefault,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
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

    /** Everyone, "Me" first. */
    fun observeEveryone(): Flow<List<Person>> = dao.observeActive()

    /** People in the archive, the newest first (F18). */
    fun observeArchived(): Flow<List<Person>> = dao.observeArchived()

    /** Moves people to the archive and stamps the time. Returns how many moved. */
    suspend fun archive(ids: Collection<Long>): Int = if (ids.isEmpty()) 0 else dao.archive(ids.toList(), clock())

    /** Brings people back from the archive. */
    suspend fun restore(ids: Collection<Long>): Int = if (ids.isEmpty()) 0 else dao.restore(ids.toList())

    suspend fun getPerson(id: Long): Person? = dao.get(id)

    /**
     * Another person with the same name, ignoring case and extra spaces (F22). Archived people count.
     * "Me" never counts. [exceptId] is the person being edited.
     */
    suspend fun findSameName(name: String, exceptId: Long = 0L): Person? {
        val key = nameKey(name)
        if (key.isEmpty()) return null
        return dao.getAll().firstOrNull { !it.isSelf && it.id != exceptId && nameKey(it.name) == key }
    }

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
            // F16: an empty date is today.
            firstMetOn = draft.firstMetOn ?: epochDay(clock()),
            nativeLanguage = draft.nativeLanguage.clean(),
            birthDate = draft.birthDate,
            education = draft.education.clean(),
            currentWork = draft.currentWork.clean(),
            askAbout = draft.askAbout.clean(),
            // F19: every save from the edit screen stamps the time.
            lastUpdatedAt = clock(),
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
        val kept = existing?.let {
            PersonDraft(
                name, notes, photoFile, tabId, it.metCity, it.metLat, it.metLon, firstMetDay(it),
                it.nativeLanguage, it.birthDate, it.education, it.currentWork, it.askAbout,
            )
        }
        return save(id, kept ?: PersonDraft(name, notes, photoFile, tabId))
    }

    /** Drag and drop result: puts the shown people in this order (F5). */
    suspend fun reorder(idsInNewOrder: List<Long>) = dao.reorder(idsInNewOrder)

    /** Deletes several people and their photo files (M8). Returns how many were deleted. */
    suspend fun deleteMany(ids: Collection<Long>): Int {
        if (ids.isEmpty()) return 0
        val list = ids.toList()
        val photos = dao.photoFiles(list)
        val deleted = dao.deleteByIds(list)
        photos.forEach(deletePhoto)
        return deleted
    }

    /** Moves several people to [tabId]; null removes their tab (M8). */
    suspend fun moveToTab(ids: Collection<Long>, tabId: Long?) {
        if (ids.isNotEmpty()) dao.setTab(ids.toList(), tabId)
    }

    /** Deletes the person and their photo file. "Me" cannot be deleted. */
    suspend fun delete(id: Long): Boolean {
        val photo = dao.get(id)?.photoFile
        val deleted = dao.deleteById(id) > 0
        if (deleted) photo?.let(deletePhoto)
        return deleted
    }

    /** Day a person without a stored date counts from: the day they were added. */
    fun firstMetDay(person: Person): Long = person.firstMetOn ?: epochDay(person.createdAt)

    private fun epochDay(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** F26: the search looks at every text field and at every date, written in several common ways. */
    private fun Person.matches(needle: String): Boolean {
        val texts = listOfNotNull(name, notes, metCity, metCountry, originCountry, nativeLanguage, education, currentWork, askAbout)
        if (texts.any { normalize(it).contains(needle) }) return true
        val days = listOfNotNull(
            firstMetDay(this),
            birthDate,
            lastUpdatedAt?.let(::epochDay),
            archivedAt?.let(::epochDay),
        )
        return days.any { day -> dateTexts(day).any { normalize(it).contains(needle) } }
    }

    private fun dateTexts(epochDay: Long): List<String> {
        val date = java.time.LocalDate.ofEpochDay(epochDay)
        val locale = Locale.getDefault()
        return listOf(
            "yyyy-MM-dd", "dd.MM.yyyy", "d.M.yyyy", "d. M. yyyy", "d/M/yyyy", "M/d/yyyy", "MMMM yyyy", "d MMMM yyyy",
        ).map { java.time.format.DateTimeFormatter.ofPattern(it, locale).format(date) } +
            java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale).format(date)
    }

    companion object {
        private val marks = "\\p{Mn}+".toRegex()

        /** Lower case, no diacritics, trimmed: "  Šimon " -> "simon". */
        /** The same name written with other capitals or extra spaces gives the same key. */
        fun nameKey(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase()

        fun normalize(text: String): String =
            Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marks, "")
    }
}
