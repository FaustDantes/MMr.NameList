package com.michalmarcin.namelist.platform

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.Converters
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.Relationship
import com.michalmarcin.namelist.data.db.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class BackupSummary(val people: Int, val tabs: Int, val relationships: Int, val photos: Int)

/**
 * Export and import of all data as one .zip file (F12). Format: docs/backup-format.md.
 * The file never leaves the phone unless the user moves it.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val photos: PhotoStore,
) {

    suspend fun export(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri) ?: throw BackupException("Cannot open $uri")
        stream.use { export(it) }
    }

    suspend fun import(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw BackupException("Cannot open $uri")
        stream.use { import(it) }
    }

    suspend fun export(output: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val people = db.personDao().getAll()
        val tabs = db.tabDao().getAll()
        val links = db.relationshipDao().getAll()
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("tabs", JSONArray(tabs.map { it.toJson() }))
            .put("people", JSONArray(people.map { it.toJson() }))
            .put("relationships", JSONArray(links.map { it.toJson() }))
        var photoCount = 0
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            people.mapNotNull { it.photoFile }.distinct().forEach { name ->
                val file = photos.file(name)
                if (file.isFile) {
                    zip.putNextEntry(ZipEntry(PHOTO_PREFIX + name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    photoCount++
                }
            }
        }
        BackupSummary(people.count { !it.isSelf }, tabs.size, links.size, photoCount)
    }

    /**
     * Replaces all data with the backup. Nothing changes if the file is not a valid backup:
     * the file is read and checked completely before the database is touched.
     */
    suspend fun import(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "import-${System.nanoTime()}").apply { mkdirs() }
        try {
            var json: String? = null
            val staged = mutableListOf<String>()
            try {
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val name = entry.name
                        when {
                            entry.isDirectory -> Unit
                            name == JSON_ENTRY -> json = zip.readBytes().toString(Charsets.UTF_8)
                            name.startsWith(PHOTO_PREFIX) -> {
                                val photo = name.removePrefix(PHOTO_PREFIX)
                                // Only plain file names: no folders, no "..", nothing outside the staging folder.
                                if (SAFE_NAME.matches(photo)) {
                                    File(staging, photo).outputStream().use { zip.copyTo(it) }
                                    staged += photo
                                }
                            }
                        }
                    }
                }
            } catch (e: ZipException) {
                throw BackupException("Not a zip file", e)
            }
            val data = parse(json ?: throw BackupException("No $JSON_ENTRY in the file"))

            db.withTransaction {
                db.relationshipDao().deleteAll()
                db.personDao().deleteAll()
                db.tabDao().deleteAll()
                db.tabDao().insertAll(data.tabs)
                db.personDao().insertAll(data.people)
                db.relationshipDao().insertAll(data.relationships)
                if (data.people.none { it.isSelf }) {
                    db.personDao().insert(Person(name = AppDatabase.SELF_NAME, isSelf = true, createdAt = System.currentTimeMillis()))
                }
            }

            // Photos: copy the backup's photos in, then remove files that no person uses.
            staged.forEach { name -> File(staging, name).copyTo(photos.file(name), overwrite = true) }
            val used = data.people.mapNotNull { it.photoFile }.toSet()
            photos.directory.listFiles()?.filter { it.name !in used }?.forEach { it.delete() }

            BackupSummary(data.people.count { !it.isSelf }, data.tabs.size, data.relationships.size, staged.size)
        } finally {
            staging.deleteRecursively()
        }
    }

    private data class Data(val tabs: List<Tab>, val people: List<Person>, val relationships: List<Relationship>)

    private fun parse(text: String): Data = try {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) throw BackupException("Not a Name List backup")
        val version = root.optInt("version", -1)
        if (version < 1 || version > VERSION) throw BackupException("Unsupported backup version $version")
        val tabs = root.getJSONArray("tabs").objects().map { it.toTab() }
        val people = root.getJSONArray("people").objects().map { it.toPerson() }
        val ids = people.map { it.id }.toSet()
        val tabIds = tabs.map { it.id }.toSet()
        val links = root.getJSONArray("relationships").objects().map { it.toRelationship() }
            .filter { it.fromPersonId in ids && it.toPersonId in ids }
        Data(tabs, people.map { if (it.tabId != null && it.tabId !in tabIds) it.copy(tabId = null) else it }, links)
    } catch (e: JSONException) {
        throw BackupException("Damaged backup data", e)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun Tab.toJson() = JSONObject().put("id", id).put("name", name).put("sortIndex", sortIndex)

    private fun JSONObject.toTab() = Tab(id = getLong("id"), name = getString("name"), sortIndex = optLong("sortIndex"))

    private fun Person.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("notes", notes)
        .put("photoFile", photoFile ?: JSONObject.NULL)
        .put("metCity", metCity ?: JSONObject.NULL)
        .put("metLat", metLat ?: JSONObject.NULL)
        .put("metLon", metLon ?: JSONObject.NULL)
        .put("isSelf", isSelf)
        .put("sortIndex", sortIndex)
        .put("createdAt", createdAt)
        .put("tabId", tabId ?: JSONObject.NULL)

    private fun JSONObject.toPerson() = Person(
        id = getLong("id"),
        name = getString("name"),
        notes = optString("notes", ""),
        photoFile = optStringOrNull("photoFile")?.takeIf { SAFE_NAME.matches(it) },
        metCity = optStringOrNull("metCity"),
        metLat = optDoubleOrNull("metLat"),
        metLon = optDoubleOrNull("metLon"),
        isSelf = optBoolean("isSelf", false),
        sortIndex = optLong("sortIndex"),
        createdAt = optLong("createdAt"),
        tabId = if (isNull("tabId") || !has("tabId")) null else getLong("tabId"),
    )

    private fun Relationship.toJson() = JSONObject()
        .put("id", id)
        .put("from", fromPersonId)
        .put("to", toPersonId)
        .put("type", type.name)

    private fun JSONObject.toRelationship() = Relationship(
        id = getLong("id"),
        fromPersonId = getLong("from"),
        toPersonId = getLong("to"),
        type = converters.nameToRelationType(getString("type")),
    )

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else getDouble(key)

    companion object {
        const val FORMAT = "mmr.namelist.backup"
        const val VERSION = 1
        const val JSON_ENTRY = "backup.json"
        const val PHOTO_PREFIX = "photos/"
        private val SAFE_NAME = Regex("(?!\\.+$)[A-Za-z0-9._-]{1,100}")
        private val converters = Converters()
    }
}
