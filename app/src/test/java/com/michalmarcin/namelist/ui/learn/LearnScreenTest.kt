package com.michalmarcin.namelist.ui.learn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.LearnRound
import com.michalmarcin.namelist.data.repo.LearnCard
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class LearnScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val anna = LearnCard(
        personId = 2,
        name = "Anna Nováková",
        tags = listOf("Work", "Gym"),
        spark = "Her dog Max and the Brno half marathon",
        place = "Brno",
        notes = "Met at the tram stop",
    )

    @Test
    fun theFrontShowsDetailsAndTheFlipShowsTheName() {
        var flipped by mutableStateOf(false)
        val answers = mutableListOf<Boolean>()
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                LearnScreen(
                    state = LearnUiState.Card(anna, number = 3, total = 10, flipped = flipped),
                    onBack = {},
                    onFlip = { flipped = !flipped },
                    onAnswer = { answers += it },
                    onNewRound = {},
                    onOpenPerson = {},
                )
            }
        }
        compose.onNodeWithText("Card 3 of 10").assertIsDisplayed()
        compose.onNodeWithText("Work  ·  Gym").assertIsDisplayed()
        compose.onNodeWithText("Who is this?").assertIsDisplayed()
        compose.onNodeWithText("Her dog Max and the Brno half marathon").assertIsDisplayed()
        compose.onNodeWithText("Brno").assertIsDisplayed()
        compose.onNodeWithText("Met at the tram stop").assertIsDisplayed()
        compose.onNodeWithText("Anna Nováková").assertDoesNotExist()
        compose.onNodeWithTag("learnKnew").assertIsNotEnabled()
        compose.onNodeWithTag("learnNotKnew").assertIsNotEnabled()

        compose.onNodeWithTag("learnCard").performClick()
        compose.onNodeWithTag("learnName", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Anna Nováková").assertIsDisplayed()
        compose.onNodeWithText("Her dog Max and the Brno half marathon").assertDoesNotExist()
        compose.onNodeWithTag("learnKnew").assertIsEnabled().performClick()
        compose.onNodeWithTag("learnNotKnew").performClick()
        assertEquals(listOf(true, false), answers)
    }

    @Test
    fun theResultShowsScoreChangeChartAndMissedPeople() {
        var opened = 0L
        var again = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                LearnScreen(
                    state = LearnUiState.Result(
                        known = 8,
                        total = 10,
                        missed = listOf(anna),
                        history = listOf(
                            LearnRound(finishedAt = 1_700_000_000_000, known = 5, total = 10),
                            LearnRound(finishedAt = 1_700_090_000_000, known = 8, total = 10),
                        ),
                    ),
                    onBack = {},
                    onFlip = {},
                    onAnswer = {},
                    onNewRound = { again++ },
                    onOpenPerson = { opened = it },
                )
            }
        }
        compose.onNodeWithText("8 of 10").assertIsDisplayed()
        compose.onNodeWithText("30 points more than the last round").assertIsDisplayed()
        compose.onNodeWithText("Best: 80%").assertIsDisplayed()
        compose.onNodeWithTag("learnChart").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("learnMissed-2").performScrollTo().performClick()
        assertEquals(2L, opened)
        compose.onNodeWithTag("learnAgain").performScrollTo().performClick()
        assertEquals(1, again)
    }

    @Test
    fun aPerfectRoundSaysSo() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                LearnScreen(
                    state = LearnUiState.Result(10, 10, emptyList(), listOf(LearnRound(finishedAt = 1, known = 10, total = 10))),
                    onBack = {}, onFlip = {}, onAnswer = {}, onNewRound = {}, onOpenPerson = {},
                )
            }
        }
        compose.onNodeWithText("Your first round").assertIsDisplayed()
        compose.onNodeWithTag("learnAllKnown").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withNobodyToLearnTheScreenExplainsWhy() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                LearnScreen(LearnUiState.NoPeople, onBack = {}, onFlip = {}, onAnswer = {}, onNewRound = {}, onOpenPerson = {})
            }
        }
        compose.onNodeWithText("Nothing to learn yet").assertIsDisplayed()
    }
}
