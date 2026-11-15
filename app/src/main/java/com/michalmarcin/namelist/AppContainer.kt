package com.michalmarcin.namelist

import android.content.Context
import com.michalmarcin.namelist.data.db.AppDatabase
import com.michalmarcin.namelist.data.repo.PeopleRepository

/** Manual dependency injection (ADR-002). One instance per app process. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.build(context) }
    val peopleRepository: PeopleRepository by lazy { PeopleRepository(database.personDao()) }
}
