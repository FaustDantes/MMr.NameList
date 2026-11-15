package com.michalmarcin.namelist.ui.tabs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class ManageTabsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val added = mutableListOf<String>()
    private val deleted = mutableListOf<Long>()

    private fun show(state: ManageTabsUiState) = compose.setContent {
        NameListTheme(dynamicColor = false) {
            ManageTabsScreen(state, {}, { added += it }, { _, _ -> }, { deleted += it }, {})
        }
    }

    @Test
    fun emptyStateWhenNoTabs() {
        show(ManageTabsUiState(isLoading = false))
        compose.onNodeWithText("No tabs yet").assertIsDisplayed()
    }

    @Test
    fun addTabThroughDialog() {
        // Under Robolectric the dialog window never reports idle while the clock runs freely,
        // so this test advances the clock by hand.
        compose.mainClock.autoAdvance = false
        show(ManageTabsUiState(isLoading = false))
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("addTab").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("confirmTabName").assertIsNotEnabled()
        compose.onNodeWithTag("tabNameField").performTextInput("Work")
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("confirmTabName").performClick()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(listOf("Work"), added)
    }

    @Test
    fun deleteTabAfterConfirm() {
        show(ManageTabsUiState(tabs = SampleData.tabs, isLoading = false))
        compose.onNodeWithTag("delete-${SampleData.tabs[1].id}").performClick()
        compose.onNodeWithTag("confirmDeleteTab").performClick()
        assertEquals(listOf(SampleData.tabs[1].id), deleted)
    }
}
