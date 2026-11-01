package id.wynn.voclone

import android.content.Context
import android.app.NotificationManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable data class TaskDiagnostic(
    val taskId: String,val action: String,val status: String,val snapshot: InferenceSnapshot,
    val updatedAtEpochMs: Long,val referenceMs: Long=0,val textCodepoints: Int=0,
    val appVersion: String=BuildConfig.VERSION_NAME,val language: String="none",val attempt: Int=0,
    val pssKb: Long=0,val javaHeapBytes: Long=0,val thermalStatus: Int=0,val stopReason: Int=0,
    val failureType: String?=null,val peakObservedPssKb: Long=0,val reusedOutput: Boolean=false
)

class DiagnosticStore(private val directory: File) {
    private val json=Json { encodeDefaults=true;ignoreUnknownKeys=true;prettyPrint=true }
    private val filename=Regex("[0-9a-f-]{36}\\.json")
    private fun files()=directory.listFiles().orEmpty().filter { it.isFile && filename.matches(it.name) }.sortedByDescending { it.lastModified() }
    fun write(report: TaskDiagnostic) {
        require(UUID.fromString(report.taskId).toString()==report.taskId)
        require(report.action in setOf("clone","generate","transcribe"))
        require(report.status in setOf("running","succeeded","failed","cancelled"))
        atomicWrite(File(directory,"${report.taskId}.json"),json.encodeToString(report))
        files().drop(20).forEach { it.delete() }
    }
    fun read(): List<TaskDiagnostic> =files().take(20).mapNotNull { file ->
        if(file.length()>65536)null else runCatching { json.decodeFromString<TaskDiagnostic>(file.readText()) }.getOrNull()
    }.take(5)
}

@Serializable private data class SupportReport(
    val appVersion: String,val capturedAtEpochMs: Long,val androidApi: Int,val manufacturer: String,
    val model: String,val primaryAbi: String,val cpuCount: Int,val notificationsAllowed: Boolean,
    val ignoringBatteryOptimizations: Boolean,val freeStorageBytes: Long,val tasks: List<TaskDiagnostic>
)

/** Explicit user export only. Whitelisted metadata; no audio, names, text or file paths. */
fun supportReport(context: Context): File {
    val data=SupportReport(BuildConfig.VERSION_NAME,System.currentTimeMillis(),Build.VERSION.SDK_INT,
        Build.MANUFACTURER,Build.MODEL,Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),Runtime.getRuntime().availableProcessors(),
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
        StatFs(context.filesDir.path).availableBytes,DiagnosticStore(File(context.filesDir,"diagnostics")).read())
    val directory=File(context.cacheDir,"exports").apply { mkdirs() }
    val file=File.createTempFile("VoClone-support-",".json",directory)
    atomicWrite(file,Json { encodeDefaults=true;prettyPrint=true }.encodeToString(data))
    directory.listFiles().orEmpty().filter { it.name.startsWith("VoClone-support-") && it.extension=="json" }
        .sortedByDescending { it.lastModified() }.drop(5).forEach { it.delete() }
    return file
}
