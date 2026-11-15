package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonDao
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.db.RelationshipDao
import com.michalmarcin.namelist.data.db.Relationship
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A link seen from one person: "[other] is my [type]". */
data class RelationView(val id: Long, val other: Person, val type: RelationType)

enum class AddResult { ADDED, DUPLICATE, SAME_PERSON }

class RelationshipsRepository(
    private val dao: RelationshipDao,
    private val people: PersonDao,
) {

    /** All links of [personId], each turned to read from that person's side. */
    fun observeFor(personId: Long): Flow<List<RelationView>> =
        combine(dao.observeFor(personId), people.observeAll()) { rows, everyone ->
            val byId = everyone.associateBy { it.id }
            rows.mapNotNull { row ->
                val (otherId, type) = row.seenFrom(personId)
                byId[otherId]?.let { RelationView(row.id, it, type) }
            }.sortedWith(compareBy({ it.type.ordinal }, { it.other.name.lowercase() }))
        }

    /** Saves "[otherId] is [personId]'s [type]". The reverse shows on the other person automatically. */
    suspend fun add(personId: Long, otherId: Long, type: RelationType): AddResult {
        if (personId == otherId) return AddResult.SAME_PERSON
        val exists = dao.between(personId, otherId).any { it.seenFrom(personId).second == type }
        if (exists) return AddResult.DUPLICATE
        dao.insert(Relationship(fromPersonId = personId, toPersonId = otherId, type = type))
        return AddResult.ADDED
    }

    /** Changes the type as seen by [viewerId]; the other side changes with it. */
    suspend fun changeType(id: Long, viewerId: Long, type: RelationType) {
        val row = dao.get(id) ?: return
        dao.setType(id, if (row.fromPersonId == viewerId) type else type.inverse)
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun Relationship.seenFrom(personId: Long): Pair<Long, RelationType> =
        if (fromPersonId == personId) toPersonId to type else fromPersonId to type.inverse
}
