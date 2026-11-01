package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class SpeechChunksTest {
    @Test fun longTextHasNoOverallLimitButEachNativeRequestStaysBounded() {
        val original=("Kalimat ini dipertahankan. これは日本語の文章です。 😀 ").repeat(200)
        val parts=StringReader(original).use { speechChunks(it).toList() }
        assertTrue(original.length>600 && parts.size>10)
        assertTrue(parts.all { it.isNotBlank() && it.codePointCount(0,it.length)<=280 && it.length<=600 })
        assertEquals(original.filterNot(Char::isWhitespace),parts.joinToString("").filterNot(Char::isWhitespace))
        assertTrue(parts.none { it.last().isHighSurrogate() || it.first().isLowSurrogate() })
    }
    @Test fun smallTextAndWhitespaceAreHandledWithoutEmptySynthesis() {
        assertEquals(listOf("Hello, world."),StringReader("  Hello, world.\n").use { speechChunks(it).toList() })
        assertTrue(StringReader(" \n\t").use { speechChunks(it).toList() }.isEmpty())
    }
    @Test fun cancellationIsCheckedEvenBeforeWhitespaceProducesAnyChunk() {
        var checks=0
        val input=StringReader(" ".repeat(100000))
        assertThrows(java.util.concurrent.CancellationException::class.java) {
            input.use { speechChunks(it) { if(++checks==3)throw java.util.concurrent.CancellationException() }.toList() }
        }
        assertEquals(3,checks)
    }
    @Test fun aLongWordAndSurrogatePairsCannotCauseInfiniteSplitting() {
        val original="x".repeat(2000)+"😀".repeat(2000)
        val parts=StringReader(original).use { speechChunks(it).toList() }
        assertEquals(original,parts.joinToString(""))
        assertTrue(parts.all { it.codePointCount(0,it.length)<=280 })
    }
}
