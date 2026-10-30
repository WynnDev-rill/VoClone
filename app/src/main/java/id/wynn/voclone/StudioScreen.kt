package id.wynn.voclone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.ImageDecoder
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StudioScreen(vm: StudioViewModel,onImport: ()->Unit,onRecord: ()->Unit,onTask: ()->Unit,onSave: (String)->Unit,onShare: (String)->Unit,onInstallUpdate: ()->Unit) {
    val resources=LocalContext.current.resources
    val logo=remember(resources) { ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources,R.drawable.voclone_icon)) { decoder,_,_ -> decoder.setTargetSize(128,128) }.asImageBitmap() }
    var settings by rememberSaveable { mutableStateOf(false) }
    var rename by remember { mutableStateOf<VoiceProfile?>(null) }
    var delete by remember { mutableStateOf<VoiceProfile?>(null) }
    var renameText by remember { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.referenceId) { consent=false }
    var dismissedUpdate by rememberSaveable { mutableLongStateOf(0L) }
    val update by vm.updater.state.collectAsState()
    BackHandler(settings) { settings=false }
    Scaffold(containerColor=MaterialTheme.colorScheme.background) { insets ->
        if(settings) {
            SettingsScreen(vm,Modifier.padding(insets),onBack={ settings=false },onTask=onTask,onInstallUpdate=onInstallUpdate)
        } else {
            Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding(),contentAlignment=Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max=640.dp).fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Image(logo,null,Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(10.dp))
                            Text("Vo",style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary)
                            Text("Clone",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f))
                            IconButton(onClick={ settings=true },modifier=Modifier.size(48.dp)) { AppIcon(R.drawable.ic_settings,"Pengaturan") }
                        }
                    }
                    item {
                        Column(Modifier.padding(top=8.dp,bottom=8.dp)) {
                            Text("Clone Your Voice",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.semantics { heading() })
                            Spacer(Modifier.height(8.dp))
                            Text("Suara Anda. Sepenuhnya di perangkat.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if(!vm.installed) item {
                        Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(12.dp),onClick={ settings=true }) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                                AppIcon(R.drawable.ic_download,null,tint=MaterialTheme.colorScheme.onPrimaryContainer)
                                Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                                    Text("Siapkan model offline",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("${bytes(ModelCatalog.totalBytes)} • unduh sekali",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                AppIcon(R.drawable.ic_chevron_right,"Buka model",tint=MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    item {
                        Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    AppIcon(R.drawable.ic_graphic_eq,null,tint=MaterialTheme.colorScheme.primary)
                                    Text("Audio Reference",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f).padding(start=8.dp))
                                    Text("MP3 / WAV",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if(vm.recording) {
                                    Text("Merekam • ${vm.recordingSeconds} / 30 detik",color=MaterialTheme.colorScheme.primary,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                                    LinearProgressIndicator(progress={ vm.recordingSeconds/30f },modifier=Modifier.fillMaxWidth())
                                } else {
                                    AudioPlayer(vm,vm.referenceFile(),remember(vm.reference) { vm.reference?.waveform().orEmpty() },vm.reference?.duration ?: 0.0,emptyLabel="Impor atau rekam 3–30 detik")
                                    vm.reference?.let { audio ->
                                        Text(if(audio.clipped) "Sinyal clipping • rekam ulang disarankan" else "Mono 24 kHz • ${String.format(Locale.US,"%.1f",audio.duration)} detik",style=MaterialTheme.typography.bodySmall,color=if(audio.clipped)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if(vm.importing)LinearProgressIndicator(Modifier.fillMaxWidth())
                                BoxWithConstraints {
                                    val compact=maxWidth<280.dp
                                    if(compact)Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick=onImport,enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { ButtonIcon(R.drawable.ic_file_upload);Text("Import Audio") }
                                        RecordButton(vm,onRecord,Modifier.fillMaxWidth())
                                    } else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(onClick=onImport,enabled=!vm.busy,modifier=Modifier.weight(1f).heightIn(min=52.dp)) { ButtonIcon(R.drawable.ic_file_upload);Text("Import") }
                                        RecordButton(vm,onRecord,Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    if(vm.reference!=null) item {
                        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value=vm.transcript,onValueChange={ vm.transcript=it.take(2000) },label={ Text("Ucapan persis dalam sampel") },supportingText={ Text("Periksa transkrip agar karakter suara lebih konsisten.") },minLines=2,maxLines=5,enabled=!vm.busy,modifier=Modifier.fillMaxWidth())
                            OutlinedButton(onClick={ onTask();vm.transcribe() },enabled=!vm.busy && vm.installed,modifier=Modifier.heightIn(min=48.dp)) { ButtonIcon(R.drawable.ic_subtitles);Text("Transkripsi dengan Whisper") }
                            OutlinedTextField(value=vm.voiceName,onValueChange={ vm.voiceName=it.take(60) },label={ Text("Nama suara") },singleLine=true,enabled=!vm.busy,modifier=Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth().toggleable(value=consent,enabled=!vm.busy,role=Role.Checkbox,onValueChange={ consent=it }).semantics(mergeDescendants=true) { },verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(checked=consent,onCheckedChange=null,enabled=!vm.busy)
                                Text("Saya memiliki suara ini atau izin pemiliknya.",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        Button(onClick={ onTask();vm.cloneVoice() },enabled=!vm.busy && vm.reference!=null && vm.transcript.isNotBlank() && vm.installed && consent,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)) {
                            ButtonIcon(R.drawable.ic_auto_awesome);Text("Clone Voice",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.width(12.dp));AppIcon(R.drawable.ic_arrow_forward,null)
                        }
                    }
                    if(vm.busy && !vm.recording && !vm.importing) item {
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) {
                            val phase=vm.work?.progress?.getString("phase") ?: "Menunggu tugas dimulai…"
                            val done=vm.work?.progress?.getLong("done",0) ?: 0
                            val total=vm.work?.progress?.getLong("total",0) ?: 0
                            Text(phase,style=MaterialTheme.typography.bodyMedium)
                            if(total>0) {
                                LinearProgressIndicator(progress={ (done.toFloat()/total).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
                                Text(if(total==100L)"$done%" else "${bytes(done)} / ${bytes(total)}",style=MaterialTheme.typography.bodySmall)
                            } else LinearProgressIndicator(Modifier.fillMaxWidth())
                            TextButton(onClick=vm::cancel) { Text("Batalkan") }
                        }
                    }
                    vm.error?.let { error -> item {
                        Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(error,color=MaterialTheme.colorScheme.onErrorContainer,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Assertive })
                                TextButton(onClick={ vm.error=null }) { Text("Tutup",color=MaterialTheme.colorScheme.onErrorContainer) }
                            }
                        }
                    } }
                    vm.message?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) } }
                    item { Text("My Voices",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=12.dp).semantics { heading() }) }
                    if(vm.voices.isEmpty())item {
                        Text("Belum ada suara tersimpan. Profil muncul setelah pratinjau berhasil dibuat.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(vm.voices,key={ it.id }) { voice ->
                        VoiceRow(vm,voice,onRename={ rename=voice;renameText=voice.name },onDelete={ delete=voice })
                    }
                    item {
                        Column(Modifier.padding(top=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text("Generate Speech",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics { heading() })
                            Text(vm.voices.firstOrNull { it.id==vm.selected }?.let { "Menggunakan ${it.name}" } ?: "Pilih suara dari My Voices.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value=vm.text,onValueChange={ vm.text=it.take(600) },label={ Text("Teks untuk diucapkan") },supportingText={ Text("${vm.text.length} / 600") },minLines=4,maxLines=8,enabled=!vm.busy,modifier=Modifier.fillMaxWidth())
                            LanguagePicker(vm)
                            Button(onClick={ onTask();vm.generate() },enabled=!vm.busy && vm.installed && vm.selected!=null && vm.text.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(12.dp)) { ButtonIcon(R.drawable.ic_play_arrow);Text("Generate Audio") }
                        }
                    }
                    vm.result?.let { result -> item {
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("Audio hasil",style=MaterialTheme.typography.titleMedium)
                            AudioPlayer(vm,result,emptyList(),0.0)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick={ onSave("wav") },modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Text("Simpan WAV") }
                                OutlinedButton(onClick={ onSave("mp3") },modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Text("Simpan MP3") }
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick={ onShare("wav") },modifier=Modifier.weight(1f).heightIn(min=48.dp)) { ButtonIcon(R.drawable.ic_share);Text("Bagikan WAV") }
                                TextButton(onClick={ onShare("mp3") },modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Text("Bagikan MP3") }
                            }
                        }
                    } }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
    if(rename!=null)AlertDialog(onDismissRequest={ rename=null },title={ Text("Ubah nama suara") },text={ OutlinedTextField(value=renameText,onValueChange={ renameText=it.take(60) },label={ Text("Nama suara") },singleLine=true) },confirmButton={ TextButton(onClick={ rename?.let { vm.rename(it,renameText) };rename=null },enabled=renameText.isNotBlank()) { Text("Simpan") } },dismissButton={ TextButton(onClick={ rename=null }) { Text("Batal") } })
    delete?.let { voice -> AlertDialog(onDismissRequest={ delete=null },title={ Text("Hapus ${voice.name}?") },text={ Text("Referensi dan pratinjau profil ini akan dihapus dari perangkat.") },confirmButton={ TextButton(onClick={ vm.delete(voice);delete=null }) { Text("Hapus") } },dismissButton={ TextButton(onClick={ delete=null }) { Text("Batal") } }) }
    update.available?.takeIf { it.versionCode!=dismissedUpdate }?.let { release ->
        AlertDialog(onDismissRequest={ dismissedUpdate=release.versionCode },title={ Text("VoClone ${release.versionName} tersedia") },text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(release.notes);Text(update.message);update.progress?.let { p -> LinearProgressIndicator(progress={ p },modifier=Modifier.fillMaxWidth()) };if(update.error)Text("Coba lagi dari Pengaturan.",color=MaterialTheme.colorScheme.error) } },confirmButton={ TextButton(onClick={ if(update.ready)onInstallUpdate() else vm.downloadUpdate() },enabled=update.progress==null) { Text(if(update.ready)"Pasang update" else "Unduh ${bytes(release.sizeBytes)}") } },dismissButton={ TextButton(onClick={ dismissedUpdate=release.versionCode }) { Text("Nanti") } })
    }
}
@Composable private fun ButtonIcon(icon: Int) { AppIcon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp)) }
@Composable private fun RecordButton(vm: StudioViewModel,onRecord: ()->Unit,modifier: Modifier) {
    FilledTonalButton(onClick=onRecord,enabled=!vm.busy || vm.recording,modifier=modifier.heightIn(min=52.dp)) { ButtonIcon(if(vm.recording)R.drawable.ic_stop else R.drawable.ic_mic);Text(if(vm.recording)"Stop" else "Record") }
}
@Composable fun AudioPlayer(vm: StudioViewModel,file: File?,waveform: List<Float>,duration: Double,emptyLabel: String="") {
    val metadata by produceState(emptyList<Float>() to 0.0,file) {
        if(file!=null && waveform.isEmpty())value=withContext(Dispatchers.IO) { runCatching { PcmAudio.readWav(file).let { it.waveform() to it.duration } }.getOrDefault(emptyList<Float>() to 0.0) }
    }
    val displayWave=if(waveform.isNotEmpty())waveform else metadata.first
    val displayDuration=if(duration>0)duration else metadata.second
    val isPlaying=file!=null && vm.playingPath==file.path
    val primary=MaterialTheme.colorScheme.primary
    val idle=MaterialTheme.colorScheme.outlineVariant
    Column {
        Row(verticalAlignment=Alignment.CenterVertically) {
            FilledTonalIconButton(onClick={ file?.let(vm::play) },enabled=file!=null,modifier=Modifier.size(48.dp)) { AppIcon(if(isPlaying)R.drawable.ic_stop else R.drawable.ic_play_arrow,if(isPlaying)"Hentikan audio" else "Putar audio") }
            Spacer(Modifier.width(12.dp))
            Canvas(Modifier.weight(1f).height(64.dp).pointerInput(file,isPlaying) { detectTapGestures { offset -> if(file!=null) { if(!isPlaying)vm.play(file);vm.seek(offset.x/size.width) } } }.semantics { contentDescription=if(file==null)"Belum ada sampel audio" else "Waveform audio. Gunakan penggeser di bawah untuk mengatur posisi." }) {
                if(displayWave.isEmpty())drawLine(idle,androidx.compose.ui.geometry.Offset(0f,size.height/2),androidx.compose.ui.geometry.Offset(size.width,size.height/2),2.dp.toPx())
                else {
                    val peak=(displayWave.maxOrNull() ?: 1f).coerceAtLeast(.001f)
                    val step=size.width/displayWave.size
                    displayWave.forEachIndexed { i,value ->
                        val h=(value/peak*size.height*.85f).coerceAtLeast(2.dp.toPx())
                        val color=if(isPlaying && i.toFloat()/displayWave.size<=vm.playbackPosition)primary else primary.copy(alpha=.45f)
                        drawLine(color,androidx.compose.ui.geometry.Offset(i*step+step/2,(size.height-h)/2),androidx.compose.ui.geometry.Offset(i*step+step/2,(size.height+h)/2),2.dp.toPx(),StrokeCap.Round)
                    }
                }
            }
        }
        if(file==null)Text(emptyLabel,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            val seconds=if(isPlaying)vm.playbackDuration/1000.0 else displayDuration
            Text(if(seconds>0)formatTime(if(isPlaying)vm.playbackPosition*seconds else 0.0)+" / "+formatTime(seconds) else "WAV • audio sintetis",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(isPlaying)Slider(value=vm.playbackPosition,onValueChange=vm::seek,modifier=Modifier.fillMaxWidth().semantics { contentDescription="Posisi pemutaran audio" })
        }
    }
}
@Composable private fun VoiceRow(vm: StudioViewModel,voice: VoiceProfile,onRename: ()->Unit,onDelete: ()->Unit) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            FilledTonalIconButton(onClick={ vm.play(vm.previewFile(voice)) },modifier=Modifier.size(48.dp)) { AppIcon(if(vm.playingPath==vm.previewFile(voice).path)R.drawable.ic_stop else R.drawable.ic_graphic_eq,"Pratinjau ${voice.name}") }
            Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                Text(voice.name,style=MaterialTheme.typography.titleMedium)
                Text("${formatTime(voice.duration)} • tersimpan lokal",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick={ vm.select(voice.id) },enabled=!vm.busy,modifier=Modifier.heightIn(min=48.dp).semantics { selected=vm.selected==voice.id }) { Text(if(vm.selected==voice.id)"Dipilih" else "Use") }
            Box {
                IconButton(onClick={ menu=true },enabled=!vm.busy,modifier=Modifier.size(48.dp)) { AppIcon(R.drawable.ic_more_vert,"Opsi ${voice.name}") }
                DropdownMenu(expanded=menu,onDismissRequest={ menu=false }) {
                    DropdownMenuItem(text={ Text("Ubah nama") },onClick={ menu=false;onRename() })
                    DropdownMenuItem(text={ Text("Buat ulang dari referensi") },onClick={ menu=false;vm.recreate(voice) })
                    DropdownMenuItem(text={ Text("Hapus") },onClick={ menu=false;onDelete() })
                }
            }
        }
        HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
    }
}
@Composable private fun LanguagePicker(vm: StudioViewModel) {
    var open by remember { mutableStateOf(false) }
    val options=listOf("none" to "Otomatis","id" to "Indonesia (uji)","ja" to "Jepang (uji)","en" to "English")
    Box {
        OutlinedButton(onClick={ open=true },enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Bahasa: ${options.first { it.first==vm.language }.second}",Modifier.weight(1f));AppIcon(R.drawable.ic_expand_more,null) }
        DropdownMenu(expanded=open,onDismissRequest={ open=false }) { options.forEach { (code,label) -> DropdownMenuItem(text={ Text(label) },onClick={ vm.language=code;open=false }) } }
    }
}
fun bytes(value: Long): String = String.format(Locale.US,"%.1f MiB",value/1048576.0)
private fun formatTime(seconds: Double): String = "%02d:%02d".format(Locale.US,seconds.toInt()/60,seconds.toInt()%60)
