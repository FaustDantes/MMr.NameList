package com.michalmarcin.namelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.michalmarcin.namelist.ui.components.LocalPhotoDirectory
import com.michalmarcin.namelist.ui.nav.AppNavHost
import com.michalmarcin.namelist.ui.theme.NameListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val photos = (application as App).container.photoStore
            CompositionLocalProvider(LocalPhotoDirectory provides photos.directory) {
                NameListTheme {
                    AppNavHost()
                }
            }
        }
    }
}
