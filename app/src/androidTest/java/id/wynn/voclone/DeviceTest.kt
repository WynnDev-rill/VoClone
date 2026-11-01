package id.wynn.voclone

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DeviceTest {
    private val context: Context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun loadsAllArm64NativeDependencies() {
        System.loadLibrary("vc_omni");System.loadLibrary("vc_whisper");System.loadLibrary("vc_mp3")
        assertEquals(2,NativeBridge.omniAbi())
        assertTrue("Generic real Q4_K/Q6_K kernel math",NativeBridge.mathSelfTest())
        val selected=CpuRuntime.select()
        if(selected.fast) {
            assertTrue(NativeBridge.hasI8mm());assertTrue(FastNativeBridge.mathSelfTest())
            assertEquals("arm64-v8a",android.os.Build.SUPPORTED_ABIS.first())
        }
    }
    @Test fun decodesRealWavAndEncodedMp3() = runBlocking {
        val dir=File(context.cacheDir,"exports").apply { mkdirs() }
        val wav=File(dir,"codec-test.wav");val mp3=File(dir,"codec-test.mp3")
        try {
            val audio=PcmAudio(FloatArray(96000) { kotlin.math.sin(it*.08).toFloat()*.3f },24000)
            audio.writeWav(wav);encodeMp3(wav,mp3)
            for(file in listOf(wav,mp3)) {
                val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
                val decoded=AndroidAudio.decode(context,uri)
                assertEquals(24000,decoded.sampleRate);assertTrue(decoded.duration in 3.5..4.5);assertTrue(decoded.rms>.05)
            }
        } finally { wav.delete();mp3.delete() }
    }
    /** Opt-in only after the tester has accepted model terms and downloaded them in the app. */
    @Test fun actualOfflineReferenceToProfileToNewSpeech() = runBlocking {
        assumeTrue("Enable -e fullInference true after model consent/download",InstrumentationRegistry.getArguments().getString("fullInference")=="true")
        val models=ModelStore(context);assertTrue(models.accepted());models.verify()
        val reference=File(context.cacheDir,"exports/e2e-reference.wav").apply { parentFile?.mkdirs() }
        InstrumentationRegistry.getInstrumentation().context.assets.open("reference.wav").use { input -> reference.outputStream().use { input.copyTo(it) } }
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",reference)
        val audio=AndroidAudio.decode(context,uri)
        val engine=VoiceEngine(models)
        val transcript=engine.transcribe(audio) { }
        assertTrue(transcript.isNotBlank())
        val preview=engine.clone(audio,transcript,"Hello, friend.","en") { }
        val dir=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
        val previewFile=File(dir,"real-preview.wav");preview.writeWav(previewFile,true)
        val store=ProfileStore(File(context.filesDir,"voices"))
        val profile=store.create("QA ${UUID.randomUUID()}",reference,transcript,previewFile)
        try {
            assertTrue(ProfileStore(File(context.filesDir,"voices")).list().any { it.id==profile.id })
            val generated=engine.clone(PcmAudio.readWav(File(store.directory(profile.id),"reference.wav")),profile.transcript,"This is new speech.","en") { }
            val file=File(dir,"real-generated.wav");generated.writeWav(file,true)
            PcmAudio.readWav(file).validateOutput()
            assertNotEquals(fileSha256(previewFile),fileSha256(file))
            File(dir,"e2e-result.txt").writeText("PASS: reference decoded, Whisper transcript nonempty, real preview, profile persisted, different text synthesized, WAV validated.\nModel fingerprint accepted on device; no inference network call.\n")
        } finally { store.delete(profile.id);reference.delete() }
    }
}
