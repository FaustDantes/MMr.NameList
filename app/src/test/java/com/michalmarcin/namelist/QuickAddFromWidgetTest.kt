package com.michalmarcin.namelist

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.platform.widget.NameListWidget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The widget's add button opens the form; saving leaves the app so the phone shows its home screen (F20). */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class QuickAddFromWidgetTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private fun launch(action: String?): ActivityScenario<MainActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext<Application>(), MainActivity::class.java)
        if (action != null) intent.action = action
        return ActivityScenario.launch(intent)
    }

    @Test
    fun widgetIntentOpensTheFormWithTheNameField() {
        launch(NameListWidget.ACTION_ADD_PERSON).use {
            compose.waitUntilAtLeastOneExists(hasTestTag("nameField"), 10_000)
        }
    }

    @Test
    fun savingClosesTheApp() {
        launch(NameListWidget.ACTION_ADD_PERSON).use { scenario ->
            compose.waitUntilAtLeastOneExists(hasTestTag("nameField"), 10_000)
            compose.onNodeWithTag("nameField").performTextInput("Widget Person")
            compose.onNodeWithTag("save").performClick()
            compose.waitUntil(10_000) {
                var finishing = false
                scenario.onActivity { finishing = it.isFinishing }
                finishing
            }
        }
    }

    @Test
    fun normalLaunchShowsThePeopleList() {
        launch(null).use {
            compose.waitUntilAtLeastOneExists(hasTestTag("addPerson"), 10_000)
        }
    }
}
