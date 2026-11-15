package com.michalmarcin.namelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.michalmarcin.namelist.platform.ThemeMode
import com.michalmarcin.namelist.platform.WorldShapes
import com.michalmarcin.namelist.ui.map.LocalWorldShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.michalmarcin.namelist.ui.components.LocalPhotoDirectory
import com.michalmarcin.namelist.ui.nav.AppNavHost
import com.michalmarcin.namelist.ui.theme.NameListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = (application as App).container
            val theme by container.settingsStore.theme.collectAsState()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val worldShapes by produceState<WorldShapes?>(null) {
                value = withContext(Dispatchers.IO) { container.worldShapes }
            }
            CompositionLocalProvider(
                LocalPhotoDirectory provides container.photoStore.directory,
                LocalWorldShapes provides worldShapes,
            ) {
                NameListTheme(darkTheme = dark) {
                    AppNavHost()
                }
            }
        }
    }
}
