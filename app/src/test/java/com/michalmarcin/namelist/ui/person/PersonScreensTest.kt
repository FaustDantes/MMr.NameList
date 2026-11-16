package com.michalmarcin.namelist.ui.person

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.RelationView
import com.michalmarcin.namelist.platform.Coordinates
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

    @Test
    fun photoButtonsCallBackAndRemoveShowsOnlyWithPhoto() {
        var camera = 0
        var gallery = 0
        var removed = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(
                    state = PersonEditUiState(name = "Anna", photoFile = "a.jpg"),
                    onNameChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onClose = {},
                    onCamera = { camera++ },
                    onGallery = { gallery++ },
                    onRemovePhoto = { removed++ },
                )
            }
        }
        compose.onNodeWithTag("camera").performClick()
        compose.onNodeWithTag("gallery").performClick()
        compose.onNodeWithTag("removePhoto").performClick()
        assertEquals(listOf(1, 1, 1), listOf(camera, gallery, removed))
    }

    @Test
    fun noRemoveButtonWithoutPhoto() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(name = "Anna"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("removePhoto").assertDoesNotExist()
    }

    @Test
    fun importErrorIsShown() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(name = "Anna", photoMessage = PhotoMessage.IMPORT_FAILED), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("That photo could not be loaded. Try another one.").assertIsDisplayed()
    }

    @Test
    fun detailShowsTabLabel() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(SampleData.anna, tabNames = listOf("Work"), isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun withoutTabsTheFormStillOffersToAddOne() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(name = "Anna"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("tabField").assertIsDisplayed()
        compose.onNodeWithTag("addTabChip").assertIsDisplayed()
        compose.onNodeWithText("Add tab").assertIsDisplayed()
    }

    @Test
    fun nameTabAndConversationSparkAreOnTopAndTheRestIsCollapsed() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(name = "Anna", tabs = SampleData.tabs), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("nameField").assertIsDisplayed()
        compose.onNodeWithTag("askAboutField").assertIsDisplayed()
        compose.onNodeWithTag("tabField").assertIsDisplayed()
        compose.onNodeWithTag("detailsToggle").assertIsDisplayed()
        listOf("placeField", "firstMetField", "birthField", "nativeLanguageField", "educationField", "currentWorkField", "notesField")
            .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }

        compose.onNodeWithTag("detailsToggle").performClick()
        listOf("placeField", "firstMetField", "birthField", "nativeLanguageField", "educationField", "currentWorkField", "notesField")
            .forEach { compose.onNodeWithTag(it).assertExists() }

        compose.onNodeWithTag("detailsToggle").performClick()
        compose.onNodeWithTag("educationField").assertDoesNotExist()
    }

    @Test
    fun moreDetailsShowTheirValuesAndReportChanges() {
        val changes = mutableListOf<String>()
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(
                    state = PersonEditUiState(
                        name = "Anna",
                        nativeLanguage = "Slovak",
                        education = "MSc",
                        currentWork = "Nurse",
                        askAbout = "Her dog",
                        birthDate = 7_000,
                    ),
                    onNameChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onClose = {},
                    onNativeLanguageChange = { changes += "lang:$it" },
                    onEducationChange = { changes += "edu:$it" },
                    onCurrentWorkChange = { changes += "work:$it" },
                    onAskAboutChange = { changes += "ask:$it" },
                    onBirthDateChange = { changes += "birth:$it" },
                )
            }
        }
        compose.onNodeWithTag("detailsToggle").performClick()
        compose.onNodeWithText("Slovak").assertExists()
        compose.onNodeWithText("MSc").assertExists()
        compose.onNodeWithText("Nurse").assertExists()
        compose.onNodeWithText("Her dog").assertExists()
        compose.onNodeWithText(formatEpochDay(7_000)).assertExists()
        compose.onNodeWithTag("nativeLanguageField").performTextInput("!")
        compose.onNodeWithTag("clearBirth").performClick()
        assertEquals(listOf("lang:!Slovak", "birth:null"), changes)
    }

    @Test
    fun detailShowsTheMoreDetailsAndTheLastUpdate() {
        val updated = 1_700_000_000_000L
        val person = SampleData.anna.copy(
            nativeLanguage = "Slovak",
            education = "MSc in design",
            currentWork = "Nurse at a clinic",
            askAbout = "Her trip to Japan",
            birthDate = 7_000,
            lastUpdatedAt = updated,
        )
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(person, isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithTag("factsSection").assertExists()
        listOf("Slovak", "MSc in design", "Nurse at a clinic", "Her trip to Japan", formatEpochDay(7_000))
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithTag("lastUpdated").assertExists()
        compose.onNodeWithText("Last updated: " + formatDateTime(updated)).assertExists()
    }

    @Test
    fun detailWithoutMoreDetailsHasNoFactsSection() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(SampleData.anna, isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithTag("factsSection").assertDoesNotExist()
    }

    @Test
    fun detailOfAnArchivedPersonShowsSinceWhenAndHowLong() {
        val person = SampleData.anna.copy(archivedAt = System.currentTimeMillis() - 3 * 86_400_000L - 1_000)
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(person, isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithTag("archiveStatus").assertTextContains("in the archive for 3 days", substring = true)
    }

    @Test
    fun newPersonOpensWithTheCursorInTheNameField() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(isNew = true), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("nameField").assertIsFocused()
    }

    @Test
    fun editingAPersonDoesNotStealTheFocus() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(isNew = false, name = "Anna"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("nameField").assertIsNotFocused()
    }

    @Test
    fun emptyFirstMetDateShowsTodayHintAndNoClearButton() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(name = "Anna"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("detailsToggle").performClick()
        compose.onNodeWithText("Empty: today is used").assertIsDisplayed()
        compose.onNodeWithTag("pickFirstMet").assertIsDisplayed()
        compose.onNodeWithTag("clearFirstMet").assertDoesNotExist()
    }

    @Test
    fun filledFirstMetDateShowsAndClears() {
        val changes = mutableListOf<Long?>()
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(
                    state = PersonEditUiState(name = "Anna", firstMetOn = 20_407),
                    onNameChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onClose = {},
                    onFirstMetChange = { changes += it },
                )
            }
        }
        compose.onNodeWithTag("detailsToggle").performClick()
        compose.onNodeWithText(formatEpochDay(20_407)).assertIsDisplayed()
        compose.onNodeWithTag("clearFirstMet").performClick()
        assertEquals(listOf<Long?>(null), changes)
    }

    @Test
    fun detailShowsTheFirstMetDate() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(
                    PersonDetailUiState(SampleData.anna.copy(firstMetOn = 20_407), isLoading = false),
                    {}, {}, {},
                )
            }
        }
        compose.onNodeWithText("First met: " + formatEpochDay(20_407)).assertIsDisplayed()
    }

    @Test
    fun gpsButtonAndCoordinatesWithClear() {
        var located = 0
        var cleared = 0
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(
                    state = PersonEditUiState(name = "Anna", metPlace = "Brno", coordinates = Coordinates(49.1951, 16.6068)),
                    onNameChange = {},
                    onNotesChange = {},
                    onSave = {},
                    onClose = {},
                    onUseLocation = { located++ },
                    onClearCoordinates = { cleared++ },
                )
            }
        }
        compose.onNodeWithTag("detailsToggle").performClick()
        compose.onNodeWithText("GPS 49.1951, 16.6068").assertIsDisplayed()
        compose.onNodeWithTag("useLocation").performClick()
        compose.onNodeWithTag("clearCoordinates").performClick()
        assertEquals(listOf(1, 1), listOf(located, cleared))
    }

    @Test
    fun locationDeniedMessage() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonEditScreen(PersonEditUiState(name = "Anna", locationMessage = LocationMessage.DENIED), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("detailsToggle").performClick()
        compose.onNodeWithText("Location is not allowed. You can still type the place.").assertIsDisplayed()
    }

    @Test
    fun detailShowsPlaceAndMapButtonOnlyWithCoordinates() {
        var opened = 0
        val withGps = SampleData.anna.copy(metCity = "Brno", metLat = 49.1951, metLon = 16.6068)
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(withGps, isLoading = false), {}, {}, {}, onOpenMap = { opened++ })
            }
        }
        compose.onNodeWithTag("placeSection").assertIsDisplayed()
        compose.onNodeWithTag("openMap").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun detailWithoutPlaceHasNoPlaceSection() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(SampleData.anna.copy(metCity = null), isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithTag("placeSection").assertDoesNotExist()
    }

    @Test
    fun relationshipsSectionShowsLinksAndRemoves() {
        val peter = SampleData.people[1]
        var removed = -1L
        var opened = -1L
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(
                    PersonDetailUiState(
                        SampleData.anna,
                        relations = listOf(RelationView(9, peter, RelationType.SIBLING)),
                        isLoading = false,
                    ),
                    {}, {}, {},
                    onOpenPerson = { opened = it },
                    onRemoveRelationship = { removed = it },
                )
            }
        }
        compose.onNodeWithText("Sibling").assertIsDisplayed()
        compose.onNodeWithText(peter.name).performClick()
        assertEquals(peter.id, opened)
        compose.onNodeWithTag("relationMenu-9").performClick()
        compose.onNodeWithTag("removeRelation").performClick()
        assertEquals(9L, removed)
    }

    @Test
    fun emptyRelationshipsText() {
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                PersonDetailScreen(PersonDetailUiState(SampleData.anna, isLoading = false), {}, {}, {})
            }
        }
        compose.onNodeWithText("No relationships yet.").assertExists()
        compose.onNodeWithTag("addRelationship").assertExists()
    }

    @Test
    fun addRelationshipPicksTypeThenPerson() {
        var added: Pair<Long, RelationType>? = null
        compose.setContent {
            NameListTheme(dynamicColor = false) {
                AddRelationshipContent(
                    personName = "Anna",
                    candidates = SampleData.people.drop(1),
                    onAdd = { id, type -> added = id to type },
                )
            }
        }
        compose.onNodeWithTag("type-PARENT").performClick()
        compose.onNodeWithText("Tap the person who is Anna's parent.").assertIsDisplayed()
        compose.onNodeWithTag("candidate-3").performClick()
        assertEquals(3L to RelationType.PARENT, added)
    }
}
