package com.michalmarcin.namelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.michalmarcin.namelist.platform.ThemeMode
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
            CompositionLocalProvider(LocalPhotoDirectory provides container.photoStore.directory) {
                NameListTheme(darkTheme = dark) {
                    AppNavHost()
                }
            }
        }
    }
}
