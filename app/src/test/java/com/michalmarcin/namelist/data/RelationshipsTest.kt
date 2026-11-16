package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.AddResult
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RelationshipsTest {

    private lateinit var db: AppDatabase
    private lateinit var people: PeopleRepository
    private lateinit var links: RelationshipsRepository
    private var anna = 0L
    private var peter = 0L

    @Before
    fun setUp() = runBlocking {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        people = PeopleRepository(db.personDao())
        links = RelationshipsRepository(db.relationshipDao(), db.personDao(), db.relationKindDao())
        anna = people.save(0, "Anna", "")
        peter = people.save(0, "Peter", "")
    }

    @After
    fun tearDown() = db.close()

    /** "name:TYPE" as seen from [personId]. */
    private fun view(personId: Long) = runBlocking {
        links.observeFor(personId).first().map { "${it.other.name}:${it.type.builtIn}" }
    }

    @Test
    fun inverseRules() {
        RelationType.entries.forEach { assertEquals(it, it.inverse.inverse) }
        assertEquals(RelationType.CHILD, RelationType.PARENT.inverse)
        assertEquals(RelationType.SIBLING, RelationType.SIBLING.inverse)
        assertEquals(RelationType.FRIEND, RelationType.FRIEND.inverse)
    }

    @Test
    fun linkShowsOnBothSidesWithTheInverse() = runBlocking {
        // "Peter is Anna's parent."
        assertEquals(AddResult.ADDED, links.add(anna, peter, RelationType.PARENT.seedId))
        assertEquals(listOf("Peter:PARENT"), view(anna))
        assertEquals(listOf("Anna:CHILD"), view(peter))
    }

    @Test
    fun duplicatesAreRejectedFromEitherSide() = runBlocking {
        links.add(anna, peter, RelationType.PARENT.seedId)
        assertEquals(AddResult.DUPLICATE, links.add(anna, peter, RelationType.PARENT.seedId))
        // Same fact entered on Peter's page: "Anna is Peter's child".
        assertEquals(AddResult.DUPLICATE, links.add(peter, anna, RelationType.CHILD.seedId))
        // A different fact is allowed.
        assertEquals(AddResult.ADDED, links.add(anna, peter, RelationType.COLLEAGUE.seedId))
        assertEquals(AddResult.SAME_PERSON, links.add(anna, anna, RelationType.FRIEND.seedId))
    }

    @Test
    fun changingTypeFromTheOtherSideUpdatesBoth() = runBlocking {
        links.add(anna, peter, RelationType.FRIEND.seedId)
        val id = links.observeFor(peter).first().single().id
        // On Peter's page: "Anna is Peter's parent".
        links.changeType(id, viewerId = peter, typeId = RelationType.PARENT.seedId)
        assertEquals(listOf("Anna:PARENT"), view(peter))
        assertEquals(listOf("Peter:CHILD"), view(anna))
    }

    @Test
    fun deletingAPersonRemovesTheirLinks() = runBlocking {
        val eva = people.save(0, "Eva", "")
        links.add(anna, peter, RelationType.SIBLING.seedId)
        links.add(anna, eva, RelationType.FRIEND.seedId)
        people.delete(peter)
        assertEquals(listOf("Eva:FRIEND"), view(anna))
    }

    @Test
    fun linkToMe() = runBlocking {
        val me = requireNotNull(people.getSelf()).id
        links.add(anna, me, RelationType.FRIEND.seedId)
        assertEquals(listOf("Me:FRIEND"), view(anna))
        assertEquals(listOf("Anna:FRIEND"), view(me))
    }

    @Test
    fun removeLink() = runBlocking {
        links.add(anna, peter, RelationType.SIBLING.seedId)
        links.delete(links.observeFor(anna).first().single().id)
        assertEquals(emptyList<String>(), view(peter))
    }
}
