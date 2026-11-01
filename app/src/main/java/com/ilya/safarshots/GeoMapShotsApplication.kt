package com.ilya.safarshots

import android.app.Application
import com.ilya.safarshots.core.dunta.DuntaSdkController
import com.ilya.safarshots.core.locale.AppLocaleController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GeoMapShotsApplication : Application() {
    @Inject lateinit var localeController: AppLocaleController
    @Inject lateinit var duntaSdkController: DuntaSdkController

    override fun onCreate() {
        super.onCreate()
        localeController.initialize()
        duntaSdkController.prepare()
    }
}
