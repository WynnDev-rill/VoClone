package id.wynn.voclone

import android.content.Context
import androidx.work.WorkManager
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Preserve active/system-retry parts, reclaim terminal and abandoned preparation data. */
fun pruneSpeechStorage(context: Context,manager: WorkManager) = pruneSpeechFiles(context.filesDir,System.currentTimeMillis(),
    workActive={ manager.getWorkInfoById(it).get(5,TimeUnit.SECONDS)?.state?.isFinished==false },
    textActive={ manager.getWorkInfosByTag("text:$it").get(5,TimeUnit.SECONDS).any { task -> !task.state.isFinished } })

internal fun pruneSpeechFiles(filesDir: File,now: Long,workActive: (UUID)->Boolean,textActive: (String)->Boolean) {
    File(filesDir,"results").listFiles().orEmpty().filter { it.isDirectory && it.extension=="parts" || it.isFile && it.extension=="part" }.forEach { file ->
        val uuid=runCatching { UUID.fromString(file.nameWithoutExtension).also { require(it.toString()==file.nameWithoutExtension) } }.getOrNull() ?: return@forEach
        if(!workActive(uuid))file.deleteRecursively()
    }
    File(filesDir,"texts").listFiles().orEmpty().filter { it.extension in setOf("txt","part") && now-it.lastModified()>86400000L }.forEach { file ->
        if(!textActive(file.nameWithoutExtension))file.delete()
    }
}
