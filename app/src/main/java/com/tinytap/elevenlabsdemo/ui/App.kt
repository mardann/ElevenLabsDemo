package com.tinytap.elevenlabsdemo.ui

import android.app.Application
import app.rive.runtime.kotlin.core.Rive

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        Rive.init(this)
    }
}