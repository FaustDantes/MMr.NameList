package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonDao
import com.michalmarcin.namelist.data.db.RelationKind
import com.michalmarcin.namelist.data.db.RelationKindDao
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.db.Relationship
import com.michalmarcin.namelist.data.db.RelationshipDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A link seen from one person: "[other] is my [type]". */
data class RelationView(val id: Long, val other: Person, val type: RelationKind)

enum class AddResult { ADDED, DUPLICATE, SAME_PERSON }

class RelationshipsRepository(
    private val dao: RelationshipDao,
    private val people: PersonDao,
    private val kinds: RelationKindDao,
) {

    /** All links of [personId], each turned to read from that person's side. */
    fun observeFor(personId: Long): Flow<List<RelationView>> =
        combine(dao.observeFor(personId), people.observeAll(), kinds.observeAll()) { rows, everyone, types ->
            val byId = everyone.associateBy { it.id }
            val typeById = types.associateBy { it.id }
            rows.mapNotNull { row ->
                val (otherId, typeId) = row.seenFrom(personId, types)
                val other = byId[otherId] ?: return@mapNotNull null
                val type = typeById[typeId] ?: return@mapNotNull null
                RelationView(row.id, other, type)
            }.sortedWith(compareBy({ it.type.sortIndex }, { it.type.id }, { it.other.name.lowercase() }))
        }

    /** Saves "[otherId] is [personId]'s [typeId]". The reverse shows on the other person automatically. */
    suspend fun add(personId: Long, otherId: Long, typeId: Long): AddResult {
        if (personId == otherId) return AddResult.SAME_PERSON
        val types = kinds.getAll()
        val exists = dao.between(personId, otherId).any { it.seenFrom(personId, types).second == typeId }
        if (exists) return AddResult.DUPLICATE
        dao.insert(Relationship(fromPersonId = personId, toPersonId = otherId, typeId = typeId))
        return AddResult.ADDED
    }

    /** Changes the type as seen by [viewerId]; the other side changes with it. */
    suspend fun changeType(id: Long, viewerId: Long, typeId: Long) {
        val row = dao.get(id) ?: return
        dao.setType(id, if (row.fromPersonId == viewerId) typeId else inverseOf(typeId, kinds.getAll()))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun Relationship.seenFrom(personId: Long, types: List<RelationKind>): Pair<Long, Long> =
        if (fromPersonId == personId) toPersonId to typeId else fromPersonId to inverseOf(typeId, types)

    companion object {
        /** Parent and child swap when seen from the other side. Every other type reads the same both ways. */
        fun inverseOf(typeId: Long, types: List<RelationKind>): Long {
            val builtIn = types.firstOrNull { it.id == typeId }?.builtIn ?: return typeId
            if (builtIn.inverse == builtIn) return typeId
            return types.firstOrNull { it.builtIn == builtIn.inverse }?.id ?: typeId
        }

        /** Row id of a built-in type. */
        fun idOf(type: RelationType): Long = type.seedId
    }
}
