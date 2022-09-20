package com.denytheflowerpot.scrunch

import android.Manifest
import android.app.Application
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.denytheflowerpot.scrunch.helpers.folding.FoldDetectionStrategy
import com.denytheflowerpot.scrunch.managers.NotificationManager
import com.denytheflowerpot.scrunch.managers.SettingsManager
import com.denytheflowerpot.scrunch.managers.SoundPlaybackManager
import com.denytheflowerpot.scrunch.services.FoldActionSignalingService

class ScrunchApplication: Application() {
    val settingsManager: SettingsManager by lazy {
        SettingsManager(applicationContext)
    }
    val notificationManager: NotificationManager by lazy {
        NotificationManager(applicationContext)
    }
    val soundPlaybackManager: SoundPlaybackManager by lazy { SoundPlaybackManager(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        soundPlaybackManager.loadPreviousSounds()
        startServiceIfNeeded()
    }

    fun startServiceIfNeeded() {
        if (settingsManager.serviceStarted && checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            if (FoldDetectionStrategy.instanceForThisDevice == null) {
                Log.d("Scrunch", "Could not detect model")
            } else {
                startForegroundService(FoldActionSignalingService.getServiceIntent(true))
            }
        }
    }

    companion object {
        lateinit var instance: ScrunchApplication
            private set
    }
}