package com.michalmarcin.namelist.ui.person

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.PersonDraft
import com.michalmarcin.namelist.data.repo.SortMode
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.Coordinates
import com.michalmarcin.namelist.platform.LocationProvider
import com.michalmarcin.namelist.platform.PhotoStore
import com.michalmarcin.namelist.testutil.MainDispatcherRule
import com.michalmarcin.namelist.testutil.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Most used feature: adding and editing a person. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PersonEditViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var people: PeopleRepository
    private lateinit var tabs: TabsRepository
    private lateinit var photos: PhotoStore
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        db = AppDatabase.build(context, inMemory = true)
        photos = PhotoStore(context)
        people = PeopleRepository(db.personDao(), deletePhoto = photos::delete)
        tabs = TabsRepository(db.tabDao())
    }

    @After
    fun tearDown() {
        store.clear()
        db.close()
    }

    /** Creates the ViewModel the way navigation does, in a store we can clear (= user leaves the screen). */
    private fun viewModel(id: Long = 0, tabId: Long = 0): PersonEditViewModel {
        val factory = viewModelFactory {
            initializer {
                PersonEditViewModel(
                    people,
                    tabs,
                    photos,
                    LocationProvider(context),
                    SavedStateHandle(mapOf("id" to id, "tabId" to tabId)),
                )
            }
        }
        return ViewModelProvider.create(store, factory)[PersonEditViewModel::class]
    }

    private fun everyone() = runBlocking { people.observePeople("", SortMode.MANUAL).first() }

    private fun jpeg(): Uri {
        val file = File(context.cacheDir, "pick-${System.nanoTime()}.jpg")
        file.outputStream().use { Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun newPersonIsSavedTrimmedInThePreselectedTab() {
        val work = runBlocking { tabs.add("Work") }
        val vm = viewModel(tabId = work)
        vm.uiState.await { it.tabs.isNotEmpty() }
        assertEquals(work, vm.uiState.value.tabId)

        vm.onNameChange("  Anna Nováková ")
        vm.onNotesChange("Met at the tram stop ")
        vm.onPlaceChange(" Brno, Morava ")
        vm.save()
        vm.uiState.await { it.isSaved }

        val anna = everyone().single()
        assertEquals("Anna Nováková", anna.name)
        assertEquals("Met at the tram stop", anna.notes)
        assertEquals("Brno, Morava", anna.metCity)
        assertEquals(work, anna.tabId)
    }

    @Test
    fun blankNameShowsAnErrorAndSavesNothing() {
        val vm = viewModel()
        vm.onNameChange("   ")
        vm.save()
        assertTrue(vm.uiState.value.showNameError)
        assertFalse(vm.uiState.value.isSaved)
        assertTrue(everyone().isEmpty())

        vm.onNameChange("A")
        assertFalse("typing clears the error", vm.uiState.value.showNameError)
    }

    @Test
    fun editLoadsThePersonAndKeepsWhatWasNotChanged() {
        val id = runBlocking {
            people.save(0, PersonDraft("Peter", "Plays badminton", metPlace = "Praha", metLat = 50.08, metLon = 14.42))
        }
        val vm = viewModel(id = id)
        val loaded = vm.uiState.await { !it.isLoading }
        assertFalse(loaded.isNew)
        assertEquals("Peter", loaded.name)
        assertEquals("Praha", loaded.metPlace)
        assertEquals(Coordinates(50.08, 14.42), loaded.coordinates)

        vm.onNameChange("Peter Horváth")
        vm.save()
        vm.uiState.await { it.isSaved }

        val peter = everyone().single()
        assertEquals("Peter Horváth", peter.name)
        assertEquals("Plays badminton", peter.notes)
        assertEquals(50.08, peter.metLat ?: 0.0, 1e-9)
    }

    @Test
    fun clearingCoordinatesRemovesThemOnSave() {
        val id = runBlocking { people.save(0, PersonDraft("Eva", metLat = 1.0, metLon = 2.0)) }
        val vm = viewModel(id = id)
        vm.uiState.await { !it.isLoading }
        vm.clearCoordinates()
        vm.save()
        vm.uiState.await { it.isSaved }
        assertNull(everyone().single().metLat)
    }

    @Test
    fun leavingWithoutSavingDeletesTheNewPhoto() {
        val vm = viewModel()
        vm.onPhotoPicked(jpeg())
        val photo = vm.uiState.await { it.photoFile != null }.photoFile!!
        assertTrue(photos.file(photo).exists())

        store.clear() // the user closes the screen without Save

        assertFalse(photos.file(photo).exists())
        assertTrue(everyone().isEmpty())
    }

    @Test
    fun savedPhotoStaysAndAReplacedPickIsDeleted() {
        val vm = viewModel()
        vm.onNameChange("Anna")
        vm.onPhotoPicked(jpeg())
        val first = vm.uiState.await { it.photoFile != null }.photoFile!!
        vm.onPhotoPicked(jpeg())
        val second = vm.uiState.await { it.photoFile != null && it.photoFile != first }.photoFile!!
        assertFalse("first pick was replaced before saving", photos.file(first).exists())

        vm.save()
        vm.uiState.await { it.isSaved }
        store.clear()

        assertTrue(photos.file(second).exists())
        assertEquals(second, everyone().single().photoFile)
    }

    @Test
    fun locationDeniedShowsAMessageAndTypingStillWorks() {
        val vm = viewModel()
        vm.onLocationDenied()
        assertEquals(LocationMessage.DENIED, vm.uiState.value.locationMessage)
        vm.onPlaceChange("Lisboa")
        assertEquals("Lisboa", vm.uiState.value.metPlace)
    }
}
