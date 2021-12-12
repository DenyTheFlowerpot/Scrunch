package com.denytheflowerpot.scrunch.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.denytheflowerpot.scrunch.ScrunchApplication
import kotlinx.coroutines.*
import java.lang.Exception

class FoldActionSignalingService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val foldParamPrefix = "setDeviceFolded"
    private val notificationId = 4653

    private var logcatJob: Job? = null
    private var currentFoldState: Boolean? = null

    private suspend fun announceFoldAction() = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec("logcat -c")
        Runtime.getRuntime().exec("logcat DisplayFoldController:V *:S -e $foldParamPrefix")
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                lines.forEach { line ->
                        val processedLine = line.split(" ").firstOrNull { it.startsWith("Folded=") }?.removePrefix("Folded=")
                        if (processedLine != line) {
                            val folded = processedLine.toBoolean()
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
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == stopServiceAction) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (logcatJob == null) {
            logcatJob = scope.launch { announceFoldAction() }
        }
        startForeground(notificationId, ScrunchApplication.instance.notificationManager.generateNotification(stopServiceAction))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        scope.cancel()
        logcatJob = null
    }

    companion object {
        const val stopServiceAction = "StopFoldServiceAction"
    }
}