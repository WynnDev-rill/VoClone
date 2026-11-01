package id.wynn.voclone

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DiagnosticStoreTest {
    @Test fun exportDropsUnknownPrivateFieldsAndNeverReadsAudio() {
        val root=java.nio.file.Files.createTempDirectory("diagnostic").toFile()
        try {
            val store=DiagnosticStore(root)
            val id=UUID.randomUUID().toString()
            store.write(TaskDiagnostic(id,"clone","running",InferenceTrace(0).snapshot(20),100))
            val file=File(root,"$id.json")
            file.writeText(file.readText().replaceFirst("{","{\"transcript\":\"PRIVATE-VOICE-TEXT\","))
            File(root,"recording.wav").writeText("PRIVATE-VOICE-AUDIO")
            val export=Json.encodeToString(store.read())
            assertTrue(export.contains("clone"))
            assertFalse(export.contains("PRIVATE-VOICE"))
            assertFalse(export.contains("recording.wav"))
        } finally { root.deleteRecursively() }
    }
    @Test fun boundedReportsIgnoreCorruptFilesAndRejectPathTraversal() {
        val root=java.nio.file.Files.createTempDirectory("diagnostic").toFile()
        try {
            val store=DiagnosticStore(root)
            for(i in 0..24)store.write(TaskDiagnostic(UUID.randomUUID().toString(),"generate","succeeded",InferenceTrace(0).snapshot(i.toLong()),100L+i))
            assertTrue(root.listFiles()!!.size<=20)
            File(root,"${UUID.randomUUID()}.json").writeText("broken JSON")
            assertTrue(store.read().size in 1..5)
            assertThrows(IllegalArgumentException::class.java) { store.write(TaskDiagnostic("../escape","clone","running",InferenceTrace(0).snapshot(1),1)) }
            assertFalse(File(root.parentFile,"escape.json").exists())
        } finally { root.deleteRecursively() }
    }
}
