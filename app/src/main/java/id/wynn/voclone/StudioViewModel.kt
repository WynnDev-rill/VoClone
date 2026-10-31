package id.wynn.voclone

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(application: Application): AndroidViewModel(application) {
    private val context=application.applicationContext
    private val prefs=context.getSharedPreferences("studio",0)
    val models=ModelStore(context)
    val profiles=ProfileStore(File(context.filesDir,"voices"))
    val updater=AppUpdater(context)
    private val manager=WorkManager.getInstance(context)
    private val savedActive=prefs.getString("active",null)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    private val active=MutableStateFlow<UUID?>(null)
    var work by mutableStateOf<WorkInfo?>(null);private set
    var queued by mutableStateOf(true);private set
    var importing by mutableStateOf(false);private set
    var recording by mutableStateOf(false);private set
    var recordingSeconds by mutableIntStateOf(0);private set
    var referenceId by mutableStateOf<String?>(null);private set
    var reference by mutableStateOf<PcmAudio?>(null);private set
    var transcript by mutableStateOf("")
    var voiceName by mutableStateOf("Suara saya")
    var voices by mutableStateOf<List<VoiceProfile>>(emptyList());private set
    var selected by mutableStateOf<String?>(prefs.getString("voice",null));private set
    var text by mutableStateOf("")
    var language by mutableStateOf("none")
    var result by mutableStateOf<File?>(null);private set
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var installed by mutableStateOf(false);private set
    var accepted by mutableStateOf(models.accepted());private set
    var theme by mutableStateOf(prefs.getString("theme","system") ?: "system");private set
    var playingPath by mutableStateOf<String?>(null);private set
    var playbackPosition by mutableFloatStateOf(0f);private set
    var playbackDuration by mutableIntStateOf(0);private set
    private var player: MediaPlayer?=null
    private val recorder=VoiceRecorder(context)
    private val exportMutex=kotlinx.coroutines.sync.Mutex()
    val busy: Boolean get() = importing || recording || queued || work?.state in setOf(WorkInfo.State.ENQUEUED,WorkInfo.State.RUNNING,WorkInfo.State.BLOCKED)
    private var handled: UUID?=null
    init {
        viewModelScope.launch {
            try {
            withContext(Dispatchers.IO) {
                val id=prefs.getString("reference",null)
                if(id!=null)runCatching { privateReference(context,id).let { it to PcmAudio.readWav(it) } }.getOrNull()?.let { (file,audio) ->
                    withContext(Dispatchers.Main) { referenceId=id;reference=audio;transcript=File(file.parentFile,file.nameWithoutExtension+".txt").readTextOrNull().orEmpty() }
                }
            }
            refresh()
            prefs.getString("result",null)?.takeIf { it.matches(Regex("[0-9a-f-]{36}\\.wav")) }?.let { filename -> result=File(context.filesDir,"results/$filename").takeIf { it.isFile } }
            val actual=withContext(Dispatchers.IO) {
                manager.getWorkInfosForUniqueWork("voclone-task").get(30,java.util.concurrent.TimeUnit.SECONDS)
            }
            val pending=actual.firstOrNull { !it.state.isFinished } ?: actual.firstOrNull { it.id==savedActive }
            active.value=pending?.id
            if(pending==null)prefs.edit().remove("active").apply()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { error="Status tugas tidak dapat dipulihkan. Coba tutup dan buka kembali aplikasi." }
            finally { queued=false }
        }
        viewModelScope.launch {
            active.flatMapLatest { id -> if(id==null)flowOf(null) else manager.getWorkInfoByIdFlow(id) }.collect { info ->
                work=info
                if(info!=null)queued=false
                if(info!=null && info.state.isFinished && handled!=info.id) {
                    handled=info.id
                    if(info.state==WorkInfo.State.SUCCEEDED) {
                        info.outputData.getString("transcript")?.let { transcript=it;message="Transkrip siap. Periksa dan koreksi sebelum cloning." }
                        info.outputData.getString("voice")?.let { select(it);message="Profil suara disimpan dengan pratinjau asli." }
                        info.outputData.getString("result")?.takeIf { it.matches(Regex("[0-9a-f-]{36}\\.wav")) }?.let {
                            result=File(context.filesDir,"results/$it");prefs.edit().putString("result",it).apply();message="Audio selesai dibuat."
                        }
                    } else if(info.state==WorkInfo.State.FAILED)error=info.outputData.getString("error") ?: "Tugas gagal. Coba kembali."
                    else if(info.state==WorkInfo.State.CANCELLED)message="Tugas dibatalkan."
                    refresh()
                    prefs.edit().remove("active").apply()
                }
            }
        }
        viewModelScope.launch {
            while(isActive) {
                val p=player
                if(p!=null)runCatching { if(p.isPlaying)playbackPosition=p.currentPosition.toFloat()/p.duration.coerceAtLeast(1) }
                delay(120)
            }
        }
    }
    suspend fun refresh() {
        val data=withContext(Dispatchers.IO) { profiles.list() to models.installed() }
        voices=data.first;installed=data.second;accepted=models.accepted()
        if(selected !in voices.map { it.id })select(voices.firstOrNull()?.id)
    }
    fun select(id: String?) { selected=id;prefs.edit().putString("voice",id).apply() }
    fun setAppearance(value: String) { theme=value;prefs.edit().putString("theme",value).apply() }
    private fun action(block: suspend ()->Unit) {
        viewModelScope.launch {
            try { block() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { error=e.message ?: "Operasi gagal. Coba kembali." }
            catch(e: LinkageError) { error="Runtime audio tidak tersedia di perangkat ini." }
            catch(e: OutOfMemoryError) { error="RAM tidak cukup. Tutup aplikasi berat atau gunakan sampel lebih singkat." }
        }
    }
    fun importAudio(uri: Uri) {
        if(busy)return
        importing=true;stopPlayback()
        action { try { saveReference(AndroidAudio.decode(context,uri)) } finally { importing=false } }
    }
    private suspend fun saveReference(audio: PcmAudio,words: String="") {
        val id=UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            val file=privateReference(context,id);audio.writeWav(file)
            if(words.isNotBlank())atomicWrite(File(file.parentFile,"$id.txt"),words)
            prefs.edit().putString("reference",id).commit()
            // Keep only the active staging reference. Profiles own independent copies.
            file.parentFile?.listFiles()?.filter { it.name!in setOf("$id.wav","$id.txt") }?.forEach { it.delete() }
        }
        referenceId=id;reference=audio;transcript=words
    }
    fun record() {
        if(recording) { recorder.stop();return }
        if(busy)return
        stopPlayback();recording=true;recordingSeconds=0;recorder.prepare()
        action {
            try {
                val audio=recorder.record { seconds -> viewModelScope.launch { recordingSeconds=seconds } }
                saveReference(audio)
            } finally { recording=false }
        }
    }
    fun stopRecording() { recorder.stop() }
    fun transcribe() { if(referenceId!=null)submit("transcribe",workDataOf("reference" to referenceId)) }
    fun cloneVoice() {
        if(referenceId==null) { error="Impor atau rekam suara terlebih dahulu.";return }
        if(transcript.isBlank()) { error="Jalankan transkripsi atau isi ucapan persis dari sampel.";return }
        if(voiceName.isBlank()) { error="Isi nama profil suara.";return }
        submit("clone",cloneInput(referenceId!!,transcript,voiceName,language))
    }
    fun generate() {
        if(selected==null || text.isBlank()) { error="Pilih profil suara dan tulis teks terlebih dahulu.";return }
        submit("generate",workDataOf("voice" to selected,"text" to text,"language" to language))
    }
    fun acceptAndDownload() {
        if(busy)return
        action { withContext(Dispatchers.IO) { models.accept() };accepted=true;submit("download") }
    }
    fun download()=submit("download")
    fun deleteModels()=submit("deleteModels")
    private fun submit(action: String,extra: Data=Data.EMPTY) {
        if(busy)return
        if(action !in setOf("download","deleteModels") && !installed) { error="Unduh model AI terlebih dahulu di Pengaturan.";return }
        stopPlayback();error=null;message=null;queued=true
        val request=OneTimeWorkRequestBuilder<CloneWorker>().setInputData(Data.Builder().putAll(extra).putString("action",action).build()).apply {
            if(action=="download")setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        }.build()
        action {
            try {
                val actual=withContext(Dispatchers.IO) {
                    manager.enqueueUniqueWork("voclone-task",ExistingWorkPolicy.KEEP,request).result.get(30,java.util.concurrent.TimeUnit.SECONDS)
                    manager.getWorkInfosForUniqueWork("voclone-task").get(30,java.util.concurrent.TimeUnit.SECONDS)
                }
                val target=actual.firstOrNull { !it.state.isFinished } ?: actual.firstOrNull { it.id==request.id }
                check(target!=null) { "Tugas tidak dapat dijadwalkan. Coba kembali." }
                prefs.edit().putString("active",target.id.toString()).apply();active.value=target.id
            } finally { queued=false }
        }
    }
    fun cancel() { active.value?.let { manager.cancelWorkById(it) } }
    fun rename(voice: VoiceProfile,name: String) {
        if(busy)return
        action { withContext(Dispatchers.IO) { TaskLease.mutex.withLock { profiles.rename(voice.id,name) } };refresh() }
    }
    fun delete(voice: VoiceProfile) {
        if(busy)return
        stopPlayback()
        action { withContext(Dispatchers.IO) { TaskLease.mutex.withLock { profiles.delete(voice.id) } };refresh() }
    }
    fun recreate(voice: VoiceProfile) {
        if(busy)return
        importing=true;stopPlayback()
        action {
            try { val audio=withContext(Dispatchers.IO) { PcmAudio.readWav(File(profiles.directory(voice.id),"reference.wav")) };saveReference(audio,voice.transcript);voiceName=voice.name+" baru" }
            finally { importing=false }
        }
    }
    fun referenceFile(): File?=referenceId?.let { privateReference(context,it) }
    fun previewFile(voice: VoiceProfile)=File(profiles.directory(voice.id),"preview.wav")
    fun play(file: File) {
        if(playingPath==file.path) { stopPlayback();return }
        stopPlayback()
        try {
            val p=MediaPlayer();player=p
            p.setDataSource(file.path)
            p.setOnCompletionListener { stopPlayback() }
            p.setOnErrorListener { _,_,_ -> stopPlayback();error="Audio tidak dapat diputar.";true }
            p.prepare();playbackDuration=p.duration;playingPath=file.path;playbackPosition=0f;p.start()
        } catch(e: Exception) { stopPlayback();error="Audio tidak dapat dibuka. Buat ulang hasil." }
    }
    fun seek(fraction: Float) { player?.let { it.seekTo((fraction.coerceIn(0f,1f)*it.duration).toInt());playbackPosition=fraction } }
    fun stopPlayback() { player?.release();player=null;playingPath=null;playbackPosition=0f }
    suspend fun exportFile(format: String): File {
        val source=result ?: error("Belum ada hasil audio.")
        return withContext(Dispatchers.IO) {
            if(format=="wav")return@withContext source
            exportMutex.withLock {
                val out=File(context.cacheDir,"exports/${source.nameWithoutExtension}.mp3").apply { parentFile?.mkdirs() }
                // Commit receipt separates a complete previous export from interrupted/legacy files.
                val receipt=File(out.parentFile,out.name+".sha256")
                if(!out.isFile || receipt.readTextOrNull()!=fileSha256(out)) {
                    out.delete();receipt.delete()
                    val stage=File(out.parentFile,out.name+".part")
                    try {
                        stage.delete()
                        encodeMp3(PcmAudio.readWav(source),stage)
                        currentCoroutineContext().ensureActive()
                        check(stage.renameTo(out)) { "MP3 gagal disimpan." }
                        atomicWrite(receipt,fileSha256(out))
                    } finally { stage.delete() }
                }
                out
            }
        }
    }
    fun save(uri: Uri,format: String) {
        action {
            val file=exportFile(format)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri,"wt")?.use { out -> file.inputStream().use { input -> input.copyTo(out) } } ?: error("Tujuan penyimpanan tidak dapat dibuka.")
            }
            message="Audio disimpan."
        }
    }
    fun checkUpdate(automatic: Boolean=false) {
        action { if(!automatic || (updater.automatic.value && System.currentTimeMillis()-updater.state.value.lastChecked>12*60*60*1000))updater.check() }
    }
    fun downloadUpdate() { action { updater.download() } }
    override fun onCleared() { recorder.stop();stopPlayback();super.onCleared() }
}
