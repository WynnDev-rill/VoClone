package id.wynn.voclone

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(vm: StudioViewModel,modifier: Modifier=Modifier,onBack: ()->Unit,onTask: ()->Unit,onInstallUpdate: ()->Unit) {
    var legal by rememberSaveable { mutableStateOf(false) }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var deleteModels by remember { mutableStateOf(false) }
    val update by vm.updater.state.collectAsState()
    val automatic by vm.updater.automatic.collectAsState()
    val context=LocalContext.current
    BackHandler(legal) { legal=false }
    Column(modifier.fillMaxSize()) {
        TopAppBar(title={ Text(if(legal)"Lisensi & sumber" else "Pengaturan") },navigationIcon={ IconButton(onClick={ if(legal)legal=false else onBack() }) { AppIcon(R.drawable.ic_arrow_back,"Kembali") } })
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
            if(legal)LegalPage(Modifier.widthIn(max=640.dp).fillMaxSize())
            else LazyColumn(Modifier.widthIn(max=640.dp).fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                item {
                    SectionHeading("Model AI offline")
                    Text(if(vm.installed)"Model terpasang • ${bytes(ModelCatalog.totalBytes)}" else "Unduh satu kali: ${bytes(ModelCatalog.totalBytes)}",style=MaterialTheme.typography.titleMedium)
                    Text("OmniVoice Q4 + tokenizer: ${bytes(659959328)}\nWhisper Tiny: ${bytes(77691713)}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Setelah model siap, cloning dan transkripsi berjalan tanpa internet. Unduhan terputus dapat dilanjutkan.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(!vm.accepted)item {
                    Text("Bobot OmniVoice dibatasi untuk penggunaan nonkomersial; tokenizer tunduk pada lisensi Higgs Audio 2 dan Meta Llama 3. Baca ketentuan lengkap sebelum melanjutkan.",style=MaterialTheme.typography.bodyMedium)
                    TextButton(onClick={ legal=true }) { Text("Baca lisensi model & atribusi") }
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=agreed,role=Role.Checkbox,onValueChange={ agreed=it }).semantics(mergeDescendants=true) { },verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=agreed,onCheckedChange=null)
                        Spacer(Modifier.width(8.dp))
                        Text("Saya membaca dan menyetujui ketentuan model.",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                    }
                    Button(onClick={ onTask();vm.acceptAndDownload() },enabled=agreed && !vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Setujui & unduh model") }
                } else item {
                    if(!vm.installed)Button(onClick={ onTask();vm.download() },enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Unduh / lanjutkan model") }
                    else OutlinedButton(onClick={ deleteModels=true },enabled=!vm.busy,modifier=Modifier.heightIn(min=48.dp)) { Text("Hapus model dari perangkat") }
                }
                if(vm.busy)item {
                    val total=vm.work?.progress?.getLong("total",0) ?: 0
                    val done=vm.work?.progress?.getLong("done",0) ?: 0
                    Text(vm.work?.progress?.getString("phase") ?: "Menunggu tugas dimulai…")
                    if(total>0) {
                        LinearProgressIndicator(progress={ (done.toFloat()/total).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
                        Text(if(total==100L)"$done%" else "${bytes(done)} / ${bytes(total)}",style=MaterialTheme.typography.bodySmall)
                    } else LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick=vm::cancel) { Text("Batalkan tugas") }
                }
                vm.error?.let { error -> item { Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium) } }
                item {
                    HorizontalDivider();Spacer(Modifier.height(16.dp));SectionHeading("Tampilan")
                    listOf("system" to "Ikuti sistem","light" to "Terang","dark" to "Gelap").forEach { (value,label) ->
                        Row(Modifier.fillMaxWidth().selectable(selected=vm.theme==value,role=Role.RadioButton,onClick={ vm.setAppearance(value) }).semantics(mergeDescendants=true) { }.heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(selected=vm.theme==value,onClick=null);Text(label)
                        }
                    }
                }
                item {
                    HorizontalDivider();Spacer(Modifier.height(16.dp));SectionHeading("Pembaruan aplikasi")
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("Periksa otomatis saat dibuka",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                        Switch(checked=automatic,onCheckedChange={ vm.updater.automatic(it) },modifier=Modifier.semantics { contentDescription="Periksa pembaruan otomatis" })
                    }
                    Text("Pemeriksaan maksimal sekali tiap 12 jam. Android meminta persetujuan pemasangan. APK harus memiliki tanda tangan yang sama.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(update.message,color=if(update.error)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
                    update.progress?.let { p -> LinearProgressIndicator(progress={ p },modifier=Modifier.fillMaxWidth().padding(top=8.dp)) }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={ vm.checkUpdate() },enabled=!update.checking && update.progress==null,modifier=Modifier.heightIn(min=48.dp)) { Text("Periksa update") }
                        if(update.available!=null)Button(onClick={ if(update.ready)onInstallUpdate() else vm.downloadUpdate() },enabled=update.progress==null,modifier=Modifier.heightIn(min=48.dp)) { Text(if(update.ready)"Pasang" else "Unduh") }
                    }
                }
                item {
                    HorizontalDivider();Spacer(Modifier.height(16.dp));SectionHeading("Tentang VoClone")
                    Text("Versi ${BuildConfig.VERSION_NAME} • ARM64",style=MaterialTheme.typography.bodyMedium)
                    Text("Versi alpha. Kualitas identitas suara, bahasa Indonesia/Jepang, suhu, dan performa POCO F6 masih memerlukan pengujian perangkat fisik.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Sampel dan profil tersimpan privat di perangkat. Android backup dinonaktifkan. Jangan hapus aplikasi sebelum menyimpan audio yang diperlukan.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick={ legal=true }) { Text("Lisensi, atribusi & ketentuan model") }
                    TextButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/WynnDev-rill/VoClone"))) }) { Text("Kode sumber & catatan rilis") }
                }
            }
        }
    }
    if(deleteModels)AlertDialog(onDismissRequest={ deleteModels=false },title={ Text("Hapus model AI?") },text={ Text("Profil suara dan audio tetap tersimpan. Model perlu diunduh lagi untuk generasi berikutnya.") },confirmButton={ TextButton(onClick={ vm.deleteModels();deleteModels=false }) { Text("Hapus model") } },dismissButton={ TextButton(onClick={ deleteModels=false }) { Text("Batal") } })
}
@Composable private fun SectionHeading(text: String) { Text(text,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(bottom=8.dp).semantics { heading() }) }
@Composable private fun LegalPage(modifier: Modifier) {
    val context=LocalContext.current
    var documents by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        documents=withContext(Dispatchers.IO) { context.assets.list("licenses").orEmpty().sorted().map { name -> name to context.assets.open("licenses/$name").bufferedReader().use { it.readText() } } }
    }
    LazyColumn(modifier,contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {
            Text("Ketentuan penggunaan model",style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp));Text(ModelCatalog.NOTICE,style=MaterialTheme.typography.bodyMedium)
        }
        documents.forEach { (name,text) -> item(key=name) {
            Text(name,style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp));Text(text,style=MaterialTheme.typography.bodySmall)
        } }
    }
}
