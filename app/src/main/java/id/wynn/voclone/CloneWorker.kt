package id.wynn.voclone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.os.Debug
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class VoCloneApp: Application() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("tasks","Pemrosesan VoClone",NotificationManager.IMPORTANCE_LOW))
    }
}
/** One lease covers inference, model installation/deletion, and profile mutation. */
object TaskLease { val mutex=Mutex() }
fun privateReference(context: Context,id: String): File {
    require(id.matches(Regex("[0-9a-f-]{36}")) && java.util.UUID.fromString(id).toString()==id) { "Referensi tidak valid." }
    return File(context.filesDir,"references/$id.wav")
}
class CloneWorker(context: Context,parameters: WorkerParameters): CoroutineWorker(context,parameters) {
    private val startedMs=SystemClock.elapsedRealtime()
    private var currentProgress=workDataOf("phase" to "Menunggu engine…")
    private var trace: InferenceTrace?=null
    private var lastPublishedMs=0L
    private var lastDiagnosticMs=0L
    private var referenceMs=0L
    private var textCodepoints=0
    private var outcome="failed"
    private var failureType: String?=null
    private var peakObservedPssKb=0L
    private var reusedOutput=false
    private var lastUiPublishMs=0L
    private fun publish(data: Data) {
        val now=SystemClock.elapsedRealtime()
        val changed=data.getString("phase")!=currentProgress.getString("phase") || data.getString("kind")!=currentProgress.getString("kind")
        val complete=data.getLong("total",0)>0 && data.getLong("done",0)==data.getLong("total",0)
        if(!changed && !complete && now-lastUiPublishMs<500)return
        currentProgress=data;setProgressAsync(data);lastUiPublishMs=now
    }
    private fun report(phase: String,done: Long=0,total: Long=0) {
        val kind=when(inputData.getString("action")) { "download" -> "download";"transcribe" -> "transcription";else -> "phase" }
        publish(workDataOf("phase" to phase,"done" to done,"total" to total,"kind" to kind,
            "startedElapsedMs" to startedMs,"updatedElapsedMs" to SystemClock.elapsedRealtime()))
    }
    private fun inference(event: InferenceProgress) {
        val tracker=trace ?: return
        synchronized(tracker) {
            val now=SystemClock.elapsedRealtime()
            val previous=tracker.snapshot(now).stage
            tracker.record(event,now)
            val snapshot=tracker.snapshot(now)
            if(previous!=event.stage || now-lastPublishedMs>=500 || event.total>0 && event.done==event.total) {
                publish(workDataOf("phase" to event.label,"stage" to event.stage,"done" to event.done.toLong(),"total" to event.total.toLong(),
                    "kind" to "inference","chunk" to snapshot.chunk,"backend" to snapshot.backend,
                    "startedElapsedMs" to startedMs,"updatedElapsedMs" to now-snapshot.sinceProgressMs))
                lastPublishedMs=now
            }
            if(previous!=event.stage || now-lastDiagnosticMs>=1000) { saveDiagnostic("running");lastDiagnosticMs=now }
        }
    }
    private fun saveDiagnostic(status: String) {
        val tracker=trace ?: return
        try {
            val runtime=Runtime.getRuntime()
            val pss=Debug.getPss();peakObservedPssKb=maxOf(peakObservedPssKb,pss)
            DiagnosticStore(File(applicationContext.filesDir,"diagnostics")).write(TaskDiagnostic(
                id.toString(),inputData.getString("action").orEmpty(),status,tracker.snapshot(SystemClock.elapsedRealtime()),
                System.currentTimeMillis(),referenceMs,textCodepoints,language=inputData.getString("language") ?: "none",attempt=runAttemptCount,
                pssKb=pss,javaHeapBytes=runtime.totalMemory()-runtime.freeMemory(),
                thermalStatus=applicationContext.getSystemService(PowerManager::class.java).currentThermalStatus,
                stopReason=stopReason,failureType=failureType,peakObservedPssKb=peakObservedPssKb,reusedOutput=reusedOutput))
        } catch(e: OutOfMemoryError) { /* Best-effort telemetry must not override inference's outcome. */ }
        catch(e: Exception) { android.util.Log.w("VoClone","Diagnostic snapshot unavailable: ${e.javaClass.simpleName}") }
    }
    private fun succeeded(data: Data=Data.EMPTY): Result { outcome="succeeded";return Result.success(data) }
    override suspend fun getForegroundInfo(): ForegroundInfo=foregroundInfo()
    private fun foregroundInfo(): ForegroundInfo {
        val notificationId=(id.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
        val open=PendingIntent.getActivity(applicationContext,notificationId,
            Intent(applicationContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(applicationContext,"tasks")
            .setSmallIcon(R.drawable.ic_microphone).setContentTitle("VoClone sedang bekerja")
            .setContentText("Ketuk untuk melihat progres. Batal untuk menghentikan tugas.").setOnlyAlertOnce(true).setOngoing(true)
            .setContentIntent(open)
            .addAction(0,"Batal",WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)).build()
        val download=inputData.getString("action")=="download"
        val type=if(Build.VERSION.SDK_INT>=35 && !download)ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        return ForegroundInfo(notificationId,notification,type)
    }
    override suspend fun doWork(): Result {
        val context=applicationContext
        val models=ModelStore(context)
        val profiles=ProfileStore(File(context.filesDir,"voices"))
        val output=File(context.filesDir,"results/$id.wav")
        val temporary=File(context.filesDir,"results/$id.part")
        return try {
            setForeground(getForegroundInfo())
            report("Menunggu engine…")
            TaskLease.mutex.withLock {
                currentCoroutineContext().ensureActive()
                val action=inputData.getString("action") ?: error("Tugas tidak dikenal.")
                if(action in setOf("clone","generate","transcribe"))trace=InferenceTrace(SystemClock.elapsedRealtime())
                when(action) {
                    "download" -> { report("Mengunduh model…");models.download { done,total -> report("Mengunduh model…",done,total) };succeeded() }
                    "deleteModels" -> { models.delete();succeeded() }
                    "transcribe" -> {
                        val reference=privateReference(context,inputData.getString("reference") ?: error("Pilih audio terlebih dahulu."))
                        report("Whisper sedang mentranskripsikan…")
                        val samples=PcmAudio.readWav(reference);referenceMs=(samples.duration*1000).toLong()
                        val transcript=VoiceEngine(models).transcribe(samples) { p -> inference(InferenceProgress(0,(p*100).toInt(),100)) }
                        currentCoroutineContext().ensureActive()
                        atomicWrite(File(reference.parentFile,reference.nameWithoutExtension+".txt"),transcript)
                        succeeded(workDataOf("transcript" to transcript))
                    }
                    "clone","generate" -> {
                        // Re-execution after process death is idempotent: commit uses this worker's UUID.
                        if(action=="clone" && File(profiles.directory(id.toString()),"profile.json").isFile) {
                            profiles.get(id.toString())
                            PcmAudio.readWav(File(profiles.directory(id.toString()),"preview.wav")).validateOutput()
                            reusedOutput=true;return@withLock succeeded(workDataOf("voice" to id.toString()))
                        }
                        if(action=="generate" && output.isFile && runCatching { PcmAudio.readWav(output).validateOutput() }.isSuccess) { reusedOutput=true;return@withLock succeeded(workDataOf("result" to output.name)) }
                        val voice=if(action=="generate")profiles.get(inputData.getString("voice") ?: error("Pilih profil suara.")) else null
                        val reference=if(voice!=null)File(profiles.directory(voice.id),"reference.wav") else privateReference(context,inputData.getString("reference") ?: error("Pilih audio terlebih dahulu."))
                        val transcript=voice?.transcript ?: inputData.getString("transcript").orEmpty()
                        require(transcript.isNotBlank()) { "Isi transkrip referensi atau jalankan Whisper dahulu." }
                        val text=inputData.getString("text").orEmpty()
                        textCodepoints=text.codePointCount(0,text.length)
                        report("Memeriksa model…")
                        val samples=PcmAudio.readWav(reference);referenceMs=(samples.duration*1000).toLong()
                        val audio=VoiceEngine(models).clone(samples,transcript,text,inputData.getString("language") ?: "none",::inference)
                        currentCoroutineContext().ensureActive()
                        report("Menyimpan audio…")
                        audio.writeWav(temporary,synthetic=true)
                        PcmAudio.readWav(temporary).validateOutput()
                        currentCoroutineContext().ensureActive()
                        if(action=="clone") {
                            val profile=profiles.create(inputData.getString("name") ?: "Suara saya",reference,transcript,temporary,id.toString())
                            succeeded(workDataOf("voice" to profile.id))
                        } else {
                            check(temporary.renameTo(output)) { "Gagal menyimpan hasil." }
                            succeeded(workDataOf("result" to output.name))
                        }
                    }
                    else -> error("Tugas tidak dikenal.")
                }
            }
        } catch(e: CancellationException) { outcome="cancelled";failureType=e.javaClass.simpleName;throw e }
        catch(e: OutOfMemoryError) { failureType=e.javaClass.simpleName;Result.failure(workDataOf("error" to "RAM tidak cukup. Tutup aplikasi berat, lalu coba teks lebih singkat.")) }
        catch(e: LinkageError) { failureType=e.javaClass.simpleName;Result.failure(workDataOf("error" to "Runtime native tidak kompatibel dengan perangkat ini. Gunakan Android ARM64.")) }
        catch(e: Exception) {
            failureType=e.javaClass.simpleName
            val message=when {
                e.message?.contains("ENOSPC")==true || e.message?.contains("No space")==true -> "Penyimpanan penuh. Kosongkan ruang lalu coba kembali."
                e.message=="Native inference failed" -> "Engine AI gagal membuat audio. Coba referensi yang jelas, transkrip yang tepat, dan teks lebih singkat."
                e is java.io.FileNotFoundException -> "Berkas referensi atau profil tidak ditemukan. Impor atau rekam kembali."
                e is java.io.IOException && inputData.getString("action")=="download" -> "Unduhan terputus. Periksa koneksi dan lanjutkan dari Pengaturan."
                else -> e.message?.take(600) ?: "Pemrosesan gagal. Periksa referensi, model, dan ruang penyimpanan."
            }
            Result.failure(workDataOf("error" to message,"errorType" to e.javaClass.simpleName))
        }
        finally { saveDiagnostic(outcome);temporary.delete() }
    }
}
