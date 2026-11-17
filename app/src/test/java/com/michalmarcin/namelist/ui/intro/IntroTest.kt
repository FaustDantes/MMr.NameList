package com.michalmarcin.namelist.ui.intro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.SettingsStore
import com.michalmarcin.namelist.testutil.MainDispatcherRule
import com.michalmarcin.namelist.ui.people.PeopleViewModel
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** F34: the welcome with four points, shown once until the user confirms it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class IntroTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theIntroShowsFourPointsAndOneButton() {
        var confirmed = 0
        compose.setContent { NameListTheme(dynamicColor = false) { IntroContent(onConfirm = { confirmed++ }) } }
        compose.onNodeWithText("Welcome to Name List").assertIsDisplayed()
        listOf("Save people in seconds", "Know what to talk about", "Find anyone again", "Private by design").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("introConfirm").performScrollTo().performClick()
        assertEquals(1, confirmed)
    }
}

/** F34: the "seen" flag lives in the settings, so the intro stays away after a restart. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class IntroOnceTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun theIntroShowsUntilConfirmedAndNeverAgain() {
        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val db = AppDatabase.build(context, inMemory = true)
        try {
            val settings = SettingsStore(context)
            val first = PeopleViewModel(PeopleRepository(db.personDao()), TabsRepository(db.tabDao()), SavedStateHandle(), settings)
            assertTrue(first.showIntro.value)
            first.dismissIntro()
            assertFalse(first.showIntro.value)
            // A new start of the app reads the saved choice.
            val again = PeopleViewModel(PeopleRepository(db.personDao()), TabsRepository(db.tabDao()), SavedStateHandle(), SettingsStore(context))
            assertFalse(again.showIntro.value)
        } finally {
            db.close()
        }
    }
}
