package com.michalmarcin.namelist.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.PersonDraft
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackupTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var photos: PhotoStore
    private lateinit var backup: BackupManager
    private lateinit var people: PeopleRepository
    private lateinit var tabs: TabsRepository
    private lateinit var links: RelationshipsRepository

    @Before
    fun setUp() {
        db = AppDatabase.build(context, inMemory = true)
        photos = PhotoStore(context)
        photos.directory.listFiles()?.forEach { it.delete() }
        backup = BackupManager(context, db, photos)
        people = PeopleRepository(db.personDao(), deletePhoto = photos::delete)
        tabs = TabsRepository(db.tabDao())
        links = RelationshipsRepository(db.relationshipDao(), db.personDao())
    }

    @After
    fun tearDown() = db.close()

    private fun seed(): ByteArray = runBlocking {
        photos.file("anna.jpg").writeBytes(byteArrayOf(1, 2, 3, 4))
        val work = tabs.add("Work")
        val anna = people.save(
            0,
            PersonDraft("Anna Nováková", "Brno tram", "anna.jpg", work, "Brno, Morava", 49.1951, 16.6068, firstMetOn = 19_000),
        )
        val peter = people.save(0, "Peter", "")
        links.add(anna, peter, RelationType.PARENT)
        val out = ByteArrayOutputStream()
        val summary = backup.export(out)
        assertEquals(BackupSummary(people = 2, tabs = 1, relationships = 1, photos = 1), summary)
        out.toByteArray()
    }

    private fun snapshot() = runBlocking {
        Triple(
            db.personDao().getAll().map { "${it.id}|${it.name}|${it.notes}|${it.photoFile}|${it.tabId}|${it.metCity}|${it.metLat}|${it.firstMetOn}|${it.isSelf}" },
            db.tabDao().getAll(),
            db.relationshipDao().getAll(),
        )
    }

    @Test
    fun exportThenImportRestoresEverything() = runBlocking {
        val file = seed()
        val before = snapshot()

        // Change everything after the export.
        people.save(0, "Someone new", "")
        tabs.add("Family")
        db.personDao().getAll().first { it.name == "Anna Nováková" }.let { people.delete(it.id) }
        assertFalse(photos.file("anna.jpg").exists())
        photos.file("orphan.jpg").writeBytes(byteArrayOf(9))

        val summary = backup.import(ByteArrayInputStream(file))

        assertEquals(2, summary.people)
        assertEquals(before, snapshot())
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), photos.file("anna.jpg").readBytes())
        assertFalse("unused photo removed", photos.file("orphan.jpg").exists())
        assertEquals(1, db.personDao().getAll().count { it.isSelf })
    }

    @Test
    fun notAZipChangesNothing() = runBlocking {
        seed()
        val before = snapshot()
        try {
            backup.import(ByteArrayInputStream("hello".toByteArray()))
            fail("Expected BackupException")
        } catch (_: BackupException) {
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun otherZipChangesNothing() = runBlocking {
        seed()
        val before = snapshot()
        val zip = zip(mapOf("backup.json" to """{"format":"something.else","version":1}""".toByteArray()))
        try {
            backup.import(ByteArrayInputStream(zip))
            fail("Expected BackupException")
        } catch (_: BackupException) {
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun photoPathsCannotEscapeThePhotoFolder() = runBlocking {
        val json = """
            {"format":"mmr.namelist.backup","version":1,"tabs":[],"relationships":[],
             "people":[{"id":1,"name":"Me","isSelf":true},{"id":2,"name":"Eve","photoFile":"../evil.jpg"}]}
        """.trimIndent()
        val zip = zip(
            mapOf(
                "backup.json" to json.toByteArray(),
                "photos/../evil.jpg" to byteArrayOf(6, 6, 6),
                "photos/.." to byteArrayOf(6),
            ),
        )
        backup.import(ByteArrayInputStream(zip))
        assertFalse(File(photos.directory.parentFile, "evil.jpg").exists())
        assertEquals(null, db.personDao().getAll().first { it.name == "Eve" }.photoFile)
    }

    @Test
    fun firstMetDateSurvivesExportAndImport() = runBlocking {
        val file = seed()
        db.personDao().getAll().forEach { people.delete(it.id) }
        backup.import(ByteArrayInputStream(file))
        assertEquals(19_000L, db.personDao().getAll().first { it.name == "Anna Nováková" }.firstMetOn)
    }

    @Test
    fun backupFromVersion1_0WithoutTheDateStillImports() = runBlocking {
        val json = """{"format":"mmr.namelist.backup","version":1,"tabs":[],"relationships":[],"people":[{"id":5,"name":"Eve","createdAt":7}]}"""
        backup.import(ByteArrayInputStream(zip(mapOf("backup.json" to json.toByteArray()))))
        assertNull(db.personDao().getAll().first { it.name == "Eve" }.firstMetOn)
    }

    @Test
    fun backupWithoutMeGetsMe() = runBlocking {
        val json = """{"format":"mmr.namelist.backup","version":1,"tabs":[],"relationships":[],"people":[{"id":5,"name":"Eve"}]}"""
        backup.import(ByteArrayInputStream(zip(mapOf("backup.json" to json.toByteArray()))))
        assertTrue(db.personDao().getAll().any { it.isSelf })
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
