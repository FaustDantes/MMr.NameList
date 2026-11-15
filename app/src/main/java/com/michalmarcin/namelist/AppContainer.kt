package com.michalmarcin.namelist

import android.content.Context
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.BackupManager
import com.michalmarcin.namelist.platform.LocationProvider
import com.michalmarcin.namelist.platform.SettingsStore
import com.michalmarcin.namelist.platform.PhotoStore

/** Manual dependency injection (ADR-002). One instance per app process. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.build(context) }
    val tabsRepository: TabsRepository by lazy { TabsRepository(database.tabDao()) }
    val relationshipsRepository: RelationshipsRepository by lazy {
        RelationshipsRepository(database.relationshipDao(), database.personDao())
    }
    val photoStore: PhotoStore by lazy { PhotoStore(context) }
    val locationProvider: LocationProvider by lazy { LocationProvider(context) }
    val settingsStore: SettingsStore by lazy { SettingsStore(context) }
    val backupManager: BackupManager by lazy { BackupManager(context, database, photoStore) }
    val peopleRepository: PeopleRepository by lazy {
        PeopleRepository(database.personDao(), deletePhoto = photoStore::delete)
    }
}
