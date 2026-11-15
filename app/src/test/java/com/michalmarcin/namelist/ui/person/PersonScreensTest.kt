package com.michalmarcin.namelist.ui.person

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class PersonScreensTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun detailShowsPersonAndDeletesAfterConfirm() {
        var deleted = false
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(
                    state = PersonDetailUiState(person = SampleData.anna, isLoading = false),
                    onBack = {},
                    onEdit = {},
                    onDelete = { deleted = true },
                )
            }
        }
        compose.onNodeWithText("Anna Nováková").assertIsDisplayed()
        compose.onNodeWithTag("delete").performClick()
        compose.onNodeWithTag("confirmDelete").performClick()
        assertTrue(deleted)
    }

    @Test
    fun meHasNoDeleteButton() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(
                    state = PersonDetailUiState(person = SampleData.anna.copy(name = "Me", isSelf = true), isLoading = false),
                    onBack = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
        compose.onNodeWithTag("delete").assertDoesNotExist()
    }

    @Test
    fun editShowsNameErrorAndSaves() {
        var saves = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(
                    state = PersonEditUiState(showNameError = true),
                    onNameChange = {},
                    onNotesChange = {},
                    onSave = { saves++ },
                    onClose = {},
                )
            }
        }
        compose.onNodeWithText("Enter a name.").assertIsDisplayed()
        compose.onNodeWithTag("save").performClick()
        assertEquals(1, saves)
    }
}
