package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.PersonDraft
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.data.repo.TabsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TabsTest {

    private lateinit var db: AppDatabase
    private lateinit var people: PeopleRepository
    private lateinit var tabs: TabsRepository

    @Before
    fun setUp() {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        people = PeopleRepository(db.personDao(), locale = { Locale.ENGLISH })
        tabs = TabsRepository(db.tabDao())
    }

    @After
    fun tearDown() = db.close()

    private fun names(tabId: Long? = null, query: String = "") =
        runBlocking { people.observePeople(query, SortMode.MANUAL, tabId).first().map { it.name } }

    private fun tabNames() = runBlocking { tabs.observeTabs().first().map { it.name } }

    @Test
    fun tabFilterShowsOnlyThatTab() = runBlocking {
        val work = tabs.add("Work")
        val family = tabs.add("Family")
        people.save(0, "Anna", "", tabId = work)
        people.save(0, "Peter", "", tabId = family)
        people.save(0, "Eva", "")
        assertEquals(listOf("Anna"), names(work))
        assertEquals(listOf("Peter"), names(family))
        assertEquals(listOf("Anna", "Peter", "Eva"), names())
    }

    @Test
    fun searchIsGlobalAndIgnoresTheTab() = runBlocking {
        val work = tabs.add("Work")
        people.save(0, "Anna", "", tabId = work)
        people.save(0, "Peter", "")
        assertEquals(listOf("Peter"), names(tabId = work, query = "pet"))
    }

    @Test
    fun deletingATabKeepsItsPeopleWithoutATab() = runBlocking {
        val work = tabs.add("Work")
        val anna = people.save(0, "Anna", "", tabId = work)
        tabs.delete(work)
        assertEquals(emptySet<Long>(), people.tabIdsOf(anna))
        assertEquals(listOf("Anna"), names())
    }

    @Test
    fun renameAndReorderTabs() = runBlocking {
        val a = tabs.add("Work")
        val b = tabs.add("Family")
        val c = tabs.add("Sport")
        tabs.rename(a, "  Office ")
        tabs.reorder(listOf(c, a, b))
        assertEquals(listOf("Sport", "Office", "Family"), tabNames())
    }

    @Test
    fun reorderInsideATabKeepsOtherPeopleInPlace() = runBlocking {
        val work = tabs.add("Work")
        val anna = people.save(0, "Anna", "", tabId = work)
        people.save(0, "Peter", "")
        val eva = people.save(0, "Eva", "", tabId = work)
        people.save(0, "Tom", "")
        // Swap Anna and Eva inside "Work".
        people.reorder(listOf(eva, anna))
        assertEquals(listOf("Eva", "Anna"), names(work))
        assertEquals(listOf("Eva", "Peter", "Anna", "Tom"), names())
    }

    @Test
    fun aPersonInTwoTabsShowsInBothAndSurvivesLosingOne() = runBlocking {
        val work = tabs.add("Work")
        val gym = tabs.add("Gym")
        val anna = people.save(0, PersonDraft("Anna", tabIds = setOf(work, gym)))
        people.save(0, "Peter", "", tabId = work)

        assertEquals(listOf("Anna", "Peter"), names(tabId = work))
        assertEquals(listOf("Anna"), names(tabId = gym))
        assertEquals(setOf(work, gym), people.tabIdsOf(anna))

        tabs.delete(work)
        assertEquals(setOf(gym), people.tabIdsOf(anna))
        assertEquals(listOf("Anna"), names(tabId = gym))
        assertEquals(listOf("Anna", "Peter"), names())
    }

    @Test
    fun movingPeopleToATabReplacesAllTheirTabs() = runBlocking {
        val work = tabs.add("Work")
        val gym = tabs.add("Gym")
        val anna = people.save(0, PersonDraft("Anna", tabIds = setOf(work, gym)))
        people.moveToTab(listOf(anna), gym)
        assertEquals(setOf(gym), people.tabIdsOf(anna))
        people.moveToTab(listOf(anna), null)
        assertEquals(emptySet<Long>(), people.tabIdsOf(anna))
    }

    @Test
    fun savingAgainKeepsAndChangesTabs() = runBlocking {
        val work = tabs.add("Work")
        val gym = tabs.add("Gym")
        val anna = people.save(0, PersonDraft("Anna", tabIds = setOf(work)))
        people.save(anna, PersonDraft("Anna", tabIds = setOf(work, gym)))
        assertEquals(setOf(work, gym), people.tabIdsOf(anna))
        people.save(anna, PersonDraft("Anna"))
        assertEquals(emptySet<Long>(), people.tabIdsOf(anna))
    }
}
