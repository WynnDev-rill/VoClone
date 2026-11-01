package id.wynn.voclone

import androidx.annotation.Keep
import kotlinx.coroutines.*

@Keep class NativeControl(private val job: Job,private val report: (InferenceProgress)->Unit) {
    fun cancelled(): Boolean = !job.isActive
    fun progress(value: Float) { status(0,(value.coerceIn(0f,1f)*100).toInt(),100) }
    fun status(stage: Int,done: Int,total: Int) { if(job.isActive)report(InferenceProgress(stage,done,total)) }
}
@Keep internal object NativeBridge {
    external fun omniAbi(): Int
    external fun hasI8mm(): Boolean
    external fun mathSelfTest(): Boolean
    external fun cloneAudio(model: String,codec: String,text: String,referenceText: String,reference: FloatArray,language: String,control: NativeControl): FloatArray?
    external fun transcribe(model: String,audio: FloatArray,control: NativeControl): ByteArray?
}
@Keep internal object FastNativeBridge {
    external fun omniAbi(): Int
    external fun mathSelfTest(): Boolean
    external fun cloneAudio(model: String,codec: String,text: String,referenceText: String,reference: FloatArray,language: String,control: NativeControl): FloatArray?
}
internal data class CpuChoice(val fast: Boolean,val label: String)
internal object CpuRuntime {
    @Volatile private var selected: CpuChoice?=null
    @Synchronized fun select(): CpuChoice {
        selected?.let { return it }
        System.loadLibrary("vc_omni")
        check(NativeBridge.omniAbi()==2 && NativeBridge.mathSelfTest()) { "Pemeriksaan kernel CPU gagal. Bagikan laporan diagnostik." }
        val choice=if(useI8mm(android.os.Build.SUPPORTED_ABIS.firstOrNull(),NativeBridge.hasI8mm())) {
            try {
                System.loadLibrary("vc_omni_i8mm")
                if(FastNativeBridge.omniAbi()==2 && FastNativeBridge.mathSelfTest())CpuChoice(true,"arm64-i8mm")
                else CpuChoice(false,"generic:fast-selftest-failed")
            } catch(e: LinkageError) { CpuChoice(false,"generic:fast-load-failed") }
        } else CpuChoice(false,"generic:capability-or-native-abi")
        selected=choice;return choice
    }
}
class VoiceEngine(private val models: ModelStore) {
    suspend fun transcribe(audio: PcmAudio,progress: (Float)->Unit): String = withContext(Dispatchers.Default) {
        audio.validateReference();models.verify()
        System.loadLibrary("vc_whisper")
        val job=currentCoroutineContext().job
        val result=NativeBridge.transcribe(models.file(2).path,audio.resample(16000).samples,NativeControl(job) { event -> progress(if(event.total>0)event.done.toFloat()/event.total else 0f) })
        job.ensureActive()
        val words=result?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotBlank() } ?: error("Whisper tidak mengenali ucapan. Rekam ulang atau isi transkrip secara manual.")
        require(words.length<=2000) { "Transkrip terlalu panjang. Isi manual dengan maksimal 2.000 karakter." }
        words
    }
    suspend fun clone(audio: PcmAudio,transcript: String,text: String,language: String,progress: (InferenceProgress)->Unit): PcmAudio = withContext(Dispatchers.Default) {
        audio.validateReference()
        require(audio.sampleRate==24000 && transcript.isNotBlank() && transcript.length<=2000) { "Isi transkrip referensi yang sesuai dengan rekaman." }
        require(text.isNotBlank() && text.length<=600 && !text.contains('\u0000') && !transcript.contains('\u0000')) { "Tulis 1–600 karakter untuk satu generasi." }
        require(language in setOf("none","id","ja","en")) { "Pilihan bahasa tidak valid." }
        progress(InferenceProgress(6,0,0));models.verify()
        progress(InferenceProgress(1,0,0))
        val cpu=CpuRuntime.select()
        progress(InferenceProgress(1,0,0,backend=cpu.label))
        val job=currentCoroutineContext().job
        job.ensureActive()
        // Route once before allocating model contexts. Never retry failed/cancelled inference silently.
        val control=NativeControl(job,progress)
        val result=if(cpu.fast)FastNativeBridge.cloneAudio(models.file(0).path,models.file(1).path,text,transcript,audio.samples,language,control)
                   else NativeBridge.cloneAudio(models.file(0).path,models.file(1).path,text,transcript,audio.samples,language,control)
        job.ensureActive()
        progress(InferenceProgress(5,0,0))
        PcmAudio(result ?: error("Cloning gagal. Periksa model, referensi, dan transkrip."),24000).also { it.validateOutput() }
    }
}
