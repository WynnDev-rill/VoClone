package id.wynn.voclone

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
fun fileSha256(file: File,checkpoint: ()->Unit={}): String {
    val digest=MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input -> val bytes=ByteArray(64*1024);while(true) { checkpoint();val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n) } }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
data class ModelAsset(val name: String,val bytes: Long,val hash: String,val url: String) {
    init {
        require(name.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,100}")) && !name.contains("..")) { "Nama model tidak aman." }
        require(bytes>0 && hash.matches(Regex("[a-f0-9]{64}")))
        require(URI(url).host!=null)
    }
}
object ModelCatalog {
    private const val OMNI="https://huggingface.co/Serveurperso/OmniVoice-GGUF/resolve/361609388ae572a820d085185bbbe2a2aac4b30e/"
    val assets = listOf(
        ModelAsset("omnivoice-base-Q4_K_M.gguf",407485216,"4836ba5affcb83c04d08b3e96b6e48ac839947ee3864188739807b68e0e159c6",OMNI+"omnivoice-base-Q4_K_M.gguf"),
        ModelAsset("omnivoice-tokenizer-Q4_K_M.gguf",252474112,"988fc32bc699bce361c44e9af8383be9811960441f293eb5ab4ec6bf6386378d",OMNI+"omnivoice-tokenizer-Q4_K_M.gguf"),
        ModelAsset("ggml-tiny.bin",77691713,"be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21","https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/ggml-tiny.bin")
    )
    val totalBytes=assets.sumOf { it.bytes }
    const val NOTICE="""OmniVoice Q4: bobot model nonkomersial (CC-BY-NC; versi 4.0 dinyatakan oleh kartu konversi lama). Metadata konversi terbaru menyebut Apache-2.0, tetapi tidak menggantikan batasan model asli atau tokenizer. Gunakan hanya untuk penggunaan pribadi/nonkomersial dan suara milik Anda atau dengan izin.

Tokenizer: Boson Higgs Audio 2 Community License dan Meta Llama 3 Community License. Baca semua ketentuan di bawah, termasuk pembatasan penggunaan dan distribusi. Whisper: kode dan bobot asli MIT; runtime whisper.cpp MIT. Model diunduh dari repositori pembuat/konverter di Hugging Face, tidak dibundel dalam APK. Tidak ada audio yang diunggah.

Built with Higgs Materials licensed from Boson AI USA, Inc., Copyright Boson AI USA, Inc., All Rights Reserved and Meta Llama 3 licensed under the Meta Llama 3 Community License, Copyright Meta Platforms, Inc., All Right Reserved". based on Meta Llama 3

Kredit: OmniVoice — Han Zhu dan rekan; GGUF — Serveurperso; tokenizer — Boson AI / Meta; Whisper — OpenAI / Georgi Gerganov. Dukungan kualitas bahasa Indonesia/Jepang perlu diuji pada perangkat Anda."""
}

/** Range resumption is accepted only when the server confirms the exact offset and total. */
class VerifiedDownload(private val client: OkHttpClient) {
    suspend fun fetch(asset: ModelAsset, directory: File, progress: (Long,Long)->Unit) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val final=File(directory,asset.name);val partial=File(directory,asset.name+".part")
        if(final.isFile && final.length()==asset.bytes && fileSha256(final)==asset.hash) { progress(asset.bytes,asset.bytes);return@withContext }
        final.delete()
        if(partial.length()>asset.bytes)partial.delete()
        var offset=partial.length()
        if(offset==asset.bytes) {
            if(fileSha256(partial)==asset.hash) { check(partial.renameTo(final));progress(asset.bytes,asset.bytes);return@withContext }
            partial.delete();offset=0
        }
        require(directory.usableSpace >= asset.bytes-offset+16L*1024*1024) { "Penyimpanan tidak cukup. Kosongkan ruang lalu coba lagi." }
        val request=Request.Builder().url(asset.url).header("Accept-Encoding","identity")
        if(offset>0)request.header("Range","bytes=$offset-")
        val call=client.newCall(request.build())
        try {
            call.execute().use { response ->
                require(response.code==200 || response.code==206) { "Unduhan gagal (HTTP ${response.code}). Coba lagi untuk melanjutkan." }
                if(asset.url.startsWith("https:"))require(response.request.url.isHttps) { "Pengalihan unduhan tidak aman." }
                if(response.code==206) {
                    require(response.header("Content-Range")=="bytes $offset-${asset.bytes-1}/${asset.bytes}") { "Server mengirim rentang unduhan yang salah." }
                } else offset=0
                val body=response.body ?: error("Unduhan kosong.")
                val contentLength=body.contentLength()
                require(contentLength==-1L || contentLength==asset.bytes-offset) { "Ukuran model dari server berubah." }
                body.byteStream().use { input -> FileOutputStream(partial,offset>0).use { output ->
                    val buffer=ByteArray(64*1024);var count=offset;var lastReport=0L
                    progress(count,asset.bytes)
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val n=input.read(buffer);if(n<0)break
                        require(count+n<=asset.bytes) { "Unduhan melebihi ukuran model." }
                        output.write(buffer,0,n);count+=n
                        val now=System.currentTimeMillis()
                        if(now-lastReport>250) { progress(count,asset.bytes);lastReport=now }
                    }
                    output.fd.sync()
                    require(count==asset.bytes) { "Unduhan terputus. Coba lagi untuk melanjutkan." }
                } }
            }
            currentCoroutineContext().ensureActive()
            if(fileSha256(partial)!=asset.hash) { partial.delete();throw IllegalArgumentException("Integritas model gagal. Unduh ulang model.") }
            check(partial.renameTo(final)) { "Tidak dapat menyimpan model." }
            progress(asset.bytes,asset.bytes)
        } finally { call.cancel() }
    }
}
class ModelStore(context: Context) {
    val directory=File(context.noBackupFilesDir,"models").apply { mkdirs() }
    private val prefs=context.getSharedPreferences("licence",Context.MODE_PRIVATE)
    private val fingerprint: String by lazy {
        val terms=listOf("Higgs-Audio-2.txt","Meta-Llama-3.txt","OmniVoice-model-card.md","OmniVoice-current-card.md","Whisper-MIT.txt","CC-BY-NC-4.0.txt")
            .joinToString { name -> context.assets.open("licenses/$name").use { it.readBytes().toString(Charsets.UTF_8) } }
        sha256((ModelCatalog.NOTICE+terms+ModelCatalog.assets.joinToString { it.hash }).toByteArray())
    }
    fun accepted()=prefs.getString("fingerprint",null)==fingerprint
    fun accept() { check(prefs.edit().putString("fingerprint",fingerprint).commit()) { "Persetujuan tidak dapat disimpan. Kosongkan penyimpanan lalu coba lagi." } }
    fun installed()=ModelCatalog.assets.all { File(directory,it.name).length()==it.bytes } && File(directory,"verified").readTextOrNull()==fingerprint
    fun file(index: Int)=File(directory,ModelCatalog.assets[index].name)
    suspend fun verify() = withContext(Dispatchers.IO) {
        require(accepted()) { "Terima ketentuan model di Pengaturan terlebih dahulu." }
        val job=currentCoroutineContext()
        ModelCatalog.assets.forEach { asset ->
            job.ensureActive()
            require(File(directory,asset.name).let { it.isFile && it.length()==asset.bytes && fileSha256(it) { job.ensureActive() }==asset.hash }) { "Model belum lengkap atau rusak. Unduh ulang di Pengaturan." }
        }
    }
    suspend fun download(progress: (Long,Long)->Unit) {
        require(accepted()) { "Persetujuan lisensi model diperlukan." }
        val client=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
        var completed=0L
        ModelCatalog.assets.forEach { asset ->
            VerifiedDownload(client).fetch(asset,directory) { done,_ -> progress(completed+done,ModelCatalog.totalBytes) }
            completed+=asset.bytes
        }
        verify()
        atomicWrite(File(directory,"verified"),fingerprint)
    }
    fun delete() { directory.deleteRecursively();directory.mkdirs() }
}
fun File.readTextOrNull(): String? = if(isFile) runCatching { readText() }.getOrNull() else null
fun atomicWrite(file: File, text: String) {
    file.parentFile?.mkdirs()
    val temporary=File(file.parentFile,file.name+".tmp")
    FileOutputStream(temporary).use { it.write(text.toByteArray());it.fd.sync() }
    check(temporary.renameTo(file)) { "Tidak dapat menyimpan data. Periksa ruang penyimpanan." }
}
