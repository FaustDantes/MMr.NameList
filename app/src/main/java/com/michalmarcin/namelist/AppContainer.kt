package com.michalmarcin.namelist

import android.content.Context
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.PhotoStore

/** Manual dependency injection (ADR-002). One instance per app process. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.build(context) }
    val tabsRepository: TabsRepository by lazy { TabsRepository(database.tabDao()) }
    val photoStore: PhotoStore by lazy { PhotoStore(context) }
    val peopleRepository: PeopleRepository by lazy {
        PeopleRepository(database.personDao(), deletePhoto = photoStore::delete)
    }
}
