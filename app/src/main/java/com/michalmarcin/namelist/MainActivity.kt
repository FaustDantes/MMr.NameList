package com.michalmarcin.namelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.michalmarcin.namelist.ui.people.PeopleScreen
import com.michalmarcin.namelist.ui.theme.NameListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NameListTheme {
                PeopleScreen()
            }
        }
    }
}
