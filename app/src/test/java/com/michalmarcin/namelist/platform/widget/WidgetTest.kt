package com.michalmarcin.namelist.platform.widget

import android.app.Application
import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.R
import com.michalmarcin.namelist.data.db.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    private val people = listOf(
        Person(id = 1, name = "Me", isSelf = true, createdAt = 900),
        Person(id = 2, name = "Anna Nováková", createdAt = 100),
        Person(id = 3, name = "Peter Horváth", createdAt = 300),
        Person(id = 4, name = "Eva Malá", createdAt = 200),
        Person(id = 5, name = "Old Friend", createdAt = 50),
        Person(id = 6, name = "Archived One", createdAt = 800, archivedAt = 1),
    )

    // ---- content ----

    @Test
    fun newestThreeFirstWithoutMeAndArchive() {
        val content = WidgetContent.from(people)
        assertEquals("Peter Horváth", content.lastAdded)
        assertEquals(listOf("Peter Horváth", "Eva Malá", "Anna Nováková"), content.recentNames)
    }

    @Test
    fun sameCreationTimeUsesTheHigherId() {
        val tied = listOf(Person(id = 1, name = "Alpha", createdAt = 5), Person(id = 2, name = "Beta", createdAt = 5))
        assertEquals("Beta", WidgetContent.from(tied).lastAdded)
    }

    @Test
    fun nobodyGivesAnEmptyContent() {
        val content = WidgetContent.from(emptyList())
        assertNull(content.lastAdded)
        assertEquals(emptyList<String>(), content.recentNames)
    }

    // ---- size ----

    @Test
    fun sizeFollowsTheLauncherSize() {
        assertEquals(WidgetSize.CARD, WidgetSize.forDp(widthDp = 250, heightDp = 110))
        assertEquals(WidgetSize.CARD, WidgetSize.forDp(widthDp = 120, heightDp = 200))
        assertEquals(WidgetSize.STRIP, WidgetSize.forDp(widthDp = 250, heightDp = 70))
        assertEquals(WidgetSize.ICON, WidgetSize.forDp(widthDp = 70, heightDp = 70))
    }

    // ---- views ----

    private fun render(size: WidgetSize, content: WidgetContent) =
        NameListWidget.build(context, size, content).apply(context, FrameLayout(context))

    @Test
    fun cardShowsNamesOfTheNewestPeople() {
        val view = render(WidgetSize.CARD, WidgetContent.from(people))
        assertEquals("Peter Horváth", view.findViewById<TextView>(R.id.widget_tile_1).text.toString())
        assertEquals("Eva Malá", view.findViewById<TextView>(R.id.widget_tile_2).text.toString())
        assertEquals("Anna Nováková", view.findViewById<TextView>(R.id.widget_tile_3).text.toString())
    }

    @Test
    fun cardHidesTilesWhenThereAreFewerPeople() {
        val view = render(WidgetSize.CARD, WidgetContent.from(people.take(2)))
        assertEquals(android.view.View.VISIBLE, view.findViewById<TextView>(R.id.widget_tile_1).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<TextView>(R.id.widget_tile_2).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.widget_recent).visibility)
        // ADR-039: with people, the hint gives way to the names.
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.widget_hint).visibility)
    }

    @Test
    fun cardHidesTheRecentLabelWhenNobodyIsThere() {
        val view = render(WidgetSize.CARD, WidgetContent.from(emptyList()))
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.widget_recent).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.widget_hint).visibility)
    }

    @Test
    fun theSmallWidgetSaysItIsNameList() {
        val view = render(WidgetSize.ICON, WidgetContent.from(people))
        assertEquals("Name List: add a person", view.findViewById<android.view.View>(R.id.widget_add).contentDescription)
    }

    @Test
    fun stripShowsTheNewestName() {
        val view = render(WidgetSize.STRIP, WidgetContent.from(people))
        assertEquals("Last added: Peter Horváth", view.findViewById<TextView>(R.id.widget_subtitle).text.toString())
    }

    @Test
    fun stripShowsTheHintWhenEmpty() {
        val view = render(WidgetSize.STRIP, WidgetContent.from(emptyList()))
        assertEquals("Tap the button to add someone you just met.", view.findViewById<TextView>(R.id.widget_subtitle).text.toString())
    }

    @Test
    fun everySizeHasAnAddButtonWithADescription() {
        for (size in WidgetSize.entries) {
            val add = render(size, WidgetContent.from(people)).findViewById<android.view.View>(R.id.widget_add)
            // The 1 x 1 widget is only the app icon, so it names the app too (ADR-039).
            val expected = if (size == WidgetSize.ICON) "Name List: add a person" else "Add person"
            assertEquals(expected, add.contentDescription.toString())
            assertTrue(add.hasOnClickListeners())
        }
    }
}
