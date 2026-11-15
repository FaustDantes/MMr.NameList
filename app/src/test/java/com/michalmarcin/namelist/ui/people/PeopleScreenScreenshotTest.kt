package com.michalmarcin.namelist.ui.people

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots for the milestone report (docs/milestones.md).
 * Run with ./gradlew recordRoborazziDebug. Output: app/build/outputs/roborazzi/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PeopleScreenScreenshotTest {

    @Test
    fun peopleEmptyLight() = captureRoboImage("$DIR/people_empty_light.png") {
        NameListTheme(darkTheme = false, dynamicColor = false) { PeopleScreen() }
    }

    @Test
    fun peopleEmptyDark() = captureRoboImage("$DIR/people_empty_dark.png") {
        NameListTheme(darkTheme = true, dynamicColor = false) { PeopleScreen() }
    }

    private companion object {
        const val DIR = "build/outputs/roborazzi"
    }
}
