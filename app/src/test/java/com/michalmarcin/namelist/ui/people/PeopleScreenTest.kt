package com.michalmarcin.namelist.ui.people

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PeopleScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent { NameListTheme(dynamicColor = false) { PeopleScreen() } }
    }

    @Test
    fun showsTitleAndEmptyState() {
        compose.onNodeWithText("People").assertIsDisplayed()
        compose.onNodeWithText("No people yet").assertIsDisplayed()
    }

    @Test
    fun searchFieldAcceptsText() {
        compose.onNodeWithTag("search").performTextInput("Anna")
        compose.onNodeWithText("Anna").assertIsDisplayed()
    }

    @Test
    fun addButtonShowsMessage() {
        val message = "Adding people comes in the next update."
        compose.onNodeWithText("Add person").performClick()
        // The snackbar is shown from a coroutine, so wait until it is in the tree.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(message).assertExists()
    }
}
