package com.michalmarcin.namelist.ui.learn

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.repo.LearnRepository
import com.michalmarcin.namelist.testutil.MainDispatcherRule
import com.michalmarcin.namelist.testutil.await
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

/** F35: a round of ten cards, flip, answer, saved result with the change against the last round. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LearnViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val db = AppDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
    private var now = 1_000L
    private val repo = LearnRepository(db.personDao(), db.tabDao(), db.learnRoundDao(), clock = { now })

    @After
    fun close() = db.close()

    private fun addPeople(count: Int) = runBlocking {
        db.personDao().insertAll((1..count).map { Person(name = "Person $it", notes = "Detail $it", sortIndex = it.toLong()) })
    }

    private fun LearnViewModel.card() = uiState.await { it is LearnUiState.Card } as LearnUiState.Card

    @Test
    fun withoutDetailsThereIsNothingToLearn() {
        runBlocking { db.personDao().insert(Person(name = "Only a name")) }
        val vm = LearnViewModel(repo, Random(1))
        assertEquals(LearnUiState.NoPeople, vm.uiState.await { it != LearnUiState.Loading })
    }

    @Test
    fun aFullRoundIsSavedAndShowsTheMissedPeople() {
        addPeople(14)
        val vm = LearnViewModel(repo, Random(1))
        val seen = mutableListOf<Long>()
        repeat(10) { index ->
            val card = vm.card()
            assertEquals(index + 1, card.number)
            assertEquals(10, card.total)
            assertFalse(card.flipped)
            // An answer before the flip does nothing.
            vm.answer(true)
            assertEquals(index + 1, vm.card().number)
            vm.flip()
            assertTrue(vm.card().flipped)
            seen += card.card.personId
            vm.answer(known = index < 7)
        }
        assertEquals(10, seen.toSet().size)
        val result = vm.uiState.await { it is LearnUiState.Result } as LearnUiState.Result
        assertEquals(7, result.known)
        assertEquals(10, result.total)
        assertEquals(seen.drop(7), result.missed.map { it.personId })
        assertEquals(70, result.percent)
        assertNull(result.change)
        assertEquals(listOf(70), runBlocking { repo.history() }.map { it.percent })
    }

    @Test
    fun theSecondRoundComparesWithTheFirst() {
        addPeople(3)
        val vm = LearnViewModel(repo, Random(1))
        repeat(3) {
            vm.card()
            vm.flip()
            vm.answer(known = it == 0)
        }
        vm.uiState.await { it is LearnUiState.Result }
        now = 2_000L
        vm.newRound()
        repeat(3) {
            vm.card()
            vm.flip()
            vm.answer(known = true)
        }
        val result = vm.uiState.await { it is LearnUiState.Result && it.history.size == 2 } as LearnUiState.Result
        assertEquals(3, result.known)
        assertEquals(67, result.change)
        assertEquals(100, result.best)
        assertTrue(result.missed.isEmpty())
    }
}
