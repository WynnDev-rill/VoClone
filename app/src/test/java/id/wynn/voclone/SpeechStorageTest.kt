package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID

class SpeechStorageTest {
    @Test fun processDeathStagesAreReclaimedAndActiveRetryDataIsPreserved() {
        val root=Files.createTempDirectory("speech-cleanup").toFile()
        val active=UUID.randomUUID();val ended=UUID.randomUUID()
        fun stage(name: String)=File(root,"results/$name").apply { parentFile.mkdirs();writeText("interrupted bytes") }
        try {
            val activeStage=stage("$active.part");val endedStage=stage("$ended.part")
            val completed=stage("$ended.wav")
            val activeParts=File(root,"results/$active.parts").apply { mkdirs();File(this,"0.wav").writeText("verified part fixture") }
            val endedParts=File(root,"results/$ended.parts").apply { mkdirs();File(this,"0.wav").writeText("terminal part fixture") }
            pruneSpeechFiles(root,System.currentTimeMillis(),{ it==active },{ false })
            assertTrue(activeStage.exists() && activeParts.exists() && completed.exists())
            assertFalse("A terminal worker's interrupted final WAV must be reclaimed",endedStage.exists())
            assertFalse(endedParts.exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun oldPreparationStagesAreReclaimedWithoutRacingFreshOrOwnedText() {
        val root=Files.createTempDirectory("speech-text-cleanup").toFile();val now=System.currentTimeMillis()
        fun text(extension: String,age: Long)=File(root,"texts/${UUID.randomUUID()}.$extension").apply { parentFile.mkdirs();writeText("private draft fixture");setLastModified(now-age) }
        try {
            val orphan=text("part",172800000);val recent=text("part",1000);val owned=text("txt",172800000)
            pruneSpeechFiles(root,now,{ false },{ it==owned.nameWithoutExtension })
            assertFalse("A process-killed text preparation must not persist forever",orphan.exists())
            assertTrue(recent.exists() && owned.exists())
        } finally { root.deleteRecursively() }
    }
}
