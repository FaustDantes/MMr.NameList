package com.michalmarcin.namelist.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.LearnRound
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonTab
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.LearnRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

/** F35: which people go on cards, and the saved results. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LearnTest {

    private fun person(id: Long, spark: String? = null, city: String? = null, notes: String = "", archived: Boolean = false, self: Boolean = false) =
        Person(id = id, name = "Person $id", askAbout = spark, metCity = city, notes = notes, archivedAt = if (archived) 1L else null, isSelf = self)

    @Test
    fun onlyPeopleWithASparkAPlaceOrNotesAreOnCards() {
        val people = listOf(
            person(1, spark = "x", self = true),
            person(2, spark = "Her dog Max"),
            person(3, city = "Brno"),
            person(4, notes = "Plays chess"),
            person(5, notes = "   "),
            person(6),
            person(7, spark = "x", archived = true),
        )
        // Person 6 has only a tag: not enough to guess.
        val deck = LearnRepository.buildDeck(people, listOf(PersonTab(6, 1)), listOf(Tab(1, "Work")), 10, Random(1))
        assertEquals(setOf(2L, 3L, 4L), deck.map { it.personId }.toSet())
    }

    @Test
    fun aRoundHasTenDifferentPeopleInRandomOrder() {
        val people = (2L..30L).map { person(it, notes = "n$it") }
        val first = LearnRepository.buildDeck(people, emptyList(), emptyList(), 10, Random(1))
        val second = LearnRepository.buildDeck(people, emptyList(), emptyList(), 10, Random(2))
        assertEquals(10, first.size)
        assertEquals(10, first.map { it.personId }.toSet().size)
        assertTrue(first.map { it.personId } != second.map { it.personId })
        // With fewer people than ten, everyone is in the round once.
        assertEquals(3, LearnRepository.buildDeck(people.take(3), emptyList(), emptyList(), 10, Random(1)).size)
    }

    @Test
    fun theCardCarriesTagsSparkPlaceAndNotes() {
        val tags = listOf(Tab(1, "Work", sortIndex = 2), Tab(2, "Gym", sortIndex = 1), Tab(3, "Family", sortIndex = 3))
        val p = person(2, spark = " Her dog Max ", city = "Brno", notes = "Met at the tram stop")
        val card = LearnRepository.buildDeck(listOf(p), listOf(PersonTab(2, 1), PersonTab(2, 2)), tags, 10, Random(1)).single()
        assertEquals(listOf("Gym", "Work"), card.tags)
        assertEquals("Her dog Max", card.spark)
        assertEquals("Brno", card.place)
        assertEquals("Met at the tram stop", card.notes)
        assertEquals("Person 2", card.name)
        val bare = LearnRepository.buildDeck(listOf(person(3, notes = "x")), emptyList(), tags, 10, Random(1)).single()
        assertNull(bare.spark)
        assertNull(bare.place)
    }

    @Test
    fun percentIsRounded() {
        assertEquals(70, LearnRound(finishedAt = 0, known = 7, total = 10).percent)
        assertEquals(67, LearnRound(finishedAt = 0, known = 2, total = 3).percent)
        assertEquals(0, LearnRound(finishedAt = 0, known = 0, total = 0).percent)
    }

    @Test
    fun roundsAreSavedOldestFirst() = runBlocking {
        val db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        try {
            var now = 1_000L
            val repo = LearnRepository(db.personDao(), db.tabDao(), db.learnRoundDao(), clock = { now })
            repo.saveRound(known = 4, total = 10)
            now = 2_000L
            val history = repo.saveRound(known = 9, total = 10)
            assertEquals(listOf(40, 90), history.map { it.percent })
            assertEquals(listOf(1_000L, 2_000L), repo.history().map { it.finishedAt })
        } finally {
            db.close()
        }
    }
}
