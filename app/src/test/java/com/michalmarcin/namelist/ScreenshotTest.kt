package com.michalmarcin.namelist

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.michalmarcin.namelist.testutil.SampleData
import com.michalmarcin.namelist.ui.people.PeopleScreen
import com.michalmarcin.namelist.ui.people.PeopleUiState
import com.michalmarcin.namelist.ui.person.PersonDetailScreen
import com.michalmarcin.namelist.ui.person.PersonDetailUiState
import com.michalmarcin.namelist.ui.person.PersonEditScreen
import com.michalmarcin.namelist.ui.person.PersonEditUiState
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots for the milestone report (docs/milestones.md). Fictional data, fallback palette.
 * Run with ./gradlew recordRoborazziDebug. Output: app/build/outputs/roborazzi/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    private fun shot(name: String, dark: Boolean, content: @Composable () -> Unit) =
        captureRoboImage("build/outputs/roborazzi/${name}_${if (dark) "dark" else "light"}.png") {
            NameListTheme(darkTheme = dark, dynamicColor = false) { content() }
        }

    private val list = PeopleUiState(people = SampleData.people, isLoading = false)

    @Test fun peopleListLight() = shot("people_list", false) { PeopleScreen(list, {}, {}, {}, {}) }

    @Test fun peopleListDark() = shot("people_list", true) { PeopleScreen(list, {}, {}, {}, {}) }

    @Test fun peopleEmptyLight() = shot("people_empty", false) {
        PeopleScreen(PeopleUiState(isLoading = false), {}, {}, {}, {})
    }

    @Test fun peopleSearchLight() = shot("people_search", false) {
        PeopleScreen(PeopleUiState(query = "nov", people = SampleData.people.take(1), isLoading = false), {}, {}, {}, {})
    }

    @Test fun personDetailLight() = shot("person_detail", false) { detail() }

    @Test fun personDetailDark() = shot("person_detail", true) { detail() }

    @Test fun personEditLight() = shot("person_edit", false) {
        PersonEditScreen(
            PersonEditUiState(isNew = false, name = SampleData.anna.name, notes = SampleData.anna.notes),
            {}, {}, {}, {},
        )
    }

    @Composable
    private fun detail() = PersonDetailScreen(PersonDetailUiState(SampleData.anna, isLoading = false), {}, {}, {})
}
