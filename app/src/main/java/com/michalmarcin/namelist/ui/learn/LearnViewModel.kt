package com.michalmarcin.namelist.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michalmarcin.namelist.data.db.LearnRound
import com.michalmarcin.namelist.data.repo.LearnCard
import com.michalmarcin.namelist.data.repo.LearnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface LearnUiState {
    data object Loading : LearnUiState

    /** Nobody has a spark, a place or notes, so there is nothing to learn. */
    data object NoPeople : LearnUiState

    /** Card [number] of [total]. [flipped] = the name side is up. */
    data class Card(
        val card: LearnCard,
        val number: Int,
        val total: Int,
        val flipped: Boolean,
    ) : LearnUiState

    /** The end of a round. [history] holds all saved rounds, the oldest first, this round last. */
    data class Result(
        val known: Int,
        val total: Int,
        val missed: List<LearnCard>,
        val history: List<LearnRound>,
    ) : LearnUiState {
        val percent: Int get() = history.lastOrNull()?.percent ?: 0

        /** Change in percent points against the round before; null for the first round. */
        val change: Int? get() = if (history.size < 2) null else history[history.size - 1].percent - history[history.size - 2].percent

        val best: Int get() = history.maxOfOrNull { it.percent } ?: 0
    }
}

/**
 * Learning with flash cards (F35). A round has up to ten random people. The user reads the front, flips the card
 * to see the name, then says if they knew it. After the last card the round is saved and the progress is shown.
 */
class LearnViewModel(
    private val repository: LearnRepository,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val state = MutableStateFlow<LearnUiState>(LearnUiState.Loading)
    val uiState: StateFlow<LearnUiState> = state.asStateFlow()

    private var deck: List<LearnCard> = emptyList()
    private val answers = mutableListOf<Boolean>()

    init {
        newRound()
    }

    fun newRound() {
        state.value = LearnUiState.Loading
        viewModelScope.launch {
            deck = repository.newDeck(random = random)
            answers.clear()
            state.value = if (deck.isEmpty()) LearnUiState.NoPeople else cardState(0, flipped = false)
        }
    }

    fun flip() {
        val current = state.value as? LearnUiState.Card ?: return
        state.value = current.copy(flipped = !current.flipped)
    }

    /** Only after the name is shown, so a tap cannot skip a card by mistake. */
    fun answer(known: Boolean) {
        val current = state.value as? LearnUiState.Card ?: return
        if (!current.flipped || answers.size != current.number - 1) return
        answers += known
        if (answers.size < deck.size) {
            state.value = cardState(answers.size, flipped = false)
        } else {
            finish()
        }
    }

    private fun finish() {
        val known = answers.count { it }
        val missed = deck.filterIndexed { index, _ -> !answers[index] }
        val total = deck.size
        viewModelScope.launch {
            val history = repository.saveRound(known, total)
            state.value = LearnUiState.Result(known = known, total = total, missed = missed, history = history)
        }
    }

    private fun cardState(index: Int, flipped: Boolean) =
        LearnUiState.Card(card = deck[index], number = index + 1, total = deck.size, flipped = flipped)
}
