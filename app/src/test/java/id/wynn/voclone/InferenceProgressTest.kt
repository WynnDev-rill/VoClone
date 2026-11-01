package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test

class InferenceProgressTest {
    @Test fun rejectsInvalidNativeCounters() {
        for(event in listOf(Triple(9,0,1),Triple(3,-1,32),Triple(3,33,32),Triple(3,0,-1))) {
            assertThrows(IllegalArgumentException::class.java) { InferenceProgress(event.first,event.second,event.third) }
        }
    }
    @Test fun elapsedTimeDoesNotInventIterationProgress() {
        val trace=InferenceTrace(100)
        trace.record(InferenceProgress(3,4,32),200)
        trace.record(InferenceProgress(3,4,32),10200)
        val snapshot=trace.snapshot(18200)
        assertEquals(4,snapshot.done)
        assertEquals(32,snapshot.total)
        assertEquals(18100L,snapshot.elapsedMs)
        assertEquals(18000L,snapshot.sinceProgressMs)
    }
    @Test fun chunkChangesOnlyWhenSamplerIsEnteredAgain() {
        val trace=InferenceTrace(0)
        trace.record(InferenceProgress(3,0,32),10)
        trace.record(InferenceProgress(3,1,32),20)
        assertEquals(1,trace.snapshot(20).chunk)
        trace.record(InferenceProgress(4,0,1),30)
        trace.record(InferenceProgress(3,0,32),40)
        assertEquals(2,trace.snapshot(40).chunk)
    }
    @Test fun stageTimingsAccumulateAndSnapshotsDoNotMutate() {
        val trace=InferenceTrace(100)
        trace.record(InferenceProgress(1,0,0,backend="arm64-i8mm"),120)
        trace.record(InferenceProgress(2,0,1),200)
        trace.record(InferenceProgress(3,0,32),300)
        assertEquals(80L,trace.snapshot(500).stageMs[1])
        assertEquals(200L,trace.snapshot(500).stageMs[3])
        assertEquals(trace.snapshot(500),trace.snapshot(500))
        trace.record(InferenceProgress(4,0,1),800)
        assertEquals(500L,trace.snapshot(800).stageMs[3])
        assertEquals("arm64-i8mm",trace.snapshot(800).backend)
    }
    @Test fun translatedOrOldArmDeviceCannotChooseFastKernel() {
        assertFalse(useI8mm("x86_64",true))
        assertFalse(useI8mm("arm64-v8a",false))
        assertTrue(useI8mm("arm64-v8a",true))
    }
}
