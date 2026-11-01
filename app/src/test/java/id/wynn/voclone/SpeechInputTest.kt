package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.UUID

class SpeechInputTest {
    @Test fun fullTextSurvivesSmallWorkPayload() {
        val dir=Files.createTempDirectory("speech-input").toFile()
        try {
            val text="日本語 dan bahasa Indonesia 🗣️ ".repeat(3000)
            val input=prepareSpeechInput(dir,text,UUID.randomUUID().toString(),"none")
            assertTrue(input.toByteArray().size<1024)
            assertEquals(text,speechTextFile(dir,input.getString("textFile")!!).readText())
        } finally { dir.deleteRecursively() }
    }
    @Test fun invalidTextAndPathsAreRejected() {
        val dir=Files.createTempDirectory("speech-input").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) { speechTextFile(dir,"../../outside") }
            for(text in listOf(" ","test\u0000")) assertThrows(IllegalArgumentException::class.java) { prepareSpeechInput(dir,text,UUID.randomUUID().toString(),"none") }
        } finally { dir.deleteRecursively() }
    }
}
