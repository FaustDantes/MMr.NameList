package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.db.RelationKindDao
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.db.labelKey
import kotlinx.coroutines.flow.Flow

/**
 * The list of relationship types, edited in Settings (F30).
 * [builtInLabels] gives the label of a built-in type in every app language, so "Friend",
 * "Priateľ" and "Přítel" all count as taken while the built-in type keeps its label.
 */
class RelationTypesRepository(
    private val dao: RelationKindDao,
    private val builtInLabels: (RelationType) -> Set<String> = { setOf(it.name) },
) {

    fun observeTypes(): Flow<List<RelationKind>> = dao.observeAll()

    /** Makes a type. Returns null for a blank name or a name that is taken (any case). */
    suspend fun add(name: String): Long? {
        if (name.isBlank() || isTaken(name, dao.getAll())) return null
        return dao.insert(RelationKind(name = name.trim(), sortIndex = dao.maxSortIndex() + 1))
    }

    /** Renames a type, built-in ones too. Returns false, and changes nothing, for a blank or taken name. */
    suspend fun rename(id: Long, name: String): Boolean {
        if (name.isBlank() || isTaken(name, dao.getAll(), exceptId = id)) return false
        dao.rename(id, name.trim())
        return true
    }

    /** Deletes a type; its relationships become "Other". "Other", parent and child cannot be deleted. */
    suspend fun delete(id: Long): Boolean {
        val kind = dao.get(id) ?: return false
        if (!kind.canDelete) return false
        dao.delete(id, fallbackId = RelationType.OTHER.seedId)
        return true
    }

    /** True when a type other than [exceptId] shows the same name, ignoring case and outer spaces. */
    fun isTaken(name: String, types: List<RelationKind>, exceptId: Long? = null): Boolean {
        val key = labelKey(name)
        return types.any { kind -> kind.id != exceptId && key in namesOf(kind).map(::labelKey) }
    }

    private fun namesOf(kind: RelationKind): Set<String> =
        kind.name?.let(::setOf) ?: kind.builtIn?.let(builtInLabels).orEmpty()
}
