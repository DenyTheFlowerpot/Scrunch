package com.denytheflowerpot.scrunch.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.denytheflowerpot.scrunch.ScrunchApplication
import com.denytheflowerpot.scrunch.helpers.folding.FoldDetectionStrategy

class FoldActionSignalingService : Service() {
    //necessary in order to stop processing folding actions
    class DummyStopServiceException : Exception()

    private val notificationId = 4653
    private var currentFoldState: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == stopServiceAction) {
            stopSelf()
            START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        startForeground(
            notificationId,
            ScrunchApplication.instance.notificationManager.generateNotification(
                stopServiceAction
            )
        )

        FoldDetectionStrategy.instanceForThisDevice?.create(this) { state ->
            if (state == DEVICE_STATE_HALF_OPEN) {
                //Currently ignoring this state.
                return@create
            }

            if (state != currentFoldState) {
                if (currentFoldState != null) {
                    val soundPlaybackManager = ScrunchApplication.instance.soundPlaybackManager
                    if (state == DEVICE_STATE_CLOSED) soundPlaybackManager.playFoldSound() else soundPlaybackManager.playUnfoldSound()
                }
                currentFoldState = state
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        FoldDetectionStrategy.instanceForThisDevice?.destroy(this)
    }

    companion object {
        const val stopServiceAction = "StopFoldServiceAction"

        const val DEVICE_STATE_CLOSED = 1
        const val DEVICE_STATE_FULLY_OPEN = 3
        const val DEVICE_STATE_HALF_OPEN = 2

        fun getServiceIntent(start: Boolean): Intent {
            val i = Intent(ScrunchApplication.instance, FoldActionSignalingService::class.java)
            if (!start) {
                i.action = stopServiceAction
            }
            return i
        }
    }
}
