package com.michalmarcin.namelist.platform

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.db.Converters
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonTab
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.db.Relationship
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.db.labelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
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

    /** Writes the backup to [uri]. With a [password] the file is encrypted (F17). */
    suspend fun export(uri: Uri, password: CharArray? = null): BackupSummary = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri) ?: throw BackupException("Cannot open $uri")
        stream.use { export(it, password) }
    }

    /** Reads the backup at [uri]. Throws [BackupPasswordException] for an encrypted file without or with a wrong [password]. */
    suspend fun import(uri: Uri, password: CharArray? = null): BackupSummary = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw BackupException("Cannot open $uri")
        stream.use { import(it, password) }
    }

    /** True when the file at [uri] is an encrypted backup, so the app must ask for the password first. */
    suspend fun isEncrypted(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw BackupException("Cannot open $uri")
        stream.use {
            val head = ByteArray(BackupCrypto.MAGIC.size)
            val read = BackupCrypto.readUpTo(it, head)
            BackupCrypto.isEncrypted(head, read)
        }
    }

    suspend fun export(output: OutputStream, password: CharArray? = null): BackupSummary = withContext(Dispatchers.IO) {
        val people = db.personDao().getAll()
        val tabs = db.tabDao().getAll()
        val links = db.relationshipDao().getAll()
        val types = db.relationKindDao().getAll()
        val builtInOf = types.associate { it.id to it.builtIn }
        val tabLinks = db.personDao().getAllTabLinks().groupBy({ it.personId }, { it.tabId })
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("tabs", JSONArray(tabs.map { it.toJson() }))
            .put("people", JSONArray(people.map { it.toJson(tabLinks[it.id].orEmpty()) }))
            .put("relationTypes", JSONArray(types.map { it.toJson() }))
            .put("relationships", JSONArray(links.map { it.toJson(builtInOf[it.typeId]) }))
        var photoCount = 0
        val target = if (password == null) output else BackupCrypto.encrypt(output, password)
        ZipOutputStream(target).use { zip ->
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
    suspend fun import(input: InputStream, password: CharArray? = null): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "import-${System.nanoTime()}").apply { mkdirs() }
        try {
            var json: String? = null
            val staged = mutableListOf<String>()
            val buffered = BufferedInputStream(input)
            buffered.mark(BackupCrypto.MAGIC.size)
            val head = ByteArray(BackupCrypto.MAGIC.size)
            val headLength = BackupCrypto.readUpTo(buffered, head)
            buffered.reset()
            val encrypted = BackupCrypto.isEncrypted(head, headLength)
            val source = when {
                !encrypted -> buffered
                password == null -> throw BackupPasswordException(BackupPasswordException.Reason.REQUIRED)
                else -> BackupCrypto.decrypt(buffered, password)
            }
            try {
                ZipInputStream(source).use { zip ->
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
            } catch (e: IOException) {
                // In an encrypted file a failed check shows here: wrong password or a changed file.
                if (encrypted) throw BackupPasswordException(BackupPasswordException.Reason.WRONG)
                throw e
            }
            val data = parse(json ?: throw BackupException("No $JSON_ENTRY in the file"))

            db.withTransaction {
                db.relationshipDao().deleteAll()
                db.relationKindDao().deleteAll()
                db.personDao().deleteAll()
                db.tabDao().deleteAll()
                db.relationKindDao().insertAll(data.relationTypes)
                db.tabDao().insertAll(data.tabs)
                db.personDao().insertAll(data.people)
                db.personDao().insertTabLinks(data.tabLinks)
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

    private data class Data(
        val tabs: List<Tab>,
        val people: List<Person>,
        val relationships: List<Relationship>,
        val tabLinks: List<PersonTab>,
        val relationTypes: List<RelationKind>,
    )

    private fun parse(text: String): Data = try {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) throw BackupException("Not a Name List backup")
        val version = root.optInt("version", -1)
        if (version < 1 || version > VERSION) throw BackupException("Unsupported backup version $version")
        // F29: a tag name exists once. A tag of an older backup with the name of an earlier tag is merged into it.
        val allTabs = root.getJSONArray("tabs").objects().map { it.toTab() }
        val firstOfName = allTabs.groupBy { labelKey(it.name) }.mapValues { it.value.first().id }
        val tabAlias = allTabs.associate { it.id to firstOfName.getValue(labelKey(it.name)) }
        val tabs = allTabs.filter { tabAlias[it.id] == it.id }
        val people = root.getJSONArray("people").objects().map { it.toPerson() }
        val ids = people.map { it.id }.toSet()
        val tabIds = tabs.map { it.id }.toSet()
        // F30: backups of 1.2.0 and older have no "relationTypes"; they get the built-in types.
        val relationTypes = root.optJSONArray("relationTypes")?.objects()?.map { it.toRelationKind() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { list -> list + RelationKind.builtIns.filter { kind -> list.none { it.id == kind.id } && kind.builtIn == RelationType.OTHER } }
            ?: RelationKind.builtIns
        val typeIds = relationTypes.map { it.id }.toSet()
        val links = root.getJSONArray("relationships").objects().map { it.toRelationship(typeIds) }
            .filter { it.fromPersonId in ids && it.toPersonId in ids }
            .distinctBy { Triple(it.fromPersonId, it.toPersonId, it.typeId) }
        // F28: "tabIds" lists all tabs of a person. Backups of 1.2.0 and older have only the single "tabId".
        val tabLinks = root.getJSONArray("people").objects().flatMap { person ->
            val personId = person.getLong("id")
            val many = person.optJSONArray("tabIds")?.let { array -> (0 until array.length()).map { array.getLong(it) } }
            (many ?: listOfNotNull(person.optLongOrNull("tabId"))).mapNotNull { tabAlias[it] }
                .filter { it in tabIds }.distinct().map { PersonTab(personId, it) }
        }
        // The old single column is not used any more (ADR-035).
        Data(tabs, people.map { it.copy(tabId = null) }, links, tabLinks, relationTypes)
    } catch (e: JSONException) {
        throw BackupException("Damaged backup data", e)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun Tab.toJson() = JSONObject().put("id", id).put("name", name).put("sortIndex", sortIndex)

    private fun JSONObject.toTab() = Tab(id = getLong("id"), name = getString("name"), sortIndex = optLong("sortIndex"))

    private fun Person.toJson(tabIds: List<Long>) = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("notes", notes)
        .put("photoFile", photoFile ?: JSONObject.NULL)
        .put("metCity", metCity ?: JSONObject.NULL)
        .put("metLat", metLat ?: JSONObject.NULL)
        .put("metLon", metLon ?: JSONObject.NULL)
        .put("firstMetOn", firstMetOn ?: JSONObject.NULL)
        .put("nativeLanguage", nativeLanguage ?: JSONObject.NULL)
        .put("birthDate", birthDate ?: JSONObject.NULL)
        .put("education", education ?: JSONObject.NULL)
        .put("currentWork", currentWork ?: JSONObject.NULL)
        .put("askAbout", askAbout ?: JSONObject.NULL)
        .put("lastUpdatedAt", lastUpdatedAt ?: JSONObject.NULL)
        .put("archivedAt", archivedAt ?: JSONObject.NULL)
        .put("isSelf", isSelf)
        .put("sortIndex", sortIndex)
        .put("createdAt", createdAt)
        // "tabId" (the lowest tab) stays for older versions of the app; "tabIds" has all of them.
        .put("tabId", tabIds.minOrNull() ?: JSONObject.NULL)
        .put("tabIds", JSONArray(tabIds.sorted()))

    private fun JSONObject.toPerson() = Person(
        id = getLong("id"),
        name = getString("name"),
        notes = optString("notes", ""),
        photoFile = optStringOrNull("photoFile")?.takeIf { SAFE_NAME.matches(it) },
        metCity = optStringOrNull("metCity"),
        metLat = optDoubleOrNull("metLat"),
        metLon = optDoubleOrNull("metLon"),
        firstMetOn = optLongOrNull("firstMetOn"),
        nativeLanguage = optStringOrNull("nativeLanguage"),
        birthDate = optLongOrNull("birthDate"),
        education = optStringOrNull("education"),
        currentWork = optStringOrNull("currentWork"),
        askAbout = optStringOrNull("askAbout"),
        lastUpdatedAt = optLongOrNull("lastUpdatedAt"),
        archivedAt = optLongOrNull("archivedAt"),
        isSelf = optBoolean("isSelf", false),
        sortIndex = optLong("sortIndex"),
        createdAt = optLong("createdAt"),
        tabId = null,
    )

    private fun RelationKind.toJson() = JSONObject()
        .put("id", id)
        .put("name", name ?: JSONObject.NULL)
        .put("builtIn", builtIn?.name ?: JSONObject.NULL)
        .put("sortIndex", sortIndex)

    private fun JSONObject.toRelationKind() = RelationKind(
        id = getLong("id"),
        name = optStringOrNull("name"),
        builtIn = converters.nameToRelationType(optStringOrNull("builtIn")),
        sortIndex = optLong("sortIndex"),
    )

    // "type" stays for older versions of the app: the built-in type, or OTHER for a type the user made.
    private fun Relationship.toJson(builtIn: RelationType?) = JSONObject()
        .put("id", id)
        .put("from", fromPersonId)
        .put("to", toPersonId)
        .put("typeId", typeId)
        .put("type", (builtIn ?: RelationType.OTHER).name)

    /** "typeId" since 1.3.0; older backups name a built-in type in "type". An unknown type becomes OTHER. */
    private fun JSONObject.toRelationship(typeIds: Set<Long>): Relationship {
        val byId = optLongOrNull("typeId")?.takeIf { it in typeIds }
        val byName = converters.nameToRelationType(optStringOrNull("type"))?.seedId?.takeIf { it in typeIds }
        return Relationship(
            id = getLong("id"),
            fromPersonId = getLong("from"),
            toPersonId = getLong("to"),
            typeId = byId ?: byName ?: RelationType.OTHER.seedId,
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)

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
