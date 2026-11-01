package id.wynn.voclone

import android.content.Context
import androidx.work.WorkManager
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Preserve active/system-retry parts, reclaim terminal and abandoned preparation data. */
fun pruneSpeechStorage(context: Context,manager: WorkManager) {
    File(context.filesDir,"results").listFiles().orEmpty().filter { it.isDirectory && it.name.endsWith(".parts") }.forEach { dir ->
        val uuid=runCatching { UUID.fromString(dir.name.removeSuffix(".parts")) }.getOrNull() ?: return@forEach
        val info=manager.getWorkInfoById(uuid).get(5,TimeUnit.SECONDS)
        if(info==null || info.state.isFinished)dir.deleteRecursively()
    }
    File(context.filesDir,"texts").listFiles().orEmpty().filter { it.extension=="txt" && System.currentTimeMillis()-it.lastModified()>86400000L }.forEach { file ->
        val tasks=manager.getWorkInfosByTag("text:${file.nameWithoutExtension}").get(5,TimeUnit.SECONDS)
        if(tasks.none { !it.state.isFinished })file.delete()
    }
}
