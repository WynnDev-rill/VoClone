package id.wynn.voclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        setContent {
            val vm: StudioViewModel=viewModel()
            VoCloneTheme(vm.theme) {
                val scope=rememberCoroutineScope()
                val lifecycle=LocalLifecycleOwner.current
                DisposableEffect(lifecycle) {
                    val observer=LifecycleEventObserver { _,event ->
                        if(event==Lifecycle.Event.ON_STOP) { vm.stopRecording();vm.stopPlayback() }
                        if(event==Lifecycle.Event.ON_RESUME)vm.checkUpdate(automatic=true)
                    }
                    lifecycle.lifecycle.addObserver(observer)
                    onDispose { lifecycle.lifecycle.removeObserver(observer);vm.stopRecording();vm.stopPlayback() }
                }
                LaunchedEffect(Unit) { vm.checkUpdate(automatic=true) }
                val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importAudio) }
                val microphone=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted)vm.record() else vm.error="Izin mikrofon ditolak. Anda tetap dapat mengimpor audio." }
                val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                val saveWav=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { it?.let { uri -> vm.save(uri,"wav") } }
                val saveMp3=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { it?.let { uri -> vm.save(uri,"mp3") } }
                StudioScreen(vm,
                    onImport={ importer.launch(arrayOf("audio/mpeg","audio/wav","audio/x-wav")) },
                    onRecord={
                        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record()
                        else microphone.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onTask={ if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onSave={ format -> if(format=="wav")saveWav.launch("VoClone-${System.currentTimeMillis()}.wav") else saveMp3.launch("VoClone-${System.currentTimeMillis()}.mp3") },
                    onShare={ format -> scope.launch {
                        try {
                            val file=vm.exportFile(format)
                            val uri=FileProvider.getUriForFile(this@MainActivity,"$packageName.files",file)
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(if(format=="wav")"audio/wav" else "audio/mpeg").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Bagikan audio"))
                        } catch(e: Exception) { vm.error=e.message ?: "Audio gagal dibagikan." }
                    } },
                    onInstallUpdate={ scope.launch { vm.updater.install()?.let { startActivity(it) } } }
                )
            }
        }
    }
}
