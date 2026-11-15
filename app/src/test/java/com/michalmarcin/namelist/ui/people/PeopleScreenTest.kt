package com.michalmarcin.namelist.ui.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.testutil.SampleData
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
class PeopleScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(
        state: PeopleUiState,
        onQuery: (String) -> Unit = {},
        onSort: (SortMode) -> Unit = {},
        onAdd: () -> Unit = {},
        onOpen: (Long) -> Unit = {},
    ) = compose.setContent {
        NameListTheme(dynamicColor = false) { PeopleScreen(state, onQuery, onSort, onAdd, onOpen) }
    }

    @Test
    fun emptyStateWhenNoPeople() {
        show(PeopleUiState(isLoading = false))
        compose.onNodeWithText("No people yet").assertIsDisplayed()
    }

    @Test
    fun noMatchesWhenSearchFindsNothing() {
        show(PeopleUiState(query = "xyz", isLoading = false))
        compose.onNodeWithText("No matches").assertIsDisplayed()
    }

    @Test
    fun listShowsPeopleAndOpensOne() {
        var opened = -1L
        show(PeopleUiState(people = SampleData.people, isLoading = false), onOpen = { opened = it })
        compose.onNodeWithText("Anna Nováková").assertIsDisplayed()
        compose.onNodeWithText("Peter Horváth").performClick()
        assertEquals(3L, opened)
    }

    @Test
    fun typingSendsQuery() {
        var query by mutableStateOf("")
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(PeopleUiState(query = query, isLoading = false), { query = it }, {}, {}, {})
            }
        }
        compose.onNodeWithTag("search").performTextInput("Anna")
        assertEquals("Anna", query)
    }

    @Test
    fun sortMenuSelectsAtoZ() {
        var sort = SortMode.MANUAL
        show(PeopleUiState(people = SampleData.people, isLoading = false), onSort = { sort = it })
        compose.onNodeWithTag("sortMenu").performClick()
        compose.onNodeWithText("A to Z").performClick()
        assertEquals(SortMode.A_TO_Z, sort)
    }

    @Test
    fun addButtonCallsAdd() {
        var added = false
        show(PeopleUiState(isLoading = false), onAdd = { added = true })
        compose.onNodeWithTag("addPerson").performClick()
        assertEquals(true, added)
    }
}
