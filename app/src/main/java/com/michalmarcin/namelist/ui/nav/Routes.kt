package com.michalmarcin.namelist.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object PeopleRoute

@Serializable
data class PersonRoute(val id: Long)

/** [id] = 0 creates a new person; [tabId] (0 = none) preselects the tab for a new person. */
@Serializable
data class EditPersonRoute(val id: Long = 0L, val tabId: Long = 0L)

@Serializable
data object ManageTabsRoute

@Serializable
data object ArchiveRoute

@Serializable
data object SettingsRoute

/** [focusId] = 0 shows everyone; else starts centered on that person. */
@Serializable
data class MapRoute(val focusId: Long = 0L)
