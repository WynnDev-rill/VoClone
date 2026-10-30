package id.wynn.voclone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteOrder
import kotlin.math.min

/** Android platform decoder: no bundled ffmpeg, no extension-based trust. */
object AndroidAudio {
    suspend fun decode(context: Context, uri: Uri): PcmAudio = withContext(Dispatchers.IO) {
        require(uri.scheme=="content") { "Pilih berkas audio melalui pemilih berkas Android." }
        val extractor=MediaExtractor()
        var codec: MediaCodec?=null
        var started=false
        try {
            context.contentResolver.openAssetFileDescriptor(uri,"r")?.use { fd ->
                if(fd.declaredLength<0)extractor.setDataSource(fd.fileDescriptor)
                else extractor.setDataSource(fd.fileDescriptor,fd.startOffset,fd.declaredLength)
            } ?: error("Berkas tidak dapat dibuka.")
            val track=(0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true }
                ?: error("Berkas tidak memiliki audio yang didukung.")
            val format=extractor.getTrackFormat(track)
            val mime=format.getString(MediaFormat.KEY_MIME) ?: error("Format audio tidak dikenal.")
            require(mime in setOf("audio/mpeg","audio/raw","audio/mp4a-latm","audio/flac","audio/vorbis","audio/opus")) { "Gunakan MP3 atau WAV PCM." }
            if(format.containsKey(MediaFormat.KEY_DURATION))require(format.getLong(MediaFormat.KEY_DURATION)<=30_100_000L) { "Sampel maksimal 30 detik. Potong audio lalu impor kembali." }
            extractor.selectTrack(track)
            if(mime=="audio/raw") {
                val rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val encoding=if(format.containsKey(MediaFormat.KEY_PCM_ENCODING))format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                require(rate in 8000..192000 && channels in 1..8 && encoding in setOf(AudioFormat.ENCODING_PCM_16BIT,AudioFormat.ENCODING_PCM_FLOAT)) { "Gunakan WAV PCM 16-bit atau float 32-bit." }
                val bytes=if(encoding==AudioFormat.ENCODING_PCM_FLOAT)4 else 2
                val buffer=java.nio.ByteBuffer.allocate(1024*1024).order(ByteOrder.LITTLE_ENDIAN)
                val chunks=mutableListOf<FloatArray>();var count=0
                while(true) {
                    currentCoroutineContext().ensureActive();buffer.clear()
                    val n=extractor.readSampleData(buffer,0);if(n<0)break
                    require(n%(bytes*channels)==0) { "Frame WAV rusak." }
                    buffer.position(0);buffer.limit(n)
                    val mono=FloatArray(n/(bytes*channels)) {
                        var sum=0f;repeat(channels) { sum+=if(bytes==4)buffer.float else buffer.short/32768f };sum/channels
                    }
                    count+=mono.size;require(count<=rate*30) { "Sampel maksimal 30 detik." }
                    chunks+=mono;extractor.advance()
                }
                val samples=FloatArray(count);var offset=0
                chunks.forEach { it.copyInto(samples,offset);offset+=it.size }
                return@withContext PcmAudio(samples,rate).also { it.validateReference() }.resample(24000)
            }
            val decoder=MediaCodec.createDecoderByType(mime);codec=decoder
            decoder.configure(format,null,null,0);decoder.start();started=true
            var rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding=AudioFormat.ENCODING_PCM_16BIT
            var inputDone=false;var outputDone=false;var total=0;var lastOutput=System.nanoTime()
            val chunks=mutableListOf<FloatArray>();val info=MediaCodec.BufferInfo()
            while(!outputDone) {
                currentCoroutineContext().ensureActive()
                require(System.nanoTime()-lastOutput<30_000_000_000L) { "Decoder tidak merespons. Coba berkas WAV lain." }
                if(!inputDone) {
                    val index=decoder.dequeueInputBuffer(10_000)
                    if(index>=0) {
                        val buffer=decoder.getInputBuffer(index) ?: error("Decoder input gagal.")
                        val size=extractor.readSampleData(buffer,0)
                        if(size<0) { decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true }
                        else { decoder.queueInputBuffer(index,0,size,extractor.sampleTime,0);extractor.advance() }
                    }
                }
                when(val index=decoder.dequeueOutputBuffer(info,10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out=decoder.outputFormat
                        rate=out.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding=if(out.containsKey(MediaFormat.KEY_PCM_ENCODING))out.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        require(total==0) { "Format audio berubah di tengah berkas." }
                    }
                    in 0..Int.MAX_VALUE -> {
                        try {
                            require(rate in 8000..192000 && channels in 1..8 && encoding in setOf(AudioFormat.ENCODING_PCM_16BIT,AudioFormat.ENCODING_PCM_FLOAT)) { "Format PCM tidak didukung. Gunakan WAV 16-bit." }
                            val buffer=decoder.getOutputBuffer(index) ?: error("Decoder output gagal.")
                            buffer.position(info.offset);buffer.limit(info.offset+info.size);buffer.order(ByteOrder.LITTLE_ENDIAN)
                            val bytes=if(encoding==AudioFormat.ENCODING_PCM_FLOAT)4 else 2
                            require(info.size%(bytes*channels)==0) { "Frame audio rusak." }
                            val count=info.size/(bytes*channels)
                            require(total+count<=rate*30) { "Sampel maksimal 30 detik." }
                            if(count>0) {
                                val mono=FloatArray(count) {
                                    var value=0f
                                    repeat(channels) { value+=if(encoding==AudioFormat.ENCODING_PCM_FLOAT)buffer.float else buffer.short/32768f }
                                    value/channels
                                }
                                chunks+=mono;total+=count;lastOutput=System.nanoTime()
                            }
                            outputDone=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally { decoder.releaseOutputBuffer(index,false) }
                    }
                }
            }
            val samples=FloatArray(total);var offset=0
            chunks.forEach { it.copyInto(samples,offset);offset+=it.size }
            PcmAudio(samples,rate).also { it.validateReference() }.resample(24000).also { it.validateReference() }
        } finally {
            if(started)runCatching { codec?.stop() }
            codec?.release();extractor.release()
        }
    }
}

class VoiceRecorder(private val context: Context) {
    @Volatile private var stopped=true
    fun prepare() { stopped=false }
    fun stop() { stopped=true }
    suspend fun record(onTime: (Int)->Unit): PcmAudio = withContext(Dispatchers.IO) {
        check(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) { "Izinkan mikrofon untuk merekam suara." }
        val minimum=AudioRecord.getMinBufferSize(24000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        require(minimum>0) { "Perangkat tidak mendukung rekaman mono 24 kHz." }
        val recorder=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,24000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(minimum*2,8192))
        if(recorder.state!=AudioRecord.STATE_INITIALIZED) { recorder.release();error("Mikrofon tidak dapat dibuka.") }
        val samples=FloatArray(720000);val chunk=ShortArray(4096);var count=0
        try {
            if(stopped)throw CancellationException("Rekaman dibatalkan sebelum mulai")
            recorder.startRecording()
            check(recorder.recordingState==AudioRecord.RECORDSTATE_RECORDING) { "Mikrofon sedang dipakai aplikasi lain." }
            while(!stopped && count<samples.size) {
                currentCoroutineContext().ensureActive()
                val n=recorder.read(chunk,0,min(chunk.size,samples.size-count),AudioRecord.READ_BLOCKING)
                require(n>0) { "Rekaman terputus. Coba lagi." }
                repeat(n) { samples[count+it]=chunk[it]/32768f };count+=n;onTime(count/24000)
            }
        } finally { stopped=true;runCatching { recorder.stop() };recorder.release() }
        PcmAudio(samples.copyOf(count),24000).also { it.validateReference() }
    }
}

internal object NativeMp3Bridge {
    init { System.loadLibrary("vc_mp3") }
    external fun create(path: ByteArray,frames: Long,synthetic: Boolean): Long
    external fun write(handle: Long,data: FloatArray): Int
    external fun finish(handle: Long): Int
    external fun close(handle: Long)
}
suspend fun encodeMp3(audio: PcmAudio,file: File) = withContext(Dispatchers.IO) {
    require(audio.sampleRate==24000);audio.validateOutput()
    val handle=NativeMp3Bridge.create(file.absolutePath.toByteArray(Charsets.UTF_8),audio.samples.size.toLong(),true)
    check(handle!=0L) { "Encoder MP3 tidak tersedia." }
    var success=false
    try {
        for(start in audio.samples.indices step 8192) {
            currentCoroutineContext().ensureActive()
            check(NativeMp3Bridge.write(handle,audio.samples.copyOfRange(start,min(start+8192,audio.samples.size)))==0) { "Ekspor MP3 gagal." }
        }
        check(NativeMp3Bridge.finish(handle)==0 && file.length()>0) { "MP3 kosong atau tidak lengkap." };success=true
    } finally { NativeMp3Bridge.close(handle);if(!success)file.delete() }
}
