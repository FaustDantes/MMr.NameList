package com.michalmarcin.namelist.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object PeopleRoute

@Serializable
data class PersonRoute(val id: Long)

/** [id] = 0 creates a new person. */
@Serializable
data class EditPersonRoute(val id: Long = 0L)
