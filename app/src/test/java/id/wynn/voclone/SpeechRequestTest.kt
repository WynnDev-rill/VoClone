package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test

class SpeechRequestTest {
    @Test fun longJapaneseRecordingProducesOnlyShortPreviewWithoutCroppingTranscript() {
        val transcript="もっと逃げる前に話したいことがあります。".repeat(12)
        val input=cloneInput("reference",transcript,"Furina","none")
        val preview=input.getString("text")!!
        assertTrue("Preview must not re-synthesize the entire recording",preview.codePointCount(0,preview.length)<=32)
        assertTrue(preview.any { Character.UnicodeScript.of(it.code)==Character.UnicodeScript.HIRAGANA })
        assertEquals(transcript,input.getString("transcript"))
        assertEquals("none",input.getString("language"))
    }
    @Test fun explicitLanguagePreviewIsShortEvenWhenReferenceUsesAnotherScript() {
        for(language in listOf("en","id","ja")) {
            val input=cloneInput("ref","これは長い参照音声です。".repeat(30),"Voice",language)
            val preview=input.getString("text")!!
            assertTrue(preview.isNotBlank() && preview.codePointCount(0,preview.length)<=32)
            if(language!="ja")assertTrue(preview.all { it.code<128 })
            assertEquals(language,input.getString("language"))
        }
    }
    @Test fun autoExcerptPreservesWholeWordsAndUnicodeCodepoints() {
        val preview=cloneInput("ref","Hello from my own voice, recording a longer reference.","Voice","none").getString("text")!!
        assertEquals("Hello from my own",preview)
        val unicode=cloneInput("ref","😀".repeat(100),"Voice","none").getString("text")!!
        assertTrue(unicode.codePointCount(0,unicode.length)<=32)
        assertFalse(unicode.last().isHighSurrogate())
    }
    @Test fun blankReferenceTextCannotCreateRequest() {
        assertThrows(IllegalArgumentException::class.java) { cloneInput("ref","  ","Voice","none") }
    }
    @Test fun preservesCompleteReferenceAndProfileInput() {
        val transcript="This is the exact recording, including every word."
        val request=cloneInput("ref-id",transcript,"Owner voice","en")
        assertEquals(transcript,request.getString("transcript"))
        assertEquals("ref-id",request.getString("reference"))
        assertEquals("Owner voice",request.getString("name"))
        assertEquals("en",request.getString("language"))
    }
}
