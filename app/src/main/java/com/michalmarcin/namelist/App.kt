package com.michalmarcin.namelist

import android.app.Application

class App : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
