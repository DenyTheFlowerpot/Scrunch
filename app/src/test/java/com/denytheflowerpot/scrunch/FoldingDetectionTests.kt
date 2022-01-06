package com.denytheflowerpot.scrunch

import com.denytheflowerpot.scrunch.helpers.folding.SurfaceDuoDetectionStrategy
import org.junit.Test

import org.junit.Assert.*

class FoldingDetectionTests {
    @Test
    fun surfaceDuoDetectionStrategy_isCorrect() {
        val strategy = SurfaceDuoDetectionStrategy()

        val closeAfterStart = strategy.processLogcatTrace("12-29 06:37:46.516  1386  1742 D PostureMonitor: sendPostureIntent posture=Closed(0) [N/A]", null)
        assertEquals(true, closeAfterStart)

        val openAfterClosed = strategy.processLogcatTrace("12-29 06:37:48.964  1386  1742 D PostureMonitor: sendPostureIntent posture=Book(3) [Both]", true)
        assertEquals(false, openAfterClosed)

        val peekAfterOpen = strategy.processLogcatTrace("12-29 06:38:02.173  1386  1742 D PostureMonitor: sendPostureIntent posture=PeekRight(1) [R2]", false)
        assertEquals(false, peekAfterOpen)

        val closedAfterPeek = strategy.processLogcatTrace("12-29 06:38:02.443  1386  1742 D PostureMonitor: sendPostureIntent posture=Closed(0) [N/A]", false)
        assertEquals(true, closedAfterPeek)

        val peekAfterClosed = strategy.processLogcatTrace("12-29 06:38:02.173  1386  1742 D PostureMonitor: sendPostureIntent posture=PeekRight(1) [R2]", true)
        assertEquals(true, peekAfterClosed)
    }
}