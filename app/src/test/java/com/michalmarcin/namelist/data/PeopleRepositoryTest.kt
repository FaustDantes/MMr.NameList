package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
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
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PeopleRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PeopleRepository

    @Before
    fun setUp() {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        repo = PeopleRepository(db.personDao(), clock = { 1_000L }, locale = { Locale.ENGLISH })
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
}
