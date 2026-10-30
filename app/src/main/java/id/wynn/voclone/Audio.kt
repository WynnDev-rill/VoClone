package id.wynn.voclone

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** Normalized mono PCM; all model input is explicitly resampled, never relabelled. */
data class PcmAudio(val samples: FloatArray, val sampleRate: Int) {
    val duration: Double get() = samples.size.toDouble() / sampleRate
    val rms: Double get() = sqrt(samples.fold(0.0) { sum, v -> sum + v.toDouble()*v } / samples.size.coerceAtLeast(1))
    val clipped: Boolean get() = samples.count { abs(it) >= .999f } > samples.size / 100
    fun validateOutput() {
        require(sampleRate in 8000..192000 && samples.size >= sampleRate / 10) { "Audio hasil kosong atau terlalu pendek. Coba teks lain." }
        require(duration <= 180 && samples.all { it.isFinite() && abs(it) <= 10f }) { "Audio hasil tidak valid." }
        require(rms > .0001) { "Engine menghasilkan audio hening. Periksa referensi dan transkrip." }
    }
    fun validateReference() {
        require(sampleRate in 8000..192000 && duration in 3.0..30.01) { "Gunakan sampel suara 3–30 detik." }
        require(samples.all { it.isFinite() }) { "Sampel audio rusak." }
        require(rms > .002) { "Suara terlalu pelan atau hening. Rekam ulang lebih dekat ke mikrofon." }
    }
    /** Windowed-sinc anti-alias filter. 64 taps; bounded 30 s references, offline IO thread. */
    fun resample(rate: Int): PcmAudio {
        require(rate in 8000..48000 && sampleRate in 8000..192000)
        if (rate == sampleRate) return this
        val ratio = sampleRate.toDouble()/rate
        val cutoff = min(1.0,rate.toDouble()/sampleRate) * .94
        val out = FloatArray((samples.size / ratio).roundToInt())
        out.indices.forEach { i ->
            val pos = i * ratio
            val center = floor(pos).toInt()
            var sum = 0.0; var weight = 0.0
            for (j in center-31..center+32) {
                if (j !in samples.indices) continue
                val distance = pos-j
                val x = Math.PI*distance*cutoff
                val sinc = if (abs(x)<1e-10) 1.0 else sin(x)/x
                val window = .42 + .5*cos(Math.PI*distance/32) + .08*cos(2*Math.PI*distance/32)
                val w = cutoff*sinc*window
                sum += samples[j]*w; weight += w
            }
            out[i] = if (abs(weight)<1e-10) 0f else (sum/weight).toFloat()
        }
        return PcmAudio(out,rate)
    }
    fun waveform(bins: Int = 72): List<Float> = List(bins) { i ->
        val start = i*samples.size/bins
        val end = ((i+1)*samples.size/bins).coerceAtLeast(start+1).coerceAtMost(samples.size)
        (start until end).maxOfOrNull { abs(samples[it]) } ?: 0f
    }
    fun writeWav(file: File, synthetic: Boolean = false) {
        require(samples.isNotEmpty() && samples.all { it.isFinite() } && duration <= 180)
        file.parentFile?.mkdirs()
        val marker = if (synthetic) "VoClone synthetic audio\u0000".toByteArray() else byteArrayOf()
        val infoSize = if (synthetic) 12 + marker.size + marker.size%2 else 0
        FileOutputStream(file).use { output ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray()).putInt(36+samples.size*2+if(synthetic) 8+infoSize else 0).put("WAVEfmt ".toByteArray())
            header.putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate*2).putShort(2).putShort(16)
            header.put("data".toByteArray()).putInt(samples.size*2)
            output.write(header.array())
            val buffer = ByteBuffer.allocate(16384).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { v ->
                if (buffer.remaining()<2) { output.write(buffer.array(),0,buffer.position());buffer.clear() }
                buffer.putShort((v.coerceIn(-1f,1f)*32767).roundToInt().toShort())
            }
            output.write(buffer.array(),0,buffer.position())
            if (synthetic) {
                val info=ByteBuffer.allocate(8+infoSize).order(ByteOrder.LITTLE_ENDIAN)
                info.put("LIST".toByteArray()).putInt(infoSize).put("INFOICMT".toByteArray()).putInt(marker.size).put(marker)
                output.write(info.array())
            }
            output.fd.sync()
        }
    }
    companion object {
        fun readWav(file: File): PcmAudio = RandomAccessFile(file,"r").use { input ->
            fun text(n: Int): String { val b=ByteArray(n); input.readFully(b);return b.toString(Charsets.US_ASCII) }
            fun int(): Int = Integer.reverseBytes(input.readInt())
            fun short(): Int = java.lang.Short.reverseBytes(input.readShort()).toInt() and 65535
            require(file.length() in 44..(180L*192000*2+4096)) { "Ukuran WAV tidak valid." }
            require(text(4)=="RIFF") { "Berkas bukan WAV." }
            val declared=int().toLong()+8
            require(declared==file.length() && text(4)=="WAVE") { "WAV terpotong atau rusak." }
            var rate=0; var channels=0; var bits=0; var format=0; var pcm: FloatArray?=null
            while(input.filePointer+8<=declared) {
                val tag=text(4);val size=int();val start=input.filePointer
                require(size>=0 && start+size+(size%2)<=declared) { "Chunk WAV rusak." }
                if(tag=="fmt ") {
                    require(size>=16); format=short();channels=short();rate=int();int();short();bits=short()
                    require(format==1 && channels==1 && bits==16 && rate in 8000..192000) { "WAV harus PCM mono 16-bit." }
                } else if(tag=="data") {
                    require(rate>0 && size%2==0 && size.toLong()<=rate*180L*2)
                    pcm=FloatArray(size/2) { java.lang.Short.reverseBytes(input.readShort()).toFloat()/32768 }
                }
                input.seek(start+size+(size%2))
            }
            PcmAudio(pcm ?: error("WAV tidak memiliki audio."),rate)
        }
    }
}
