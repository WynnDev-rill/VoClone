package id.wynn.voclone

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

class ModelStoreTest {
    @Test fun rejectsCorruptDownloadAndNeverCommits() = runBlocking {
        val server = MockWebServer(); server.start()
        val dir = java.nio.file.Files.createTempDirectory("model").toFile()
        try {
            val bytes = "correct".toByteArray()
            val asset = ModelAsset("test.bin", 7, sha256(bytes), server.url("/test").toString())
            server.enqueue(MockResponse().setBody("corrupt"))
            try { VerifiedDownload(OkHttpClient()).fetch(asset, dir) { _, _ -> }; fail("corrupt asset accepted") } catch (_: IllegalArgumentException) { }
            assertFalse(File(dir, asset.name).exists())
        } finally { server.shutdown(); dir.deleteRecursively() }
    }
    @Test fun resumesOnlyAtMatchingRangeAndVerifiesWholeFile() = runBlocking {
        val server = MockWebServer(); server.start()
        val dir = java.nio.file.Files.createTempDirectory("resume").toFile()
        try {
            val asset = ModelAsset("test.bin", 7, sha256("correct".toByteArray()), server.url("/test").toString())
            File(dir, "test.bin.part").writeText("cor")
            server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range", "bytes 3-6/7").setBody("rect"))
            VerifiedDownload(OkHttpClient()).fetch(asset, dir) { _, _ -> }
            assertEquals("bytes=3-", server.takeRequest().getHeader("Range"))
            assertEquals("correct", File(dir,"test.bin").readText())
        } finally { server.shutdown(); dir.deleteRecursively() }
    }
    @Test fun restartingServerResponseReplacesPartialFile() = runBlocking {
        val server = MockWebServer(); server.start()
        val dir = java.nio.file.Files.createTempDirectory("restart").toFile()
        try {
            val asset = ModelAsset("test.bin", 7, sha256("correct".toByteArray()), server.url("/test").toString())
            File(dir,"test.bin.part").writeText("bad")
            server.enqueue(MockResponse().setBody("correct"))
            VerifiedDownload(OkHttpClient()).fetch(asset, dir) { _, _ -> }
            assertEquals("correct", File(dir,"test.bin").readText())
        } finally { server.shutdown(); dir.deleteRecursively() }
    }
    @Test fun cancelKeepsOnlyPartialAndCanResume() = runBlocking {
        val server=MockWebServer();server.start()
        val dir=java.nio.file.Files.createTempDirectory("cancel").toFile()
        try {
            val bytes=ByteArray(256*1024) { (it%127).toByte() }
            val asset=ModelAsset("test.bin",bytes.size.toLong(),sha256(bytes),server.url("/test").toString())
            server.enqueue(MockResponse().setBody(okio.Buffer().write(bytes)))
            try {
                VerifiedDownload(OkHttpClient()).fetch(asset,dir) { done,_ -> if(done>0)throw kotlinx.coroutines.CancellationException("user cancel") }
                fail("cancel ignored")
            } catch(_: kotlinx.coroutines.CancellationException) { }
            assertFalse(File(dir,"test.bin").exists())
            val count=File(dir,"test.bin.part").length()
            assertTrue(count in 1 until bytes.size.toLong())
            server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range","bytes $count-${bytes.size-1}/${bytes.size}").setBody(okio.Buffer().write(bytes.copyOfRange(count.toInt(),bytes.size))))
            VerifiedDownload(OkHttpClient()).fetch(asset,dir) { _,_ -> }
            assertArrayEquals(bytes,File(dir,"test.bin").readBytes())
        } finally { server.shutdown();dir.deleteRecursively() }
    }
    @Test fun rejectsWrongResumeRange() = runBlocking {
        val server=MockWebServer();server.start()
        val dir=java.nio.file.Files.createTempDirectory("wrong-range").toFile()
        try {
            val asset=ModelAsset("test.bin",7,sha256("correct".toByteArray()),server.url("/test").toString())
            File(dir,"test.bin.part").writeText("cor")
            server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range","bytes 0-3/7").setBody("rect"))
            try { VerifiedDownload(OkHttpClient()).fetch(asset,dir) { _,_ -> };fail("wrong range accepted") } catch(_: IllegalArgumentException) { }
            assertFalse(File(dir,"test.bin").exists())
        } finally { server.shutdown();dir.deleteRecursively() }
    }
    @Test fun rejectsTraversal() {
        assertThrows(IllegalArgumentException::class.java) { ModelAsset("../model", 1,"0".repeat(64),"https://huggingface.co/a") }
    }
}
