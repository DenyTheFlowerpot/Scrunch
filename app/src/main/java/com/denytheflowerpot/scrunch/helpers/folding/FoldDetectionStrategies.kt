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
                    -> GalaxyFoldDetectionStrategy()
                    "duo", //Duo 1
                    "ssi_sdk_x86_64" //emulator
                    -> SurfaceDuoDetectionStrategy()
                    else -> null
                }
            }
    }
}

class GalaxyFoldDetectionStrategy: FoldDetectionStrategy {
    override val logcatTraceTag: String
        get() = "DisplayFoldController"
    override val logcatTracePrefix: String
        get() = "setDeviceFolded"
    override val processLogcatTrace: (String, Boolean?) -> Boolean?
        get() = { line, _ ->
            val processedLine = line.split(" ").firstOrNull { it.startsWith("Folded=") }?.removePrefix("Folded=")
            if (processedLine != line) { processedLine.toBoolean() } else null
        }
}

class SurfaceDuoDetectionStrategy: FoldDetectionStrategy {
    override val logcatTraceTag: String
        get() = "SurfaceShell.PF"
    override val logcatTracePrefix: String
        get() = "FirePosture:Posture3D"
    override val processLogcatTrace: (String, Boolean?) -> Boolean?
        get() = { line, previous ->
            val processedLine = line.split(", ").firstOrNull { it.contains("posture=") }
            if (processedLine != line) {
                val folded = processedLine?.endsWith("Closed") == true
                if (folded) { true } else {
                    val isPeeking = processedLine?.contains("Peek") == true
                    //if closed, consider peeking to be still closed
                    //otherwise, stay open (until we are closed again)
                    isPeeking && previous == true
                }
            } else null
        }
}