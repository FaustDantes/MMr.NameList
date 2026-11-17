package com.michalmarcin.namelist

import android.content.Context
import android.content.res.Configuration
import com.michalmarcin.namelist.data.db.RelationType
import com.michalmarcin.namelist.data.repo.RelationTypesRepository
import com.michalmarcin.namelist.platform.AppLanguage
import com.michalmarcin.namelist.ui.person.labelRes
import java.util.Locale
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.LearnRepository
import com.michalmarcin.namelist.data.repo.PeopleRepository
import com.michalmarcin.namelist.data.repo.RelationshipsRepository
import com.michalmarcin.namelist.data.repo.TabsRepository
import com.michalmarcin.namelist.platform.BackupManager
import com.michalmarcin.namelist.platform.BackupPasswordStore
import com.michalmarcin.namelist.platform.LocationProvider
import com.michalmarcin.namelist.platform.SettingsStore
import com.michalmarcin.namelist.platform.WorldMapData
import com.michalmarcin.namelist.platform.WorldShapes
import com.michalmarcin.namelist.platform.PhotoStore

/** Manual dependency injection (ADR-002). One instance per app process. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.build(context) }
    val tabsRepository: TabsRepository by lazy { TabsRepository(database.tabDao()) }
    val relationshipsRepository: RelationshipsRepository by lazy {
        RelationshipsRepository(database.relationshipDao(), database.personDao(), database.relationKindDao())
    }
    val relationTypesRepository: RelationTypesRepository by lazy {
        RelationTypesRepository(database.relationKindDao(), builtInLabels(context))
    }
    val photoStore: PhotoStore by lazy { PhotoStore(context) }
    val locationProvider: LocationProvider by lazy { LocationProvider(context) }
    val settingsStore: SettingsStore by lazy { SettingsStore(context) }
    val backupPasswordStore: BackupPasswordStore by lazy { BackupPasswordStore(context) }

    /** Country borders for the map. Read on first use; call from a background thread. */
    val worldShapes: WorldShapes by lazy { context.assets.open(WorldMapData.ASSET).use(WorldMapData::read) }
    val backupManager: BackupManager by lazy { BackupManager(context, database, photoStore) }
    val learnRepository: LearnRepository by lazy {
        LearnRepository(database.personDao(), database.tabDao(), database.learnRoundDao())
    }
    val peopleRepository: PeopleRepository by lazy {
        PeopleRepository(database.personDao(), deletePhoto = photoStore::delete)
    }
}

/** The label of each built-in relationship type in every app language (F30: a taken name is taken in any language). */
private fun builtInLabels(context: Context): (RelationType) -> Set<String> {
    val labels: Map<RelationType, Set<String>> by lazy {
        val contexts = listOf(context) + AppLanguage.entries.mapNotNull { it.tag }.map { tag ->
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            context.createConfigurationContext(config)
        }
        RelationType.entries.associateWith { type -> contexts.map { it.getString(type.labelRes()) }.toSet() }
    }
    return { type -> labels[type].orEmpty() }
}
