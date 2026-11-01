package id.wynn.voclone

import kotlinx.serialization.Serializable

data class InferenceProgress(val stage: Int,val done: Int,val total: Int,val backend: String?=null) {
    init { require(stage in 0..6 && total in 0..10000 && done in 0..total) { "Progres native tidak valid." } }
    val label: String get()=when(stage) {
        0 -> "Whisper sedang mentranskripsikan…"
        1 -> "Memuat OmniVoice…"
        2 -> "Memproses referensi suara…"
        3 -> "Menyintesis suara di perangkat…"
        4 -> "Mendekode audio…"
        5 -> "Memvalidasi audio…"
        else -> "Memeriksa model…"
    }
}

@Serializable data class InferenceSnapshot(val stage: Int,val done: Int,val total: Int,val chunk: Int,val elapsedMs: Long,val sinceProgressMs: Long,val stageMs: Map<Int,Long>,val backend: String)

/** Monotonic timings and actual callbacks, never a timer-derived percentage. */
class InferenceTrace(private val startedMs: Long) {
    private var progress=InferenceProgress(6,0,0)
    private var lastMs=startedMs
    private var stageStarted=startedMs
    private var chunk=0
    private var backend="unselected"
    private val durations=mutableMapOf<Int,Long>()
    @Synchronized fun record(event: InferenceProgress,nowMs: Long) {
        require(nowMs>=lastMs)
        if(event.stage==progress.stage && event.done==progress.done && event.total==progress.total && event.backend==null)return
        if(event.stage!=progress.stage) {
            durations[progress.stage]=(durations[progress.stage] ?: 0)+(nowMs-stageStarted)
            stageStarted=nowMs
            if(event.stage==3)chunk++
        }
        event.backend?.let { backend=it }
        progress=event;lastMs=nowMs
    }
    @Synchronized fun snapshot(nowMs: Long): InferenceSnapshot {
        require(nowMs>=lastMs)
        val timing=durations.toMutableMap()
        timing[progress.stage]=(timing[progress.stage] ?: 0)+(nowMs-stageStarted)
        return InferenceSnapshot(progress.stage,progress.done,progress.total,chunk,nowMs-startedMs,nowMs-lastMs,timing,backend)
    }
}

fun useI8mm(primaryAbi: String?,capable: Boolean): Boolean=primaryAbi=="arm64-v8a" && capable
