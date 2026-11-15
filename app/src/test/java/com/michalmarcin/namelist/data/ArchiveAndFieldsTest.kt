package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.PersonDraft
import com.michalmarcin.namelist.data.repo.SortMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.util.Locale

/** F18 archive and F19 more details. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ArchiveAndFieldsTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PeopleRepository
    private var now = 1_000_000L

    @Before
    fun setUp() {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        repo = PeopleRepository(db.personDao(), clock = { now }, locale = { Locale.ENGLISH }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDown() = db.close()

    private fun listed() = runBlocking { repo.observePeople("", SortMode.MANUAL).first().map { it.name } }

    private fun mapped() = runBlocking { repo.observeEveryone().first().filter { !it.isSelf }.map { it.name } }

    private fun archived() = runBlocking { repo.observeArchived().first().map { it.name } }

    private fun add(name: String) = runBlocking { repo.save(0, name, "") }

    // ---- Archive ----

    @Test
    fun archivedPeopleLeaveTheListAndTheMapAndWaitInTheArchive() = runBlocking {
        val anna = add("Anna")
        add("Peter")
        assertEquals(1, repo.archive(listOf(anna)))
        assertEquals(listOf("Peter"), listed())
        assertEquals(listOf("Peter"), mapped())
        assertEquals(listOf("Anna"), archived())
    }

    @Test
    fun archiveStampsTheTimeAndKeepsTheFirstStamp() = runBlocking {
        val anna = add("Anna")
        now = 5_000
        repo.archive(listOf(anna))
        now = 9_000
        assertEquals("already archived: nothing changes", 0, repo.archive(listOf(anna)))
        assertEquals(5_000L, repo.getPerson(anna)?.archivedAt)
    }

    @Test
    fun restoreBringsPeopleBackWithTheirDataAndOrder() = runBlocking {
        val anna = add("Anna")
        val peter = add("Peter")
        repo.archive(listOf(anna))
        assertEquals(1, repo.restore(listOf(anna)))
        assertNull(repo.getPerson(anna)?.archivedAt)
        assertEquals(listOf("Anna", "Peter"), listed())
        assertTrue(archived().isEmpty())
        assertEquals(peter, repo.getPerson(peter)?.id)
    }

    @Test
    fun meCannotBeArchived() = runBlocking {
        val me = repo.getSelf()!!.id
        assertEquals(0, repo.archive(listOf(me)))
        assertTrue(repo.observeEveryone().first().any { it.isSelf })
    }

    @Test
    fun emptyArchiveRequestChangesNothing() = runBlocking {
        assertEquals(0, repo.archive(emptyList()))
        assertEquals(0, repo.restore(emptyList()))
    }

    @Test
    fun anArchivedPersonCanBeDeleted() = runBlocking {
        val anna = add("Anna")
        repo.archive(listOf(anna))
        assertEquals(1, repo.deleteMany(listOf(anna)))
        assertTrue(archived().isEmpty())
    }

    @Test
    fun searchDoesNotFindArchivedPeople() = runBlocking {
        val anna = add("Anna")
        repo.archive(listOf(anna))
        assertTrue(repo.observePeople("anna", SortMode.MANUAL).first().isEmpty())
    }

    // ---- More details ----

    @Test
    fun everySaveStampsTheTimeOfTheLastUpdate() = runBlocking {
        now = 5_000
        val id = repo.save(0, PersonDraft("Anna"))
        assertEquals(5_000L, repo.getPerson(id)?.lastUpdatedAt)
        now = 9_000
        repo.save(id, PersonDraft("Anna N."))
        assertEquals(9_000L, repo.getPerson(id)?.lastUpdatedAt)
        now = 12_000
        repo.save(id, "Anna N.", "a note") // the short form saves too
        assertEquals(12_000L, repo.getPerson(id)?.lastUpdatedAt)
    }

    @Test
    fun movingOrArchivingIsNotASaveAndKeepsTheStamp() = runBlocking {
        now = 5_000
        val id = repo.save(0, PersonDraft("Anna"))
        now = 9_000
        repo.archive(listOf(id))
        repo.moveToTab(listOf(id), null)
        assertEquals(5_000L, repo.getPerson(id)?.lastUpdatedAt)
    }

    @Test
    fun moreDetailsAreSavedTrimmedAndBlankMeansEmpty() = runBlocking {
        val id = repo.save(
            0,
            PersonDraft(
                name = "Anna",
                nativeLanguage = " Slovak ",
                birthDate = 7_000,
                education = "  ",
                currentWork = "Designer at a studio",
                askAbout = "Her trip to Japan",
            ),
        )
        val anna = repo.getPerson(id)!!
        assertEquals("Slovak", anna.nativeLanguage)
        assertEquals(7_000L, anna.birthDate)
        assertNull("blank text is stored as empty", anna.education)
        assertEquals("Designer at a studio", anna.currentWork)
        assertEquals("Her trip to Japan", anna.askAbout)
    }

    @Test
    fun theShortSaveKeepsTheMoreDetails() = runBlocking {
        val id = repo.save(0, PersonDraft("Anna", nativeLanguage = "Czech", birthDate = 7_000, education = "MSc", currentWork = "Nurse", askAbout = "Her dog"))
        repo.save(id, "Anna N.", "note")
        val anna = repo.getPerson(id)!!
        assertEquals(listOf("Czech", "MSc", "Nurse", "Her dog"), listOf(anna.nativeLanguage, anna.education, anna.currentWork, anna.askAbout))
        assertEquals(7_000L, anna.birthDate)
    }

    @Test
    fun searchFindsTheNewTextFields() = runBlocking {
        repo.save(0, PersonDraft("Anna", education = "Charles University", currentWork = "Nurse"))
        repo.save(0, PersonDraft("Peter", nativeLanguage = "Hungarian", askAbout = "Badminton league"))
        fun find(q: String) = runBlocking { repo.observePeople(q, SortMode.MANUAL).first().map { it.name } }
        assertEquals(listOf("Anna"), find("charles"))
        assertEquals(listOf("Anna"), find("NURSE"))
        assertEquals(listOf("Peter"), find("hungar"))
        assertEquals(listOf("Peter"), find("league"))
        assertFalse(find("zzz").isNotEmpty())
    }
}
