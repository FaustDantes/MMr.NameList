package com.michalmarcin.namelist.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.michalmarcin.namelist.App
import com.michalmarcin.namelist.AppContainer
import com.michalmarcin.namelist.ui.people.PeopleViewModel
import com.michalmarcin.namelist.ui.person.PersonDetailViewModel
import com.michalmarcin.namelist.ui.person.PersonEditViewModel
import com.michalmarcin.namelist.ui.tabs.ManageTabsViewModel

/** Creates all ViewModels from the [AppContainer] (ADR-002). */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        PeopleViewModel(container().peopleRepository, container().tabsRepository, createSavedStateHandle())
    }
    initializer {
        PersonDetailViewModel(container().peopleRepository, container().tabsRepository, createSavedStateHandle())
    }
    initializer {
        PersonEditViewModel(
            container().peopleRepository,
            container().tabsRepository,
            container().photoStore,
            container().locationProvider,
            createSavedStateHandle(),
        )
    }
    initializer { ManageTabsViewModel(container().tabsRepository) }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App).container
