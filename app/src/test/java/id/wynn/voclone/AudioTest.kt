package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AudioTest {
    @Test fun wavRoundTripAndSyntheticMarker() {
        val source = FloatArray(24000) { kotlin.math.sin(it * .04).toFloat() * .4f }
        val file = File.createTempFile("voclone", ".wav")
        try {
            PcmAudio(source, 24000).writeWav(file, synthetic = true)
            val actual = PcmAudio.readWav(file)
            assertEquals(24000, actual.sampleRate)
            assertEquals(source.size, actual.samples.size)
            assertEquals(source[500].toDouble(), actual.samples[500].toDouble(), .0001)
            assertTrue(file.readBytes().toString(Charsets.ISO_8859_1).contains("VoClone synthetic audio"))
        } finally { file.delete() }
    }
    @Test fun rejectsSilentAndShortReferences() {
        for (samples in listOf(FloatArray(24000 * 5), FloatArray(100) { .5f })) {
            assertThrows(IllegalArgumentException::class.java) { PcmAudio(samples, 24000).validateReference() }
        }
    }
    @Test fun rejectsNonFiniteOutput() {
        assertThrows(IllegalArgumentException::class.java) { PcmAudio(floatArrayOf(Float.NaN), 24000).validateOutput() }
    }
    @Test fun resamplePreservesDurationAndRejectsAliasedHighFrequency() {
        val input = PcmAudio(FloatArray(48000) { kotlin.math.sin(2*Math.PI*16000*it/48000).toFloat() }, 48000)
        val result = input.resample(16000)
        assertEquals(16000, result.samples.size)
        assertTrue(result.samples.drop(64).dropLast(64).maxOf { kotlin.math.abs(it) } < .05)
    }
    @Test fun rejectsTruncatedWav() {
        val file = File.createTempFile("broken", ".wav")
        try { file.writeText("RIFFbad"); assertThrows(Exception::class.java) { PcmAudio.readWav(file) } }
        finally { file.delete() }
    }
}
