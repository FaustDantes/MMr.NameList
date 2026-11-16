package com.michalmarcin.namelist.ui.relationtypes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** F30: the Settings list of relationship types. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ManageRelationTypesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val deleted = mutableListOf<Long>()

    private fun show(types: List<RelationKind>) = compose.setContent {
        NameListTheme(dynamicColor = false) {
            ManageRelationTypesScreen(
                state = ManageRelationTypesUiState(types = types, isLoading = false),
                onBack = {},
                onAdd = {},
                onRename = { _, _ -> },
                onDelete = { deleted += it },
            )
        }
    }

    @Test
    fun builtInTypesShowTheirLabelAndOnlySomeCanBeDeleted() {
        show(RelationKind.builtIns)
        compose.onNodeWithText("Friend").assertIsDisplayed()
        compose.onNodeWithTag("deleteType-${RelationType.FRIEND.seedId}").assertIsDisplayed()
        compose.onNodeWithTag("deleteType-${RelationType.PARENT.seedId}").assertDoesNotExist()
        compose.onNodeWithTag("deleteType-${RelationType.CHILD.seedId}").assertDoesNotExist()
        compose.onNodeWithTag("deleteType-${RelationType.OTHER.seedId}").assertDoesNotExist()
        compose.onNodeWithTag("addRelationType").assertIsDisplayed()
    }

    @Test
    fun deleteAfterConfirm() {
        show(RelationKind.builtIns + RelationKind(id = 10, name = "Mentor", sortIndex = 9))
        compose.onNodeWithTag("deleteType-10").performClick()
        compose.onNodeWithText("Relationships of this type become Other.").assertIsDisplayed()
        compose.onNodeWithTag("confirmDeleteType").performClick()
        assertEquals(listOf(10L), deleted)
    }
}
