package id.wynn.voclone

import androidx.work.Data
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun speechTextFile(directory: File,id: String): File {
    require(runCatching { UUID.fromString(id).toString()==id }.getOrDefault(false)) { "Berkas teks tidak valid." }
    return File(directory,"$id.txt")
}
/** WorkManager has a 10 KiB payload limit; the complete immutable text stays in private storage. */
fun prepareSpeechInput(directory: File,text: String,voice: String,language: String,checkpoint: ()->Unit={}): Data {
    require(text.isNotBlank() && !text.contains('\u0000')) { "Teks kosong atau tidak valid." }
    require(language in setOf("none","en","id","ja"))
    require(UUID.fromString(voice).toString()==voice)
    directory.mkdirs();val id=UUID.randomUUID().toString();val file=speechTextFile(directory,id)
    val stage=File(directory,"$id.part")
    try {
        FileOutputStream(stage).use { output ->
            val writer=output.writer(Charsets.UTF_8).buffered()
            for(start in text.indices step 8192) { checkpoint();writer.write(text,start,minOf(8192,text.length-start)) }
            writer.flush();output.fd.sync()
        }
        check(stage.renameTo(file)) { "Teks gagal disimpan." }
    } finally { stage.delete() }
    return workDataOf("textFile" to id,"voice" to voice,"language" to language,"textCodepoints" to text.codePointCount(0,text.length))
}
