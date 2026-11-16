package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.RelationTypesRepository
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
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

/** F30: the relationship types are a list the user edits in Settings. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RelationTypesTest {

    private lateinit var db: AppDatabase
    private lateinit var types: RelationTypesRepository
    private lateinit var links: RelationshipsRepository
    private lateinit var people: PeopleRepository

    @Before
    fun setUp() {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        // Labels in two languages, as the app gives them.
        types = RelationTypesRepository(db.relationKindDao()) { type ->
            when (type) {
                RelationType.FRIEND -> setOf("Friend", "Priateľ")
                else -> setOf(type.name.lowercase())
            }
        }
        links = RelationshipsRepository(db.relationshipDao(), db.personDao(), db.relationKindDao())
        people = PeopleRepository(db.personDao())
    }

    @After
    fun tearDown() = db.close()

    private fun all() = runBlocking { types.observeTypes().first() }

    @Test
    fun aNewDatabaseHasTheNineBuiltInTypes() {
        assertEquals(RelationType.entries, all().map { it.builtIn })
        assertTrue(all().all { it.name == null })
    }

    @Test
    fun namesAreUniqueInAnyCaseAndInEveryLanguageOfABuiltInLabel() = runBlocking {
        assertNull(types.add("friend"))
        assertNull(types.add(" PRIATEĽ "))
        val mentor = assertNotNull(types.add("Mentor")).let { requireNotNull(types.add("Coach")) }
        assertNull(types.add("coach"))
        assertFalse(types.rename(mentor, "mentor"))
        // A renamed built-in type gives its old label free.
        assertTrue(types.rename(RelationType.FRIEND.seedId, "Buddy"))
        assertNotNull(types.add("Friend"))
        assertEquals("Buddy", all().first { it.builtIn == RelationType.FRIEND }.name)
    }

    @Test
    fun deletingATypeTurnsItsLinksIntoOther() = runBlocking {
        val anna = people.save(0, "Anna", "")
        val peter = people.save(0, "Peter", "")
        val mentor = requireNotNull(types.add("Mentor"))
        links.add(anna, peter, mentor)
        links.add(anna, peter, RelationType.OTHER.seedId)
        links.add(anna, peter, RelationType.FRIEND.seedId)
        assertTrue(types.delete(mentor))
        assertTrue(types.delete(RelationType.FRIEND.seedId))
        // The two links become the same "Other" link, so one is left.
        assertEquals(listOf(RelationType.OTHER), links.observeFor(anna).first().map { it.type.builtIn })
        assertEquals(8, all().size)
    }

    @Test
    fun otherParentAndChildCannotBeDeleted() = runBlocking {
        listOf(RelationType.OTHER, RelationType.PARENT, RelationType.CHILD).forEach {
            assertFalse(types.delete(it.seedId))
        }
        assertEquals(9, all().size)
    }

    @Test
    fun aTypeTheUserMadeReadsTheSameFromBothSides() = runBlocking {
        val anna = people.save(0, "Anna", "")
        val peter = people.save(0, "Peter", "")
        val mentor = requireNotNull(types.add("Mentor"))
        links.add(anna, peter, mentor)
        assertEquals("Mentor", links.observeFor(peter).first().single().type.name)
    }
}
