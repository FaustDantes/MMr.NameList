package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.PersonDraft
import com.michalmarcin.namelist.data.repo.SortMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PeopleRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PeopleRepository
    private val deletedPhotos = mutableListOf<String>()

    @Before
    fun setUp() {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        repo = PeopleRepository(
            db.personDao(),
            deletePhoto = { deletedPhotos += it },
            clock = { 1_000L },
            locale = { Locale.ENGLISH },
        )
    }

    @After
    fun tearDown() = db.close()

    private fun names(query: String = "", sort: SortMode = SortMode.MANUAL) =
        runBlocking { repo.observePeople(query, sort).first().map { it.name } }

    @Test
    fun meExistsAndIsNotListed() = runBlocking {
        val me = repo.getSelf()
        assertNotNull(me)
        assertEquals("Me", me?.name)
        assertEquals(emptyList<String>(), names())
    }

    @Test
    fun meCannotBeDeleted() = runBlocking {
        val me = requireNotNull(repo.getSelf())
        assertFalse(repo.delete(me.id))
        assertNotNull(repo.getSelf())
    }

    @Test
    fun addEditDelete() = runBlocking {
        val id = repo.save(0, "  Anna  ", " notes ")
        val saved = requireNotNull(repo.getPerson(id))
        assertEquals("Anna", saved.name)
        assertEquals("notes", saved.notes)
        assertEquals(1_000L, saved.createdAt)

        repo.save(id, "Anna Nováková", "")
        assertEquals("Anna Nováková", repo.getPerson(id)?.name)

        assertTrue(repo.delete(id))
        assertNull(repo.getPerson(id))
    }

    @Test
    fun newPeopleGoToTheEndInManualOrder() {
        runBlocking {
            repo.save(0, "Zoe", "")
            repo.save(0, "Adam", "")
            repo.save(0, "Mia", "")
        }
        assertEquals(listOf("Zoe", "Adam", "Mia"), names())
    }

    @Test
    fun aToZUsesLanguageRulesNotCodePoints() {
        runBlocking {
            listOf("Čeněk", "bob", "Adam", "Cyril", "anna").forEach { repo.save(0, it, "") }
        }
        assertEquals(listOf("Adam", "anna", "bob", "Čeněk", "Cyril"), names(sort = SortMode.A_TO_Z))
    }

    @Test
    fun searchIgnoresCaseAndDiacritics() {
        runBlocking {
            repo.save(0, "Šimon Dvořák", "")
            repo.save(0, "Anna", "met in Brno")
            repo.save(0, "Peter", "")
        }
        assertEquals(listOf("Šimon Dvořák"), names("simon"))
        assertEquals(listOf("Šimon Dvořák"), names("DVORAK"))
        assertEquals(listOf("Anna"), names("brno"))
        assertEquals(emptyList<String>(), names("xyz"))
    }

    @Test
    fun searchPatternCharactersAreLiteral() {
        runBlocking {
            repo.save(0, "100% Anna", "")
            repo.save(0, "Peter", "")
        }
        assertEquals(listOf("100% Anna"), names("%"))
        assertEquals(emptyList<String>(), names("_"))
    }

    @Test
    fun replacingAPhotoDeletesTheOldFile() = runBlocking {
        val id = repo.save(0, "Anna", "", photoFile = "a.jpg")
        repo.save(id, "Anna", "", photoFile = "b.jpg")
        assertEquals("b.jpg", repo.getPerson(id)?.photoFile)
        assertEquals(listOf("a.jpg"), deletedPhotos)
    }

    @Test
    fun keepingThePhotoDeletesNothing() = runBlocking {
        val id = repo.save(0, "Anna", "", photoFile = "a.jpg")
        repo.save(id, "Anna N.", "", photoFile = "a.jpg")
        assertEquals(emptyList<String>(), deletedPhotos)
    }

    @Test
    fun deletingAPersonDeletesTheirPhoto() = runBlocking {
        val id = repo.save(0, "Anna", "", photoFile = "a.jpg")
        repo.delete(id)
        assertEquals(listOf("a.jpg"), deletedPhotos)
    }

    @Test
    fun placeAndCoordinatesAreSavedAndSearchable() = runBlocking {
        val id = repo.save(0, PersonDraft(name = "Anna", metPlace = "  Brno, Morava ", metLat = 49.19, metLon = 16.6))
        val anna = requireNotNull(repo.getPerson(id))
        assertEquals("Brno, Morava", anna.metCity)
        assertEquals(49.19, requireNotNull(anna.metLat), 0.0)
        assertEquals(listOf("Anna"), names("morava"))
    }

    @Test
    fun blankPlaceAndHalfCoordinatesAreNotStored() = runBlocking {
        val id = repo.save(0, PersonDraft(name = "Anna", metPlace = "   ", metLat = 49.19, metLon = null))
        val anna = requireNotNull(repo.getPerson(id))
        assertNull(anna.metCity)
        assertNull(anna.metLat)
        assertNull(anna.metLon)
    }

    @Test
    fun shortSaveKeepsThePlace() = runBlocking {
        val id = repo.save(0, PersonDraft(name = "Anna", metPlace = "Brno", metLat = 1.0, metLon = 2.0))
        repo.save(id, "Anna N.", "")
        assertEquals("Brno", repo.getPerson(id)?.metCity)
        assertEquals(2.0, requireNotNull(repo.getPerson(id)?.metLon), 0.0)
    }

    @Test
    fun deleteManyKeepsMeAndDeletesPhotos() = runBlocking {
        val anna = repo.save(0, "Anna", "", photoFile = "a.jpg")
        val peter = repo.save(0, "Peter", "")
        repo.save(0, "Eva", "")
        val me = requireNotNull(repo.getSelf()).id
        assertEquals(2, repo.deleteMany(listOf(anna, peter, me)))
        assertEquals(listOf("Eva"), names())
        assertNotNull(repo.getSelf())
        assertEquals(listOf("a.jpg"), deletedPhotos)
    }

    private fun repoAt(millis: Long, zone: ZoneId) =
        PeopleRepository(db.personDao(), clock = { millis }, locale = { Locale.ENGLISH }, zone = { zone })

    @Test
    fun emptyFirstMetDateIsToday() = runBlocking {
        val id = repoAt(20_000 * DAY + 5_000, ZoneOffset.UTC).save(0, PersonDraft("Anna"))
        assertEquals(20_000L, repo.getPerson(id)?.firstMetOn)
    }

    @Test
    fun todayFollowsTheLocalTimeZone() = runBlocking {
        // 15:00 UTC is already the next day in Auckland (UTC+13 in November).
        val id = repoAt(20_000 * DAY + 15 * HOUR, ZoneId.of("Pacific/Auckland")).save(0, PersonDraft("Anna"))
        assertEquals(20_001L, repo.getPerson(id)?.firstMetOn)
    }

    @Test
    fun givenFirstMetDateIsKeptAndCanChange() = runBlocking {
        val id = repoAt(20_000 * DAY, ZoneOffset.UTC).save(0, PersonDraft("Anna", firstMetOn = 19_000))
        assertEquals(19_000L, repo.getPerson(id)?.firstMetOn)

        repoAt(20_500 * DAY, ZoneOffset.UTC).save(id, PersonDraft("Anna", firstMetOn = 18_000))
        assertEquals(18_000L, repo.getPerson(id)?.firstMetOn)
    }

    @Test
    fun shortSaveKeepsTheFirstMetDate() = runBlocking {
        val id = repo.save(0, PersonDraft("Anna", firstMetOn = 19_000))
        repo.save(id, "Anna N.", "note")
        assertEquals(19_000L, repo.getPerson(id)?.firstMetOn)
    }

    @Test
    fun personWithoutStoredDateCountsFromTheDayTheyWereAdded() = runBlocking {
        val id = db.personDao().insert(Person(name = "Old", createdAt = 19_500 * DAY + 1))
        val old = repoAt(20_000 * DAY, ZoneOffset.UTC)
        assertEquals(19_500L, old.firstMetDay(old.getPerson(id)!!))
        old.save(id, "Old", "")
        assertEquals(19_500L, repo.getPerson(id)?.firstMetOn)
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
