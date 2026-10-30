package id.wynn.voclone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
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
    private fun report(phase: String,done: Long=0,total: Long=0) {
        setProgressAsync(workDataOf("phase" to phase,"done" to done,"total" to total))
    }
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification=NotificationCompat.Builder(applicationContext,"tasks")
            .setSmallIcon(R.drawable.ic_microphone).setContentTitle("VoClone sedang bekerja")
            .setContentText("Ketuk Batal untuk menghentikan tugas.").setOngoing(true)
            .addAction(0,"Batal",WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)).build()
        val download=inputData.getString("action")=="download"
        val type=if(Build.VERSION.SDK_INT>=35 && !download)ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        return ForegroundInfo(27,notification,type)
    }
    override suspend fun doWork(): Result {
        val context=applicationContext
        val models=ModelStore(context)
        val profiles=ProfileStore(File(context.filesDir,"voices"))
        val output=File(context.filesDir,"results/$id.wav")
        val temporary=File(context.filesDir,"results/$id.part")
        return try {
            setForeground(getForegroundInfo())
            TaskLease.mutex.withLock {
                currentCoroutineContext().ensureActive()
                val action=inputData.getString("action") ?: error("Tugas tidak dikenal.")
                when(action) {
                    "download" -> { report("Mengunduh model…");models.download { done,total -> report("Mengunduh model…",done,total) };Result.success() }
                    "deleteModels" -> { models.delete();Result.success() }
                    "transcribe" -> {
                        val reference=privateReference(context,inputData.getString("reference") ?: error("Pilih audio terlebih dahulu."))
                        report("Whisper sedang mentranskripsikan…")
                        val transcript=VoiceEngine(models).transcribe(PcmAudio.readWav(reference)) { p -> report("Whisper sedang mentranskripsikan…",(p*100).toLong(),100) }
                        currentCoroutineContext().ensureActive()
                        atomicWrite(File(reference.parentFile,reference.nameWithoutExtension+".txt"),transcript)
                        Result.success(workDataOf("transcript" to transcript))
                    }
                    "clone","generate" -> {
                        // Re-execution after process death is idempotent: commit uses this worker's UUID.
                        if(action=="clone" && File(profiles.directory(id.toString()),"profile.json").isFile) {
                            profiles.get(id.toString())
                            PcmAudio.readWav(File(profiles.directory(id.toString()),"preview.wav")).validateOutput()
                            return@withLock Result.success(workDataOf("voice" to id.toString()))
                        }
                        if(action=="generate" && output.isFile && runCatching { PcmAudio.readWav(output).validateOutput() }.isSuccess) return@withLock Result.success(workDataOf("result" to output.name))
                        val voice=if(action=="generate")profiles.get(inputData.getString("voice") ?: error("Pilih profil suara.")) else null
                        val reference=if(voice!=null)File(profiles.directory(voice.id),"reference.wav") else privateReference(context,inputData.getString("reference") ?: error("Pilih audio terlebih dahulu."))
                        val transcript=voice?.transcript ?: inputData.getString("transcript").orEmpty()
                        require(transcript.isNotBlank()) { "Isi transkrip referensi atau jalankan Whisper dahulu." }
                        val text=inputData.getString("text").orEmpty()
                        report("Memeriksa model…")
                        val audio=VoiceEngine(models).clone(PcmAudio.readWav(reference),transcript,text,inputData.getString("language") ?: "none") { p ->
                            report(if(p<.2f) "Memuat OmniVoice…" else if(p<1f) "Menyintesis suara di perangkat…" else "Memvalidasi audio…")
                        }
                        currentCoroutineContext().ensureActive()
                        report("Menyimpan audio…")
                        audio.writeWav(temporary,synthetic=true)
                        PcmAudio.readWav(temporary).validateOutput()
                        currentCoroutineContext().ensureActive()
                        if(action=="clone") {
                            val profile=profiles.create(inputData.getString("name") ?: "Suara saya",reference,transcript,temporary,id.toString())
                            Result.success(workDataOf("voice" to profile.id))
                        } else {
                            check(temporary.renameTo(output)) { "Gagal menyimpan hasil." }
                            Result.success(workDataOf("result" to output.name))
                        }
                    }
                    else -> error("Tugas tidak dikenal.")
                }
            }
        } catch(e: CancellationException) { throw e }
        catch(e: OutOfMemoryError) { Result.failure(workDataOf("error" to "RAM tidak cukup. Tutup aplikasi berat, lalu coba teks lebih singkat.")) }
        catch(e: LinkageError) { Result.failure(workDataOf("error" to "Runtime native tidak kompatibel dengan perangkat ini. Gunakan Android ARM64.")) }
        catch(e: Exception) {
            val message=when {
                e.message?.contains("ENOSPC")==true || e.message?.contains("No space")==true -> "Penyimpanan penuh. Kosongkan ruang lalu coba kembali."
                e.message=="Native inference failed" -> "Engine AI gagal membuat audio. Coba referensi yang jelas, transkrip yang tepat, dan teks lebih singkat."
                e is java.io.FileNotFoundException -> "Berkas referensi atau profil tidak ditemukan. Impor atau rekam kembali."
                e is java.io.IOException && inputData.getString("action")=="download" -> "Unduhan terputus. Periksa koneksi dan lanjutkan dari Pengaturan."
                else -> e.message?.take(600) ?: "Pemrosesan gagal. Periksa referensi, model, dan ruang penyimpanan."
            }
            Result.failure(workDataOf("error" to message,"errorType" to e.javaClass.simpleName))
        }
        finally { temporary.delete() }
    }
}
