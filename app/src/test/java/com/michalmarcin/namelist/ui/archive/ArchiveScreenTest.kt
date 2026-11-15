package com.michalmarcin.namelist.ui.archive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
class ArchiveScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val day = 86_400_000L
    private val now = 400 * day

    private val people = listOf(
        SampleData.people[0].copy(archivedAt = now - 5 * day),
        SampleData.people[1].copy(archivedAt = now - 90 * day),
    )

    @Test
    fun rowsShowWhenAndForHowLongPeopleAreArchived() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                ArchiveScreen(ArchiveUiState(people, isLoading = false), {}, {}, {}, now = now)
            }
        }
        compose.onNodeWithTag("archiveStatus-2").assertTextContains("in the archive for 5 days", substring = true)
        compose.onNodeWithTag("archiveStatus-3").assertTextContains("in the archive for 3 months", substring = true)
    }

    @Test
    fun restoreOpenAndRestoreAllCallBack() {
        var restored = -1L
        var opened = -1L
        var all = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                ArchiveScreen(
                    ArchiveUiState(people, isLoading = false),
                    onBack = {},
                    onOpenPerson = { opened = it },
                    onRestore = { restored = it },
                    onRestoreAll = { all++ },
                    now = now,
                )
            }
        }
        compose.onNodeWithTag("restore-3").performClick()
        assertEquals(3L, restored)
        compose.onNodeWithTag("archived-2").performClick()
        assertEquals(2L, opened)
        compose.onNodeWithTag("restoreAll").performClick()
        assertEquals(1, all)
    }

    @Test
    fun anEmptyArchiveExplainsItself() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                ArchiveScreen(ArchiveUiState(isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithTag("archiveEmpty").assertIsDisplayed()
        compose.onNodeWithTag("restoreAll").assertDoesNotExist()
    }
}
