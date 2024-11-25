package com.app.videobox

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance

class App : Application() {

    init {
        instance = this
    }

    companion object{
         private lateinit var instance: App

        fun appContext(): Context {
            return instance.applicationContext
        }
    }
    override fun onCreate() {
        super.onCreate()

    }
}