package com.denytheflowerpot.scrunch.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.denytheflowerpot.scrunch.ScrunchApplication
import com.denytheflowerpot.scrunch.helpers.folding.FoldDetectionStrategy
import kotlinx.coroutines.*

class FoldActionSignalingService : Service() {

    //necessary in order to stop processing folding actions
    class DummyStopServiceException : Exception()

    private val notificationId = 4653

    private var logcatJob: Job? = null
    private var currentFoldState: Boolean? = null

    private lateinit var foldDetectionStrategy: FoldDetectionStrategy
    private lateinit var scope: CoroutineScope

    private suspend fun announceFoldAction() = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec("logcat -c")
        Runtime.getRuntime()
            .exec("logcat ${foldDetectionStrategy.logcatTraceTag}:V *:S -e ${foldDetectionStrategy.logcatTracePrefix}")
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                try {
                    lines.forEach { line ->
                        if (!this.isActive) {
                            throw DummyStopServiceException()
                        }

                        val folded = foldDetectionStrategy.processLogcatTrace(line, currentFoldState)
                        if (folded != null) {
                            Log.d("Scrunch", "Fold status is $folded")
                            if (currentFoldState == null) {
                                currentFoldState = folded
                            } else {
                                if (folded != currentFoldState) {
                                    val soundPlaybackManager =
                                        ScrunchApplication.instance.soundPlaybackManager
                                    if (folded) soundPlaybackManager.playFoldSound() else soundPlaybackManager.playUnfoldSound()
                                    currentFoldState = folded
                                }
                            }
                        } else {
                            Log.d("Scrunch", "Invalid line: $line")
                        }
                    }
                } catch (e: Exception) {
                    if (e !is DummyStopServiceException) {
                        Log.d("Scrunch", "Error: $e")
                    }
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == stopServiceAction) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (logcatJob == null) {
            val strategy = FoldDetectionStrategy.instanceForThisDevice
            return if (strategy != null) {
                foldDetectionStrategy = strategy
                scope = CoroutineScope(Job() + Dispatchers.IO)
                logcatJob = scope.launch { announceFoldAction() }
                startForeground(
                    notificationId,
                    ScrunchApplication.instance.notificationManager.generateNotification(
                        stopServiceAction
                    )
                )
                START_STICKY
            } else {
                Log.d("Scrunch", "Could not detect model")
                START_NOT_STICKY
            }
        } else {
            Log.d("Scrunch", "Service already running")
            return START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        scope.cancel()
        logcatJob?.cancel("Normal stop")
        logcatJob = null
    }

    companion object {
        const val stopServiceAction = "StopFoldServiceAction"
    }
}