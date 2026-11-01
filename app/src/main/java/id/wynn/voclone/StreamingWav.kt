package id.wynn.voclone

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

internal data class WaveInfo(val rate: Int,val offset: Long,val bytes: Long) {
    val frames get()=bytes/2
    val duration get()=frames.toDouble()/rate
}
internal fun waveInfo(file: File): WaveInfo=RandomAccessFile(file,"r").use { f ->
    fun tag(): String=ByteArray(4).also { f.readFully(it) }.toString(Charsets.US_ASCII)
    fun uint()=Integer.reverseBytes(f.readInt()).toLong() and 0xffffffffL
    fun ushort()=java.lang.Short.reverseBytes(f.readShort()).toInt() and 65535
    require(f.length()>=44 && tag()=="RIFF") { "Berkas bukan WAV PCM." }
    val end=uint()+8
    require(end==f.length() && tag()=="WAVE") { "WAV terpotong atau rusak." }
    var rate=0;var result: WaveInfo?=null
    while(f.filePointer+8<=end) {
        val type=tag();val size=uint();val start=f.filePointer
        require(start+size+size%2<=end) { "Chunk WAV rusak." }
        when(type) {
            "fmt " -> {
                require(rate==0 && size>=16 && ushort()==1 && ushort()==1) { "WAV harus PCM mono 16-bit." }
                rate=uint().toInt();val byteRate=uint();val align=ushort();val bits=ushort()
                require(rate in 8000..192000 && byteRate==rate*2L && align==2 && bits==16)
            }
            "data" -> {
                require(rate>0 && result==null && size>0 && size%2==0L) { "Data WAV tidak valid." }
                result=WaveInfo(rate,start,size)
            }
        }
        f.seek(start+size+size%2)
    }
    require(f.filePointer==end) { "Chunk WAV terpotong." }
    result ?: error("WAV tidak memiliki audio.")
}
fun forEachWaveBlock(file: File,checkpoint: ()->Unit={},block: (FloatArray)->Unit) {
    val info=waveInfo(file)
    RandomAccessFile(file,"r").use { f ->
        f.seek(info.offset);var remaining=info.bytes
        val bytes=ByteArray(16384)
        while(remaining>0) {
            checkpoint();val n=minOf(remaining,bytes.size.toLong()).toInt();f.readFully(bytes,0,n)
            val buffer=ByteBuffer.wrap(bytes,0,n).order(ByteOrder.LITTLE_ENDIAN)
            block(FloatArray(n/2) { buffer.short/32768f });remaining-=n
        }
    }
}
data class WaveSummary(val duration: Double,val rms: Double,val waveform: List<Float>) {
    fun validateOutput() {
        require(duration>=.1 && rms>.0001) { "Audio hasil kosong, terlalu pendek, atau hening." }
    }
}
fun waveSummary(file: File,checkpoint: ()->Unit={}): WaveSummary {
    val info=waveInfo(file);val peaks=FloatArray(72);var index=0L;var sum=0.0
    forEachWaveBlock(file,checkpoint) { samples ->
        samples.forEach { value ->
            val bin=(index*72/info.frames).toInt().coerceAtMost(71)
            peaks[bin]=max(peaks[bin],abs(value));sum+=value.toDouble()*value;index++
        }
    }
    return WaveSummary(info.duration,sqrt(sum/info.frames),peaks.toList())
}
/** Stream one PCM block at a time. RIFF's unsigned 32-bit file size is the format limit. */
class StreamingWavWriter(file: File,private val checkpoint: ()->Unit={}): Closeable {
    private val output=RandomAccessFile(file.apply { parentFile?.mkdirs() },"rw")
    private var rate=0;private var dataBytes=0L;private var finished=false
    init { output.setLength(0);output.write(ByteArray(44)) }
    fun append(audio: PcmAudio) {
        audio.validateOutput();setRate(audio.sampleRate)
        val bytes=ByteBuffer.allocate(16384).order(ByteOrder.LITTLE_ENDIAN)
        audio.samples.forEachIndexed { i,value ->
            if(i%8192==0)checkpoint()
            if(bytes.remaining()<2) { write(bytes.array(),bytes.position());bytes.clear() }
            bytes.putShort((value.coerceIn(-1f,1f)*32767).roundToInt().toShort())
        }
        write(bytes.array(),bytes.position())
    }
    fun appendWav(file: File) {
        val info=waveInfo(file);setRate(info.rate)
        RandomAccessFile(file,"r").use { input ->
            input.seek(info.offset);var remaining=info.bytes;val bytes=ByteArray(16384)
            while(remaining>0) { checkpoint();val n=minOf(remaining,bytes.size.toLong()).toInt();input.readFully(bytes,0,n);write(bytes,n);remaining-=n }
        }
    }
    private fun setRate(value: Int) { check(!finished);require(rate==0 || rate==value);rate=value }
    private fun write(bytes: ByteArray,count: Int) {
        require(44+dataBytes+count+128<=0xffffffffL+8) { "Hasil mencapai batas format WAV 4 GiB. Simpan teks dalam beberapa tugas." }
        output.write(bytes,0,count);dataBytes+=count
    }
    fun finish() {
        check(!finished && rate>0 && dataBytes>0);checkpoint()
        val marker="VoClone synthetic audio\u0000".toByteArray()
        val infoSize=12+marker.size+marker.size%2
        val info=ByteBuffer.allocate(8+infoSize).order(ByteOrder.LITTLE_ENDIAN)
        info.put("LIST".toByteArray()).putInt(infoSize).put("INFOICMT".toByteArray()).putInt(marker.size).put(marker)
        output.write(info.array())
        val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt((output.length()-8).toInt()).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate*2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(dataBytes.toInt())
        output.seek(0);output.write(header.array());output.fd.sync();finished=true
    }
    override fun close()=output.close()
}
