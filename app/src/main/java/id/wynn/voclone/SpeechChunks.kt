package id.wynn.voclone

import java.io.Reader
import java.io.PushbackReader

/** Bounded native requests; the complete input can be arbitrarily longer. Caller owns Reader. */
fun speechChunks(reader: Reader,checkpoint: ()->Unit={}): Sequence<String> = sequence {
    val input=PushbackReader(reader,1)
    var carry=""
    var eof=false
    while(!eof || carry.isNotEmpty()) {
        checkpoint()
        val buffer=StringBuilder(carry);var points=carry.codePointCount(0,carry.length)
        while(points<280 && !eof) {
            val value=input.read()
            if(value<0) { eof=true;break }
            val character=value.toChar();buffer.append(character)
            if(character.isHighSurrogate()) {
                val next=input.read()
                if(next>=0) { if(next.toChar().isLowSurrogate())buffer.append(next.toChar()) else input.unread(next) }
            }
            points++
        }
        val text=buffer.toString()
        if(text.isEmpty())break
        var boundary=text.length
        if(!eof) {
            val minimum=text.offsetByCodePoints(0,text.codePointCount(0,text.length)/2)
            val sentence=text.indices.lastOrNull { i -> i>=minimum && (text[i] in "。！？\n" || text[i] in ".!?" && i+1<text.length && text[i+1].isWhitespace()) }
            val space=text.indices.lastOrNull { it>=minimum && text[it].isWhitespace() }
            boundary=sentence?.plus(1) ?: space?.plus(1) ?: boundary
        }
        carry=text.substring(boundary)
        val part=text.substring(0,boundary).trim()
        if(part.isNotEmpty())yield(part)
    }
}
