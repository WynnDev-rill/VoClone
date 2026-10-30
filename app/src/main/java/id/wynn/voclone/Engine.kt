package id.wynn.voclone

import androidx.annotation.Keep
import kotlinx.coroutines.*

@Keep class NativeControl(private val job: Job,private val report: (Float)->Unit) {
    fun cancelled(): Boolean = !job.isActive
    fun progress(value: Float) { if(job.isActive)report(value.coerceIn(0f,1f)) }
}
@Keep internal object NativeBridge {
    external fun omniAbi(): Int
    external fun cloneAudio(model: String,codec: String,text: String,referenceText: String,reference: FloatArray,language: String,control: NativeControl): FloatArray?
    external fun transcribe(model: String,audio: FloatArray,control: NativeControl): ByteArray?
}
class VoiceEngine(private val models: ModelStore) {
    suspend fun transcribe(audio: PcmAudio,progress: (Float)->Unit): String = withContext(Dispatchers.Default) {
        audio.validateReference();models.verify()
        System.loadLibrary("vc_whisper")
        val job=currentCoroutineContext().job
        val result=NativeBridge.transcribe(models.file(2).path,audio.resample(16000).samples,NativeControl(job,progress))
        job.ensureActive()
        val words=result?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotBlank() } ?: error("Whisper tidak mengenali ucapan. Rekam ulang atau isi transkrip secara manual.")
        require(words.length<=2000) { "Transkrip terlalu panjang. Isi manual dengan maksimal 2.000 karakter." }
        words
    }
    suspend fun clone(audio: PcmAudio,transcript: String,text: String,language: String,progress: (Float)->Unit): PcmAudio = withContext(Dispatchers.Default) {
        audio.validateReference()
        require(audio.sampleRate==24000 && transcript.isNotBlank() && transcript.length<=2000) { "Isi transkrip referensi yang sesuai dengan rekaman." }
        require(text.isNotBlank() && text.length<=600 && !text.contains('\u0000') && !transcript.contains('\u0000')) { "Tulis 1–600 karakter untuk satu generasi." }
        require(language in setOf("none","id","ja","en")) { "Pilihan bahasa tidak valid." }
        models.verify()
        System.loadLibrary("vc_omni")
        check(NativeBridge.omniAbi()==2) { "Versi runtime OmniVoice tidak sesuai." }
        val job=currentCoroutineContext().job
        val result=NativeBridge.cloneAudio(models.file(0).path,models.file(1).path,text,transcript,audio.samples,language,NativeControl(job,progress))
        job.ensureActive()
        PcmAudio(result ?: error("Cloning gagal. Periksa model, referensi, dan transkrip."),24000).also { it.validateOutput() }
    }
}
