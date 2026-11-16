package com.michalmarcin.namelist.ui.people

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.data.repo.TabsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PeopleViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var people: PeopleRepository
    private lateinit var tabs: TabsRepository
    private lateinit var vm: PeopleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        people = PeopleRepository(db.personDao())
        tabs = TabsRepository(db.tabDao())
        vm = PeopleViewModel(people, tabs, SavedStateHandle())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun state(check: (PeopleUiState) -> Boolean) = runBlocking {
        withTimeout(5_000) { vm.uiState.first { !it.isLoading && check(it) } }
    }

    private fun names() = runBlocking { people.observePeople("", SortMode.MANUAL).first().map { it.name } }

    @Test
    fun archivingTheSelectionMovesThemOutOfTheListAndClearsTheSelection() {
        val anna = runBlocking { people.save(0, "Anna", "") }
        val peter = runBlocking { people.save(0, "Peter", "") }
        state { it.people.size == 2 }

        vm.onToggleSelect(anna)
        state { it.selected == setOf(anna) }
        vm.onArchiveSelected()

        val after = state { it.people.size == 1 && !it.isSelecting }
        assertEquals(listOf(peter), after.people.map { it.id })
        assertEquals(listOf("Anna"), runBlocking { people.observeArchived().first().map { it.name } })
    }

    @Test
    fun bulkActionsNeverTouchHiddenPeople() {
        val work = runBlocking { tabs.add("Work")!! }
        val anna = runBlocking { people.save(0, "Anna", "", tabId = work) }
        val peter = runBlocking { people.save(0, "Peter", "") }
        state { it.people.size == 2 }

        vm.onToggleSelect(anna)
        vm.onToggleSelect(peter)
        assertEquals(setOf(anna, peter), state { it.selected.size == 2 }.selected)

        // Filter to "Work": Peter is hidden, so he drops out of the selection.
        vm.onSelectTab(work)
        assertEquals(setOf(anna), state { it.selectedTabId == work && it.people.size == 1 }.selected)

        vm.onDeleteSelected()
        state { it.people.isEmpty() && !it.isSelecting }
        assertEquals(listOf("Peter"), names())
    }

    @Test
    fun selectAllAndMoveToTab() {
        val family = runBlocking { tabs.add("Family")!! }
        runBlocking {
            people.save(0, "Anna", "")
            people.save(0, "Peter", "")
        }
        state { it.people.size == 2 }
        vm.onSelectAll()
        state { it.selected.size == 2 }
        vm.onMoveSelected(family)
        state { !it.isSelecting }
        val inFamily = runBlocking { people.observePeople("", SortMode.MANUAL, family).first().map { it.name } }
        assertEquals(listOf("Anna", "Peter"), inFamily)
    }

    @Test
    fun clearSelection() {
        val anna = runBlocking { people.save(0, "Anna", "") }
        state { it.people.size == 1 }
        vm.onToggleSelect(anna)
        state { it.isSelecting }
        vm.onClearSelection()
        assertEquals(emptySet<Long>(), state { !it.isSelecting }.selected)
    }
}
