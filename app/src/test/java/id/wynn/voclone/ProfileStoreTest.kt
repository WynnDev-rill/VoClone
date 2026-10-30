package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ProfileStoreTest {
    @Test fun recoversStagingFilesLeftByProcessDeath() {
        val root = java.nio.file.Files.createTempDirectory("voice-recovery").toFile()
        try {
            val id="12345678-1234-1234-1234-123456789abc"
            val stale=File(root,".$id").apply { mkdirs() }
            File(stale,"reference.wav").writeText("interrupted copy")
            val source=File(root,"source.wav")
            PcmAudio(FloatArray(72000) { kotlin.math.sin(it*.03).toFloat()*.3f },24000).writeWav(source)
            val voice=ProfileStore(root).create("Recovered",source,"Reference words.",source,id)
            assertEquals(id,voice.id)
            assertEquals("Recovered",ProfileStore(root).list().single().name)
            assertFalse(stale.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun doesNotSaveProfileWithoutValidPreview() {
        val root = java.nio.file.Files.createTempDirectory("voices").toFile()
        try {
            val repo = ProfileStore(root)
            val ref = File(root,"reference.wav")
            PcmAudio(FloatArray(72000) { kotlin.math.sin(it*.03).toFloat()*.3f },24000).writeWav(ref)
            val invalid = File(root,"preview.wav").apply { writeText("not a wav") }
            assertThrows(Exception::class.java) { repo.create("Mine",ref,"Reference words.",invalid) }
            assertTrue(repo.list().isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun retainsVoiceAcrossRestartAndRejectsUnsafeId() {
        val root = java.nio.file.Files.createTempDirectory("voices").toFile()
        try {
            val ref = File(root,"reference.wav")
            PcmAudio(FloatArray(72000) { kotlin.math.sin(it*.03).toFloat()*.3f },24000).writeWav(ref)
            val voice = ProfileStore(root).create("Mine",ref,"Reference words.",ref)
            assertEquals("Mine", ProfileStore(root).list().single().name)
            ProfileStore(root).rename(voice.id,"New name")
            assertEquals("New name", ProfileStore(root).list().single().name)
            assertThrows(IllegalArgumentException::class.java) { ProfileStore(root).directory("../other") }
            ProfileStore(root).delete(voice.id)
            assertTrue(ProfileStore(root).list().isEmpty())
        } finally { root.deleteRecursively() }
    }
}
