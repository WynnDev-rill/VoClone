package id.wynn.voclone

import androidx.work.Data
import androidx.work.workDataOf

fun cloneInput(referenceId: String,transcript: String,name: String,language: String): Data {
    require(transcript.isNotBlank() && !transcript.contains('\u0000')) { "Isi ucapan persis dalam sampel." }
    require(language in setOf("none","en","id","ja")) { "Pilihan bahasa tidak valid." }
    val kana=transcript.codePoints().anyMatch { Character.UnicodeScript.of(it) in setOf(Character.UnicodeScript.HIRAGANA,Character.UnicodeScript.KATAKANA) }
    val preview=when {
        language=="ja" || language=="none" && kana -> "これは私の声です。"
        language=="id" -> "Ini suara saya."
        language=="en" -> "This is my voice."
        else -> {
            val words=transcript.trim().split(Regex("\\s+"),limit=5).take(4).joinToString(" ")
            val length=words.codePointCount(0,words.length)
            if(length<=32)words else {
                val prefix=words.substring(0,words.offsetByCodePoints(0,32))
                if(prefix.contains(' '))prefix.substringBeforeLast(' ').trimEnd() else prefix
            }
        }
    }
    // Only the synthesis target is short; full conditioning data stays unchanged.
    return workDataOf("reference" to referenceId,"transcript" to transcript,"name" to name,"text" to preview,"language" to language)
}
