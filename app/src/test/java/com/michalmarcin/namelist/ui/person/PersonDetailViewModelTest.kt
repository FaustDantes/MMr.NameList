package com.michalmarcin.namelist.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.AddResult
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.testutil.MainDispatcherRule
import com.michalmarcin.namelist.testutil.await
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Most used feature: opening a person and managing their links. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PersonDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var people: PeopleRepository
    private lateinit var tabs: TabsRepository
    private lateinit var links: RelationshipsRepository
    private var anna = 0L
    private var peter = 0L

    @Before
    fun setUp() = runBlocking {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        people = PeopleRepository(db.personDao())
        tabs = TabsRepository(db.tabDao())
        links = RelationshipsRepository(db.relationshipDao(), db.personDao())
        val work = tabs.add("Work")
        anna = people.save(0, "Anna", "", tabId = work)
        peter = people.save(0, "Peter", "")
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel(id: Long) = PersonDetailViewModel(people, tabs, links, SavedStateHandle(mapOf("id" to id)))

    @Test
    fun showsThePersonTabAndWhoCanBeLinked() {
        val state = viewModel(anna).uiState.await { !it.isLoading && it.candidates.isNotEmpty() }
        assertEquals("Anna", state.person?.name)
        assertEquals("Work", state.tabName)
        // Everyone except Anna herself, "Me" first.
        assertEquals(listOf("Me", "Peter"), state.candidates.map { it.name })
    }

    @Test
    fun addedLinkShowsAndDuplicateGivesAMessage() {
        val vm = viewModel(anna)
        vm.uiState.await { !it.isLoading }
        vm.addRelationship(peter, RelationType.SIBLING)
        val withLink = vm.uiState.await { it.relations.isNotEmpty() }
        assertEquals("Peter" to RelationType.SIBLING, withLink.relations.single().let { it.other.name to it.type })
        assertNull(withLink.relationMessage)

        vm.addRelationship(peter, RelationType.SIBLING)
        vm.uiState.await { it.relationMessage == AddResult.DUPLICATE }
        vm.clearMessage()
        vm.uiState.await { it.relationMessage == null }
    }

    @Test
    fun changeAndRemoveALink() {
        val vm = viewModel(peter)
        vm.uiState.await { !it.isLoading }
        vm.addRelationship(anna, RelationType.FRIEND)
        val link = vm.uiState.await { it.relations.isNotEmpty() }.relations.single()

        // On Peter's page: "Anna is Peter's parent", so Anna's page shows Peter as her child.
        vm.changeRelationshipType(link.id, RelationType.PARENT)
        vm.uiState.await { it.relations.singleOrNull()?.type == RelationType.PARENT }
        val annaView = viewModel(anna).uiState.await { it.relations.isNotEmpty() }
        assertEquals(RelationType.CHILD, annaView.relations.single().type)

        vm.removeRelationship(link.id)
        vm.uiState.await { it.relations.isEmpty() }
    }

    @Test
    fun deleteMarksTheScreenToClose() {
        val vm = viewModel(peter)
        vm.uiState.await { it.person != null }
        vm.delete()
        vm.uiState.await { it.isDeleted }
        assertNull(runBlocking { people.getPerson(peter) })
    }
}
