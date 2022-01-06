package com.denytheflowerpot.scrunch.helpers.folding

import android.os.Build

interface FoldDetectionStrategy {
    val logcatTraceTag: String //initial filtering
    val logcatTracePrefix: String //extra filtering
    //actual processing
    //true = folded
    //false = unfolded
    //null = not a valid line
    val processLogcatTrace: (line: String, previousState: Boolean?) -> (Boolean?)

    companion object {
        val instanceForThisDevice: FoldDetectionStrategy?
            get() {
                return when (Build.DEVICE) {
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
            }
    }
}

class GalaxyFoldDetectionStrategy(val sdk: Int): FoldDetectionStrategy {
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

class SurfaceDuoDetectionStrategy: FoldDetectionStrategy {
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