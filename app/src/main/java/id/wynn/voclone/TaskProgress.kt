package id.wynn.voclone

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

fun elapsedTime(milliseconds: Long): String {
    val seconds=milliseconds.coerceAtLeast(0)/1000
    return String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60)
}

@Composable fun TaskProgress(data: Data,onCancel: ()->Unit) {
    val start=data.getLong("startedElapsedMs",0)
    val updated=data.getLong("updatedElapsedMs",0)
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val now by produceState(SystemClock.elapsedRealtime(),start,lifecycle) {
        if(start>0)lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive) { value=SystemClock.elapsedRealtime();delay(1000) }
        }
    }
    val done=data.getLong("done",0)
    val total=data.getLong("total",0)
    val kind=data.getString("kind")
    val stage=data.getInt("stage",-1)
    Column(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) {
        Text(data.getString("phase") ?: "Menunggu tugas dimulai…",style=MaterialTheme.typography.bodyMedium)
        val measurable=total>0 && (kind=="download" || kind=="transcription" || kind=="inference" && (stage==0 || stage==3))
        if(measurable)LinearProgressIndicator(progress={ (done.toFloat()/total).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
        else LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            kind=="download" && total>0 -> Text("${bytes(done)} / ${bytes(total)}",style=MaterialTheme.typography.bodySmall)
            stage==3 && total>0 -> Text("Bagian ${data.getInt("chunk",1)} • iterasi selesai $done/$total",style=MaterialTheme.typography.bodySmall)
            measurable -> Text("$done% transkripsi",style=MaterialTheme.typography.bodySmall)
        }
        if(start>0)Text("Berjalan ${elapsedTime(now-start)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(kind=="inference" && updated>0 && now-updated>=30000)
            Text("Progres terakhir ${elapsedTime(now-updated)} lalu",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick=onCancel) { Text("Batalkan") }
    }
}
