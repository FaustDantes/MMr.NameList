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
import org.junit.Assert.assertNotNull
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
            PersonDraft(
                "Anna Nováková", "Brno tram", "anna.jpg", work, "Brno, Morava", 49.1951, 16.6068, firstMetOn = 19_000,
                nativeLanguage = "Slovak", birthDate = 7_000, education = "MSc", currentWork = "Nurse", askAbout = "Her dog",
            ),
        )
        val peter = people.save(0, "Peter", "")
        people.archive(listOf(peter))
        links.add(anna, peter, RelationType.PARENT)
        val out = ByteArrayOutputStream()
        val summary = backup.export(out)
        assertEquals(BackupSummary(people = 2, tabs = 1, relationships = 1, photos = 1), summary)
        out.toByteArray()
    }

    private fun snapshot() = runBlocking {
        Triple(
            db.personDao().getAll().map { "${it.id}|${it.name}|${it.notes}|${it.photoFile}|${it.tabId}|${it.metCity}|${it.metLat}|${it.firstMetOn}|${it.nativeLanguage}|${it.birthDate}|${it.education}|${it.currentWork}|${it.askAbout}|${it.lastUpdatedAt}|${it.archivedAt}|${it.isSelf}" },
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
    fun moreDetailsAndTheArchiveSurviveExportAndImport() = runBlocking {
        val file = seed()
        db.personDao().getAll().forEach { people.delete(it.id) }
        backup.import(ByteArrayInputStream(file))
        val all = db.personDao().getAll()
        val anna = all.first { it.name == "Anna Nováková" }
        assertEquals(listOf("Slovak", "MSc", "Nurse", "Her dog"), listOf(anna.nativeLanguage, anna.education, anna.currentWork, anna.askAbout))
        assertEquals(7_000L, anna.birthDate)
        assertNotNull(anna.lastUpdatedAt)
        assertNotNull("the archive stamp is kept", all.first { it.name == "Peter" }.archivedAt)
    }

    @Test
    fun backupFromVersion1_0HasNoMoreDetailsAndIsNotArchived() = runBlocking {
        val json = """{"format":"mmr.namelist.backup","version":1,"tabs":[],"relationships":[],"people":[{"id":5,"name":"Eve"}]}"""
        backup.import(ByteArrayInputStream(zip(mapOf("backup.json" to json.toByteArray()))))
        val eve = db.personDao().getAll().first { it.name == "Eve" }
        assertNull(eve.education)
        assertNull(eve.archivedAt)
        assertNull(eve.lastUpdatedAt)
    }

    // ---- F17: password encryption ----

    private fun encryptedSeed(password: String): ByteArray = runBlocking {
        seed() // fills the database
        val out = ByteArrayOutputStream()
        backup.export(out, password.toCharArray())
        out.toByteArray()
    }

    @Test
    fun encryptedBackupRoundTripsWithThePassword() = runBlocking {
        val file = encryptedSeed("open sesame")
        assertTrue(BackupCrypto.isEncrypted(file))
        assertFalse("names must not be readable", String(file, Charsets.ISO_8859_1).contains("Anna"))
        val before = snapshot()

        people.save(0, "Someone new", "")
        val summary = backup.import(ByteArrayInputStream(file), "open sesame".toCharArray())

        assertEquals(2, summary.people)
        assertEquals(before, snapshot())
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), photos.file("anna.jpg").readBytes())
    }

    @Test
    fun encryptedBackupWithoutAPasswordAsksForOne() = runBlocking {
        val file = encryptedSeed("open sesame")
        val before = snapshot()
        try {
            backup.import(ByteArrayInputStream(file))
            fail("Expected the password to be required")
        } catch (e: BackupPasswordException) {
            assertEquals(BackupPasswordException.Reason.REQUIRED, e.reason)
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun wrongPasswordChangesNothing() = runBlocking {
        val file = encryptedSeed("open sesame")
        people.save(0, "Someone new", "")
        val before = snapshot()
        try {
            backup.import(ByteArrayInputStream(file), "open sesamE".toCharArray())
            fail("Expected a wrong-password error")
        } catch (e: BackupPasswordException) {
            assertEquals(BackupPasswordException.Reason.WRONG, e.reason)
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun aDamagedEncryptedBackupChangesNothing() = runBlocking {
        val file = encryptedSeed("open sesame")
        val before = snapshot()
        val bad = file.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x55).toByte() }
        try {
            backup.import(ByteArrayInputStream(bad), "open sesame".toCharArray())
            fail("Expected an error")
        } catch (_: BackupPasswordException) {
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun aPlainBackupStillImportsEvenWhenAPasswordIsGiven() = runBlocking {
        val file = seed()
        val before = snapshot()
        people.save(0, "Someone new", "")
        backup.import(ByteArrayInputStream(file), "unused password".toCharArray())
        assertEquals(before, snapshot())
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
