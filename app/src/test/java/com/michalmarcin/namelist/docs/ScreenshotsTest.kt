package com.michalmarcin.namelist.docs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.repo.RelationView
import com.michalmarcin.namelist.platform.widget.NameListWidget
import com.michalmarcin.namelist.platform.widget.WidgetContent
import com.michalmarcin.namelist.platform.widget.WidgetSize
import com.michalmarcin.namelist.ui.components.LocalPhotoDirectory
import com.michalmarcin.namelist.ui.people.PeopleScreen
import com.michalmarcin.namelist.ui.people.PeopleUiState
import com.michalmarcin.namelist.ui.person.PersonDetailScreen
import com.michalmarcin.namelist.ui.person.PersonDetailUiState
import com.michalmarcin.namelist.ui.person.PersonEditScreen
import com.michalmarcin.namelist.ui.person.PersonEditUiState
import com.michalmarcin.namelist.ui.settings.SettingsScreen
import com.michalmarcin.namelist.ui.settings.SettingsUiState
import com.michalmarcin.namelist.ui.theme.NameListTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Screenshots for the README (owner request 2026-11-17, ADR-040). Not a check: it draws the real
 * screens with fictional people and writes PNG files to app/build/screenshots. CI copies them to the
 * ci-outputs branch, from where they go to docs/screenshots. The "photos" are drawn shapes, not people.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-xhdpi")
class ScreenshotsTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val out = File("build/screenshots").apply { mkdirs() }
    private val photos = File(context.filesDir, "shots").apply { mkdirs() }

    private val tags = listOf(Tab(1, "Work", 1), Tab(2, "Family", 2), Tab(3, "Gym", 3))
    private val people = listOf(
        Person(id = 2, name = "Anna Nováková", askAbout = "Her dog Max and the Brno half marathon", photoFile = "anna.png", sortIndex = 1),
        Person(id = 3, name = "Peter Horváth", askAbout = "Building a cabin near Banská Štiavnica", sortIndex = 2),
        Person(id = 4, name = "Lucia Kováčová", askAbout = "New job at the Vienna design studio", sortIndex = 3),
        Person(id = 5, name = "Tomáš Dvořák", askAbout = "Climbing trip to Arco in September", sortIndex = 4),
        Person(id = 6, name = "Eva Svobodová", askAbout = "Twins started school this year", sortIndex = 5),
        Person(id = 7, name = "Martin Novák", askAbout = "Restoring a 1972 Škoda 110 R", photoFile = "martin.png", sortIndex = 6),
        Person(id = 8, name = "Jana Procházková", askAbout = "Moving to Ostrava in spring", sortIndex = 7),
    )

    /** A soft, abstract stand-in for a photo: a gradient with a light head and shoulders shape. */
    private fun fakePhoto(name: String, top: Int, bottom: Int) {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        paint.color = Color.argb(170, 255, 255, 255)
        canvas.drawCircle(size / 2f, size * 0.38f, size * 0.17f, paint)
        canvas.drawOval(size * 0.18f, size * 0.62f, size * 0.82f, size * 1.25f, paint)
        File(photos, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shoot(file: String, dark: Boolean = false, content: @Composable () -> Unit) {
        fakePhoto("anna.png", Color.rgb(120, 140, 168), Color.rgb(70, 86, 110))
        fakePhoto("martin.png", Color.rgb(118, 160, 146), Color.rgb(62, 98, 86))
        compose.setContent {
            CompositionLocalProvider(LocalPhotoDirectory provides photos) {
                NameListTheme(darkTheme = dark, dynamicColor = false) { content() }
            }
        }
        // Give the image loader time to decode the photos.
        repeat(6) {
            compose.waitForIdle()
            Thread.sleep(250)
        }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(out, file).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun peopleLight() = shoot("01-people-light.png") {
        PeopleScreen(PeopleUiState(people = people, tabs = tags, isLoading = false), {}, {}, {}, {})
    }

    @Test
    fun peopleDark() = shoot("02-people-dark.png", dark = true) {
        PeopleScreen(PeopleUiState(people = people, tabs = tags, isLoading = false), {}, {}, {}, {})
    }

    @Test
    fun personDetail() = shoot("03-person.png") {
        val anna = people.first().copy(
            nativeLanguage = "Slovak",
            currentWork = "Nurse, FN Brno",
            metCity = "Brno tram, line 4",
            firstMetOn = 20_161,
            notes = "Likes hiking and strong coffee.",
        )
        val types = RelationKind.builtIns
        PersonDetailScreen(
            state = PersonDetailUiState(
                person = anna,
                tabNames = listOf("Work", "Gym"),
                relations = listOf(
                    RelationView(1, people[1], types.first { it.builtIn == RelationType.SIBLING }),
                    RelationView(2, people[5], types.first { it.builtIn == RelationType.COLLEAGUE }),
                ),
                relationTypes = types,
                isLoading = false,
            ),
            onBack = {},
            onEdit = {},
            onDelete = {},
        )
    }

    @Test
    fun editPerson() = shoot("04-edit.png") {
        PersonEditScreen(
            PersonEditUiState(
                isNew = false,
                name = "Anna Nováková",
                askAbout = "Her dog Max, a border collie. Training for the Brno half marathon.",
                photoFile = "anna.png",
                tabs = tags,
                tabIds = setOf(1, 3),
                metPlace = "Brno tram, line 4",
                firstMetOn = 20_161,
            ),
            {}, {}, {}, {},
        )
    }

    @Test
    fun settings() = shoot("05-settings.png") {
        SettingsScreen(
            state = SettingsUiState(),
            versionName = "1.3.0",
            onBack = {},
            onThemeChange = {},
            onExport = {},
            onImport = {},
        )
    }

    @Test
    fun widgets() {
        val density = context.resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).toInt()
        val content = WidgetContent.from(people.mapIndexed { i, p -> p.copy(createdAt = i.toLong()) })
        val width = px(360)
        val heights = listOf(WidgetSize.ICON to 76, WidgetSize.STRIP to 80, WidgetSize.CARD to 170)
        val gap = px(16)
        val total = heights.sumOf { px(it.second) } + gap * (heights.size + 1)
        val bitmap = Bitmap.createBitmap(width + gap * 2, total, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // A neutral home-screen color behind the widgets.
        canvas.drawColor(Color.rgb(220, 227, 236))
        var y = gap
        heights.forEach { (size, heightDp) ->
            val w = if (size == WidgetSize.ICON) px(76) else width
            val h = px(heightDp)
            val view: View = NameListWidget.build(context, size, content).apply(context, FrameLayout(context))
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            canvas.save()
            canvas.translate(gap.toFloat(), y.toFloat())
            view.draw(canvas)
            canvas.restore()
            y += h + gap
        }
        File(out, "06-widgets.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
