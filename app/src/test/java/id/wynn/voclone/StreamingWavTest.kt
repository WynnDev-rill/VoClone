package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StreamingWavTest {
    @Test fun longOutputUsesBoundedBlocksAndKeepsSyntheticMarker() {
        val file=File.createTempFile("long-wave",".wav")
        try {
            val part=PcmAudio(FloatArray(24000*120) { kotlin.math.sin(it*.03).toFloat()*.3f },24000)
            StreamingWavWriter(file).use { writer -> writer.append(part);writer.append(part);writer.finish() }
            val summary=waveSummary(file)
            assertEquals(240.0,summary.duration,0.0001)
            assertTrue(summary.rms>.1)
            assertEquals(72,summary.waveform.size)
            var frames=0L
            forEachWaveBlock(file) { samples -> assertTrue(samples.size<=8192);frames+=samples.size }
            assertEquals(24000L*240,frames)
            java.io.RandomAccessFile(file,"r").use { f ->
                f.seek(file.length()-80);val tail=ByteArray(80);f.readFully(tail)
                assertTrue(tail.toString(Charsets.ISO_8859_1).contains("VoClone synthetic audio"))
            }
        } finally { file.delete() }
    }
    @Test fun joinsPartDataWithoutEmbeddingTheirRiffHeaders() {
        val part=File.createTempFile("part-wave",".wav");val output=File.createTempFile("joined-wave",".wav")
        try {
            PcmAudio(FloatArray(24000) { .2f },24000).writeWav(part,true)
            StreamingWavWriter(output).use { it.appendWav(part);it.appendWav(part);it.finish() }
            assertEquals(2.0,waveSummary(output).duration,0.0001)
            assertEquals(48000,PcmAudio.readWav(output).samples.size)
        } finally { part.delete();output.delete() }
    }
    @Test fun rejectsTruncationAndDoesNotCommitAnUnfinishedHeader() {
        val file=File.createTempFile("bad-wave",".wav")
        try {
            StreamingWavWriter(file).use { it.append(PcmAudio(FloatArray(24000) { .2f },24000)) }
            assertThrows(IllegalArgumentException::class.java) { waveSummary(file) }
            PcmAudio(FloatArray(24000) { .2f },24000).writeWav(file)
            java.io.RandomAccessFile(file,"rw").use { it.setLength(file.length()-1) }
            assertThrows(IllegalArgumentException::class.java) { waveSummary(file) }
        } finally { file.delete() }
    }
}
