package id.wynn.voclone

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable data class VoiceProfile(val id: String,val name: String,val transcript: String,val duration: Double,val createdAt: Long)
class ProfileStore(private val root: File) {
    private val json=Json { ignoreUnknownKeys=true }
    init { root.mkdirs() }
    fun directory(id: String): File {
        require(id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) { "Identitas profil tidak valid." }
        return File(root,id)
    }
    fun list(): List<VoiceProfile> = root.listFiles().orEmpty().filter { it.isDirectory && !it.name.startsWith(".") }.mapNotNull { dir ->
        runCatching {
            val p=json.decodeFromString<VoiceProfile>(File(dir,"profile.json").readText())
            require(directory(p.id)==dir && File(dir,"reference.wav").isFile && File(dir,"preview.wav").isFile)
            p
        }.getOrNull()
    }.sortedByDescending { it.createdAt }
    fun get(id: String): VoiceProfile = json.decodeFromString(File(directory(id),"profile.json").readText())
    fun create(name: String, reference: File, transcript: String, preview: File, id: String = UUID.randomUUID().toString()): VoiceProfile {
        require(name.trim().length in 1..60 && transcript.isNotBlank() && transcript.length<=2000)
        val source=PcmAudio.readWav(reference).also { it.validateReference() }
        PcmAudio.readWav(preview).validateOutput()
        val target=directory(id);val stage=File(root,".$id")
        try {
            check(stage.deleteRecursively()) { "Sisa profil sebelumnya tidak dapat dibersihkan." }
            check(stage.mkdirs())
            reference.copyTo(File(stage,"reference.wav"));preview.copyTo(File(stage,"preview.wav"))
            val profile=VoiceProfile(id,name.trim(),transcript.trim(),source.duration,System.currentTimeMillis())
            atomicWrite(File(stage,"profile.json"),json.encodeToString(profile))
            check(stage.renameTo(target)) { "Profil gagal disimpan." }
            return profile
        } finally { stage.deleteRecursively() }
    }
    fun rename(id: String,name: String) {
        require(name.trim().length in 1..60)
        atomicWrite(File(directory(id),"profile.json"),json.encodeToString(get(id).copy(name=name.trim())))
    }
    fun delete(id: String) { check(directory(id).deleteRecursively()) { "Profil gagal dihapus." } }
}
