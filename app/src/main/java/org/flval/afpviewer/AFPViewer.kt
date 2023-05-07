package org.flval.afpviewer

import android.app.Application
import com.google.android.material.color.DynamicColors

class AFPViewer: Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}