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

/** Creates all ViewModels from the [AppContainer] (ADR-002). */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { PeopleViewModel(container().peopleRepository, createSavedStateHandle()) }
    initializer { PersonDetailViewModel(container().peopleRepository, createSavedStateHandle()) }
    initializer { PersonEditViewModel(container().peopleRepository, createSavedStateHandle()) }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App).container
