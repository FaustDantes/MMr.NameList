package com.michalmarcin.namelist.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michalmarcin.namelist.ui.people.PeopleRoute as PeopleScreenRoute
import com.michalmarcin.namelist.ui.archive.ArchiveRoute as ArchiveScreenRoute
import com.michalmarcin.namelist.ui.learn.LearnRoute as LearnScreenRoute
import com.michalmarcin.namelist.ui.map.MapScreenRoute
import com.michalmarcin.namelist.ui.person.PersonDetailRoute
import com.michalmarcin.namelist.ui.person.PersonEditRoute
import com.michalmarcin.namelist.ui.settings.SettingsRoute as SettingsScreenRoute
import com.michalmarcin.namelist.ui.tabs.ManageTabsRoute as ManageTabsScreenRoute
import com.michalmarcin.namelist.ui.relationtypes.ManageRelationTypesRoute as ManageRelationTypesScreenRoute

@Composable
fun AppNavHost(startWithAdd: Boolean = false, onExit: () -> Unit = {}) {
    val nav = rememberNavController()
    // Opened from the widget's add button: the form comes first, and closing or saving it leaves the app
    // (back to the home screen) instead of showing the list.
    var fromWidget by rememberSaveable { mutableStateOf(startWithAdd) }
    LaunchedEffect(Unit) { if (startWithAdd) nav.navigate(EditPersonRoute()) }
    NavHost(navController = nav, startDestination = PeopleRoute) {
        composable<PeopleRoute> {
            PeopleScreenRoute(
                onAddPerson = { tabId -> nav.navigate(EditPersonRoute(tabId = tabId ?: 0L)) },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
                onSettings = { nav.navigate(SettingsRoute) },
                onLearn = { nav.navigate(LearnRoute) },
            )
        }
        composable<LearnRoute> {
            LearnScreenRoute(
                onBack = { nav.popBackStack() },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
            )
        }
        composable<PersonRoute> {
            PersonDetailRoute(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(EditPersonRoute(id)) },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
                onShowOnMap = { id -> nav.navigate(MapRoute(id)) },
            )
        }
        composable<ArchiveRoute> {
            ArchiveScreenRoute(
                onBack = { nav.popBackStack() },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
            )
        }
        composable<MapRoute> {
            MapScreenRoute(
                onBack = { nav.popBackStack() },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
            )
        }
        composable<EditPersonRoute> {
            PersonEditRoute(
                onDone = { if (fromWidget) onExit() else nav.popBackStack() },
                // F22: leave the form and show the person who already has this name.
                onOpenExisting = { id ->
                    fromWidget = false
                    nav.popBackStack()
                    nav.navigate(PersonRoute(id))
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreenRoute(
                onBack = { nav.popBackStack() },
                onManageTags = { nav.navigate(ManageTabsRoute) },
                onManageRelationTypes = { nav.navigate(ManageRelationTypesRoute) },
                onArchive = { nav.navigate(ArchiveRoute) },
            )
        }
        composable<ManageTabsRoute> {
            ManageTabsScreenRoute(onBack = { nav.popBackStack() })
        }
        composable<ManageRelationTypesRoute> {
            ManageRelationTypesScreenRoute(onBack = { nav.popBackStack() })
        }
    }
}
