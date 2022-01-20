package com.denytheflowerpot.scrunch.helpers.folding

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.devicestate.DeviceStateManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.denytheflowerpot.scrunch.services.FoldActionSignalingService
import kotlinx.coroutines.*

interface FoldDetectionStrategy {
    fun create(context: Context, callback: (Int) -> Unit)
    fun destroy(context: Context)

    companion object {
        private var _instance: FoldDetectionStrategy? = null

        val instanceForThisDevice: FoldDetectionStrategy?
            get() {
                return _instance ?: run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        return Android12DetectionStrategy()
                    }

                    when (Build.DEVICE) {
                        "q2q", //Z Fold3
                        "f2q", //Z Fold2
                        "winner", //Z Fold
                        "winnerx", //Z Fold 5G
                        "bloomq", //Z Flip
                        "bloomxq", //Z Flip 5G
                        "b2q" //Z Flip3
                        -> GalaxyFoldDetectionStrategy(Build.VERSION.SDK_INT)
                        "duo", //Duo 1
                        -> SurfaceDuoDetectionStrategy()
                        else -> null
                    }
                }.apply {
                    _instance = this
                }
            }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
class Android12DetectionStrategy : FoldDetectionStrategy {
    private var stateCallback: DeviceStateManager.DeviceStateCallback? = null

    override fun create(context: Context, callback: (Int) -> Unit) {
        var array = context.resources.getStringArray(com.android.internal.R.array.config_device_state_postures)
        val stateMapping = hashMapOf<Int, Int>()

        if (array.isNullOrEmpty()) {
            array = context.resources.getStringArray(
                context.resources.getIdentifier("config_device_state_postures", "array", "android")
            )
        }

        stateMapping.putAll(array.map { it.split(":").run { this[0].toInt() to this[1].toInt() } })

        stateCallback = DeviceStateManager.DeviceStateCallback { state ->
            callback(stateMapping[state] ?: state)
        }

        dsm(context).registerCallback(context.mainExecutor, stateCallback)
    }

    override fun destroy(context: Context) {
        dsm(context).unregisterCallback(stateCallback)
    }

    @SuppressLint("WrongConstant")
    private fun dsm(context: Context) = context.getSystemService(Context.DEVICE_STATE_SERVICE) as DeviceStateManager
}

class GalaxyFoldDetectionStrategy(val sdk: Int) : LogcatDetectionStrategy() {
    override val logcatTraceTag: String
        get() = if (sdk >= 31) "WindowManagerServiceExt" else "DisplayFoldController"
    override val logcatTracePrefix: String
        get() = if (sdk >= 31) "onStateChanged" else "setDeviceFolded"
    override val processLogcatTrace: (String, Boolean?) -> Boolean?
        get() = { line, _ ->
            val toRemove = if (sdk >= 31) "isFolded=" else "Folded="
            val processedLine = line.split(" ").firstOrNull { it.startsWith(toRemove) }?.removePrefix(toRemove)
            if (processedLine != line) {
                processedLine.toBoolean()
            } else null
        }
}

class SurfaceDuoDetectionStrategy : LogcatDetectionStrategy() {
    override val logcatTraceTag: String
        get() = "PostureMonitor"
    override val logcatTracePrefix: String
        get() = "sendPostureIntent"
    override val processLogcatTrace: (String, Boolean?) -> Boolean?
        get() = { line, previous ->
            val processedLine = line.split(" ").firstOrNull { it.contains("posture=") }
            if (processedLine != line) {
                val folded = processedLine?.contains("Closed") == true
                if (folded) { true } else {
                    val isPeeking = processedLine?.contains("Peek") == true
                    //if closed, consider peeking to be still closed
                    //otherwise, stay open (until we are closed again)
                    isPeeking && previous == true
                }
            } else null
        }
}

abstract class LogcatDetectionStrategy : FoldDetectionStrategy {
    abstract val logcatTraceTag: String //initial filtering
    abstract val logcatTracePrefix: String //extra filtering
    //actual processing
    //true = folded
    //false = unfolded
    //null = not a valid line
    abstract val processLogcatTrace: (line: String, previousState: Boolean?) -> (Boolean?)

    private var currentFoldState: Boolean? = null
    private var runningJob: Job? = null
    private var scope: CoroutineScope? = null

    override fun create(context: Context, callback: (Int) -> Unit) {
        scope = CoroutineScope(Job() + Dispatchers.IO)
        runningJob = scope?.launch { execute(callback) }
    }

    override fun destroy(context: Context) {
        scope?.cancel()
        runningJob?.cancel("Normal stop")
        runningJob = null
    }

    private suspend fun execute(callback: (Int) -> Unit) {
        Runtime.getRuntime().exec("logcat -c")
        Runtime.getRuntime()
            .exec("logcat ${logcatTraceTag}:V *:S -e $logcatTracePrefix")
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                try {
                    lines.forEach { line ->
                        if (!currentCoroutineContext().isActive) {
                            throw FoldActionSignalingService.DummyStopServiceException()
                        }

                        val folded = processLogcatTrace(line, currentFoldState)
                        if (folded != null) {
                            Log.d("Scrunch", "Fold status is $folded")
                            if (currentFoldState == null) {
                                currentFoldState = folded
                            } else {
                                if (folded != currentFoldState) {
                                    callback(if (folded) FoldActionSignalingService.DEVICE_STATE_CLOSED else FoldActionSignalingService.DEVICE_STATE_FULLY_OPEN)
                                    currentFoldState = folded
                                }
                            }
                        } else {
                            Log.d("Scrunch", "Invalid line: $line")
                        }
                    }
                } catch (e: Exception) {
                    if (e !is FoldActionSignalingService.DummyStopServiceException) {
                        Log.d("Scrunch", "Error: $e")
                    }
                }
            }
    }
}