package com.michalmarcin.namelist.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michalmarcin.namelist.ui.people.PeopleRoute as PeopleScreenRoute
import com.michalmarcin.namelist.ui.map.MapScreenRoute
import com.michalmarcin.namelist.ui.person.PersonDetailRoute
import com.michalmarcin.namelist.ui.person.PersonEditRoute
import com.michalmarcin.namelist.ui.settings.SettingsRoute as SettingsScreenRoute
import com.michalmarcin.namelist.ui.tabs.ManageTabsRoute as ManageTabsScreenRoute

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = PeopleRoute) {
        composable<PeopleRoute> {
            PeopleScreenRoute(
                onAddPerson = { tabId -> nav.navigate(EditPersonRoute(tabId = tabId ?: 0L)) },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
                onManageTabs = { nav.navigate(ManageTabsRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
                onMap = { nav.navigate(MapRoute()) },
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
        composable<MapRoute> {
            MapScreenRoute(
                onBack = { nav.popBackStack() },
                onOpenPerson = { id -> nav.navigate(PersonRoute(id)) },
            )
        }
        composable<EditPersonRoute> {
            PersonEditRoute(onDone = { nav.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreenRoute(onBack = { nav.popBackStack() })
        }
        composable<ManageTabsRoute> {
            ManageTabsScreenRoute(onBack = { nav.popBackStack() })
        }
    }
}
