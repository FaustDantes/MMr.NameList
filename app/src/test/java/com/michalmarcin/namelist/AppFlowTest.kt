package com.michalmarcin.namelist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The whole app with its real database, navigation and ViewModel factories, the way the owner uses it most:
 * add people, search, open, edit and delete. Catches wiring mistakes that screen tests with fake state miss.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), 10_000)

    private fun waitGone(text: String) = compose.waitUntilDoesNotExist(hasText(text), 10_000)

    /**
     * Waits for a screen by a test tag that only that screen has. Waiting for a name is not enough:
     * after Save, the name is still on screen in the edit form's Name field.
     */
    private fun waitForScreen(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 10_000)

    private fun search() = compose.onNodeWithTag("search")

    private fun addPerson(name: String, notes: String = "") {
        waitForScreen("addPerson")
        compose.onNodeWithTag("addPerson").performClick()
        waitForScreen("nameField")
        compose.onNodeWithTag("nameField").performTextInput(name)
        if (notes.isNotEmpty()) {
            compose.onNodeWithTag("detailsToggle").performClick()
            compose.onNodeWithTag("notesField").performTextInput(notes)
        }
        compose.onNodeWithTag("save").performClick()
        waitForScreen("peopleList")
        waitFor(name)
    }

    @Test
    fun addSearchOpenEditDelete() {
        waitFor("No people yet")
        addPerson("Anna Nováková", notes = "Met at the tram stop")
        addPerson("Peter Horváth")

        // Search ignores case and accents.
        search().performTextInput("NOVA")
        waitGone("Peter Horváth")
        compose.onNodeWithText("Anna Nováková").performClick()

        // Detail, then edit the name.
        waitForScreen("relationsSection")
        waitFor("Met at the tram stop")
        compose.onNodeWithTag("edit").performClick()
        waitForScreen("nameField")
        compose.onNodeWithTag("nameField").performTextClearance()
        compose.onNodeWithTag("nameField").performTextInput("Anna Svobodová")
        compose.onNodeWithTag("save").performClick()
        waitForScreen("relationsSection") // back on the detail screen
        waitFor("Anna Svobodová")

        // Delete from the detail screen; the app goes back to the list.
        compose.onNodeWithTag("delete").performClick()
        compose.onNodeWithTag("confirmDelete").performClick()
        waitForScreen("search")
        waitGone("Anna Svobodová")
        search().performTextClearance()
        waitFor("Peter Horváth")
    }

    @Test
    fun longPressSelectsAndBulkDeletes() {
        waitFor("No people yet")
        addPerson("Anna")
        addPerson("Peter")
        addPerson("Eva")

        compose.onNodeWithText("Anna").performTouchInput { longClick() }
        waitFor("1 selected")
        compose.onNodeWithText("Peter").performClick()
        waitFor("2 selected")
        compose.onNodeWithTag("bulkDelete").performClick()
        compose.onNodeWithTag("confirmBulkDelete").performClick()

        waitGone("Peter")
        waitGone("Anna")
        compose.onNodeWithText("Eva").assertExists()
    }
}
