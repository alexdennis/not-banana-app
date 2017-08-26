package com.carltondennis.banana

import android.app.Application
import timber.log.Timber

/**
 * Created by alex on 8/26/17.
 */
class BananaApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}