package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.db.TabDao
import kotlinx.coroutines.flow.Flow

class TabsRepository(private val dao: TabDao) {

    fun observeTabs(): Flow<List<Tab>> = dao.observeAll()

    suspend fun add(name: String): Long {
        require(name.isNotBlank()) { "Tab name must not be blank" }
        return dao.insert(Tab(name = name.trim(), sortIndex = dao.maxSortIndex() + 1))
    }

    suspend fun rename(id: Long, name: String) {
        require(name.isNotBlank()) { "Tab name must not be blank" }
        dao.rename(id, name.trim())
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun reorder(idsInNewOrder: List<Long>) = dao.reorder(idsInNewOrder)
}
