package com.michalmarcin.namelist.ui.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.testutil.SampleData
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        onAdd: (Long?) -> Unit = {},
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
    fun learnButtonSitsBottomLeftAndOpensLearning() {
        var opened = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(PeopleUiState(people = SampleData.people, isLoading = false), {}, {}, {}, {}, onLearn = { opened++ })
            }
        }
        // F35: the book button on the left, the add button on the right, both 64 dp, on one line.
        val learn = compose.onNodeWithTag("learn").assertIsDisplayed().getBoundsInRoot()
        val add = compose.onNodeWithTag("addPerson").getBoundsInRoot()
        assertEquals(16f, learn.left.value, 1f)
        assertEquals(64f, (learn.right - learn.left).value, 1f)
        assertEquals(add.top.value, learn.top.value, 1f)
        assertTrue(learn.right < add.left)
        compose.onNodeWithContentDescription("Learn names").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun learnButtonHidesWhileSelecting() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(PeopleUiState(people = SampleData.people, isLoading = false, selected = setOf(2L)), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("learn").assertDoesNotExist()
    }

    @Test
    fun addButtonCallsAdd() {
        var added: Long? = -1
        show(PeopleUiState(isLoading = false, tabs = SampleData.tabs, selectedTabId = 2), onAdd = { added = it })
        compose.onNodeWithTag("addPerson").performClick()
        assertEquals(2L, added)
    }

    @Test
    fun tabChipsShowAndSelect() {
        var selected: Long? = -1
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(
                    PeopleUiState(people = SampleData.people, tabs = SampleData.tabs, isLoading = false),
                    {}, {}, {}, {},
                    onSelectTab = { selected = it },
                )
            }
        }
        compose.onNodeWithText("All").assertIsDisplayed()
        compose.onNodeWithText("Family").performClick()
        assertEquals(2L, selected)
    }

    @Test
    fun noTabChipsWithoutTabs() {
        show(PeopleUiState(people = SampleData.people, isLoading = false))
        compose.onNodeWithTag("tabChips").assertDoesNotExist()
    }

    @Test
    fun reorderOnlyInManualOrderWithoutSearch() {
        val base = PeopleUiState(people = SampleData.people, isLoading = false)
        assertEquals(true, base.canReorder)
        assertEquals(false, base.copy(sort = SortMode.A_TO_Z).canReorder)
        assertEquals(false, base.copy(query = "an").canReorder)
    }

    @Test
    fun longPressStartsSelection() {
        var toggled = -1L
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(
                    PeopleUiState(people = SampleData.people, isLoading = false),
                    {}, {}, {}, {},
                    onToggleSelect = { toggled = it },
                )
            }
        }
        compose.onNodeWithTag("person-3").performTouchInput { longClick() }
        assertEquals(3L, toggled)
    }

    @Test
    fun selectionModeTapTogglesAndShowsBar() {
        var toggled = -1L
        var opened = -1L
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(
                    PeopleUiState(people = SampleData.people, tabs = SampleData.tabs, selected = setOf(2, 3), isLoading = false),
                    {}, {}, {},
                    onOpenPerson = { opened = it },
                    onToggleSelect = { toggled = it },
                )
            }
        }
        compose.onNodeWithText("2 selected").assertIsDisplayed()
        compose.onNodeWithTag("addPerson").assertDoesNotExist()
        compose.onNodeWithTag("bulkMove").assertExists()
        compose.onNodeWithTag("person-4").performClick()
        assertEquals(4L, toggled)
        assertEquals(-1L, opened)
    }

    @Test
    fun bulkArchiveButtonArchivesTheSelection() {
        var archived = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(
                    PeopleUiState(people = SampleData.people, selected = setOf(2, 3), isLoading = false),
                    {}, {}, {}, {},
                    onArchiveSelected = { archived++ },
                )
            }
        }
        compose.onNodeWithTag("bulkArchive").performClick()
        assertEquals(1, archived)
    }

    @Test
    fun topBarHasOnlySettingsAndTheSortLineShowsTheCount() {
        var opened = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(PeopleUiState(people = SampleData.people, isLoading = false), {}, {}, {}, {}, onSettings = { opened++ })
            }
        }
        // ADR-039: no map, archive or tags icon; the sort choice sits under the title.
        compose.onNodeWithTag("map").assertDoesNotExist()
        compose.onNodeWithTag("archive").assertDoesNotExist()
        compose.onNodeWithTag("manageTabs").assertDoesNotExist()
        compose.onNodeWithText("${SampleData.people.size} people").assertIsDisplayed()
        compose.onNodeWithText("Manual order").assertIsDisplayed()
        compose.onNodeWithTag("settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun bulkDeleteNeedsConfirmation() {
        var deletes = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PeopleScreen(
                    PeopleUiState(people = SampleData.people, selected = setOf(2, 3), isLoading = false),
                    {}, {}, {}, {},
                    onDeleteSelected = { deletes++ },
                )
            }
        }
        compose.onNodeWithTag("bulkMove").assertDoesNotExist() // no tabs, nothing to move to
        compose.onNodeWithTag("bulkDelete").performClick()
        assertEquals(0, deletes)
        compose.onNodeWithText("Delete 2 people?").assertIsDisplayed()
        compose.onNodeWithTag("confirmBulkDelete").performClick()
        assertEquals(1, deletes)
    }

    @Test
    fun moveToTabContentPicksTabOrNone() {
        val picks = mutableListOf<Long?>()
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                MoveToTabContent(count = 2, tabs = SampleData.tabs, onPick = { picks += it }, onCancel = {})
            }
        }
        compose.onNodeWithText("Move 2 people to").assertIsDisplayed()
        compose.onNodeWithTag("moveTo-2").performClick()
        compose.onNodeWithTag("moveTo-none").performClick()
        assertEquals(listOf<Long?>(2L, null), picks)
    }

    @Test
    fun noReorderWhileSelecting() {
        val state = PeopleUiState(people = SampleData.people, selected = setOf(2), isLoading = false)
        assertEquals(false, state.canReorder)
    }

    @Test
    fun addButtonIs64dpAndAtTheBottomRight() {
        show(PeopleUiState(people = SampleData.people, isLoading = false))
        val button = compose.onNodeWithTag("addPerson").fetchSemanticsNode()
        val root = compose.onRoot().fetchSemanticsNode()
        val density = compose.density.density
        assertEquals(64f * density, button.size.width.toFloat(), 1f)
        assertEquals(64f * density, button.size.height.toFloat(), 1f)
        assertTrue(button.boundsInRoot.center.x > root.size.width / 2f)
        assertTrue(button.boundsInRoot.center.y > root.size.height / 2f)
    }
}
