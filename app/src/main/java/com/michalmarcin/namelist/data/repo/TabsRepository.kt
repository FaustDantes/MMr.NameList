package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.db.TabDao
import com.michalmarcin.namelist.data.db.labelKey
import kotlinx.coroutines.flow.Flow

/** Tags (F4, F29). The code keeps the old name "tab" (ADR-037). */
class TabsRepository(private val dao: TabDao) {

    fun observeTabs(): Flow<List<Tab>> = dao.observeAll()

    /** Makes a tag. Returns null for a blank name or a name another tag has already (any case, F29). */
    suspend fun add(name: String): Long? {
        if (name.isBlank() || isTaken(name, dao.getAll())) return null
        return dao.insert(Tab(name = name.trim(), sortIndex = dao.maxSortIndex() + 1))
    }

    /** Renames a tag. Returns false, and changes nothing, for a blank name or a name another tag has. */
    suspend fun rename(id: Long, name: String): Boolean {
        if (name.isBlank() || isTaken(name, dao.getAll(), exceptId = id)) return false
        dao.rename(id, name.trim())
        return true
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun reorder(idsInNewOrder: List<Long>) = dao.reorder(idsInNewOrder)

    companion object {
        /** True when a tag other than [exceptId] has the same name, ignoring case and outer spaces. */
        fun isTaken(name: String, tabs: List<Tab>, exceptId: Long? = null): Boolean =
            tabs.any { it.id != exceptId && labelKey(it.name) == labelKey(name) }
    }
}
