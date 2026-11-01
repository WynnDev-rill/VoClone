package id.wynn.voclone

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/** Deliberately opt-in, real model download + foreground Worker pipeline. Never runs in normal UI tests. */
class WorkerInferenceTest {
    @Test fun realOfflineWorkerPipeline()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val arguments=InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("fullInference")=="true")
        check(arguments.getString("acceptModelTerms")=="true") { "QA operator must explicitly accept the documented noncommercial model terms." }
        val context=instrumentation.targetContext
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val models=ModelStore(context)
        models.accept()
        withTimeout(15*60*1000L) { models.download { _,_ -> } }
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() } }
        val priorAirplane=android.provider.Settings.Global.getInt(context.contentResolver,"airplane_mode_on",0)
        val priorWifi=android.provider.Settings.Global.getInt(context.contentResolver,"wifi_on",0)
        val requests=mutableListOf<UUID>()
        var createdVoice: String?=null
        val manager=WorkManager.getInstance(context)
        val store=ProfileStore(File(context.filesDir,"voices"))
        val referenceId=UUID.randomUUID().toString();val reference=privateReference(context,referenceId).apply { parentFile?.mkdirs() }
        try {
        shell("cmd connectivity airplane-mode enable");shell("svc wifi disable")
        assertEquals(1,android.provider.Settings.Global.getInt(context.contentResolver,"airplane_mode_on",0))
        instrumentation.context.assets.open("reference.wav").use { input -> reference.outputStream().use { input.copyTo(it) } }
        val evidence=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
        suspend fun runWorker(action: String,input: Data,background: Boolean=false): WorkInfo {
            val request=OneTimeWorkRequestBuilder<CloneWorker>().setInputData(Data.Builder().putAll(input).putString("action",action).build()).build()
            requests+=request.id
            manager.enqueue(request).result.get()
            var sentHome=false;var realSteps=false
            val result=withTimeout(35*60*1000L) {
                while(true) {
                    val info=withContext(Dispatchers.IO) { manager.getWorkInfoById(request.id).get() } ?: error("Missing worker")
                    if(info.progress.getInt("stage",-1)==3 && info.progress.getLong("done",0)>0)realSteps=true
                    if(background && info.state==WorkInfo.State.RUNNING && !sentHome) { shell("input keyevent KEYCODE_HOME");sentHome=true }
                    if(info.state.isFinished)return@withTimeout info
                    delay(1000)
                }
                error("Unreachable")
            }
            File(evidence,"worker-$action.txt").writeText("state=${result.state}\nrealSamplerProgressObserved=$realSteps\nhomePressed=$sentHome\n")
            assertEquals(result.outputData.getString("error"),WorkInfo.State.SUCCEEDED,result.state)
            if(action in setOf("clone","generate"))assertTrue("Real native iteration data must reach WorkManager",realSteps)
            return result
        }
        val transcript=runWorker("transcribe",workDataOf("reference" to referenceId)).outputData.getString("transcript")!!
        assertTrue(transcript.isNotBlank())
        val cloned=runWorker("clone",cloneInput(referenceId,transcript,"QA offline worker","en"),true)
        val voice=cloned.outputData.getString("voice")!!
        createdVoice=voice;assertEquals(voice,store.get(voice).id)
        File(store.directory(voice),"preview.wav").copyTo(File(evidence,"worker-preview.wav"),true)
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val generated=runWorker("generate",prepareSpeechInput(File(context.filesDir,"texts"),"Hello.",voice,"en"),true)
        val result=File(context.filesDir,"results/${generated.outputData.getString("result")}")
        waveSummary(result).validateOutput()
        assertNotEquals(fileSha256(File(store.directory(voice),"preview.wav")),fileSha256(result))
        result.copyTo(File(evidence,"worker-generated.wav"),true)
        File(evidence,"worker-e2e.txt").writeText("PASS actual Whisper -> short clone preview -> persistent voice -> generated WAV; airplane mode enabled before inference; Home pressed during real Workers. Emulator is not a physical-device benchmark.\n")
        } finally {
            withContext(NonCancellable+Dispatchers.IO) {
                try {
                    requests.forEach { manager.cancelWorkById(it).result.get() }
                    TaskLease.mutex.withLock { createdVoice?.let { store.delete(it) };reference.delete() }
                } finally {
                    shell("cmd connectivity airplane-mode ${if(priorAirplane==1)"enable" else "disable"}")
                    shell("svc wifi ${if(priorWifi!=0)"enable" else "disable"}")
                }
            }
        }
    }
}
