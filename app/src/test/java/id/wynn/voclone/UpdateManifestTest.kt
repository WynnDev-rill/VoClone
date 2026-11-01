package id.wynn.voclone


import org.junit.Assert.*
import org.junit.Test

class UpdateManifestTest {
    private val hash = "a".repeat(64)
    private val valid = UpdateManifest(3, "1.2.0", "id.wynn.voclone", 30,
        "https://github.com/WynnDev-rill/VoClone/releases/download/v1.2.0/VoClone-1.2.0-arm64.apk", hash, hash, 3000000)
    private fun rejected(value: UpdateManifest) { assertThrows(IllegalArgumentException::class.java) { value.validate() } }
    @Test fun validReleaseAccepted() { valid.validate() }
    @Test fun onlyNewerVersionOffered() {
        assertTrue(valid.newerFor(valid.packageName, 2, 35, setOf(hash)))
        assertFalse(valid.newerFor(valid.packageName, 3, 35, setOf(hash)))
        assertFalse(valid.newerFor(valid.packageName, 4, 35, setOf(hash)))
    }
    @Test fun wrongSigningKeyRejected() { assertThrows(IllegalArgumentException::class.java) { valid.newerFor(valid.packageName, 2, 35, setOf("b".repeat(64))) } }
    @Test fun packageMismatchRejected() { assertThrows(IllegalArgumentException::class.java) { valid.newerFor("id.wynn.voclone.preview", 2, 35, setOf(hash)) } }
    @Test fun unsupportedAndroidRejected() { assertThrows(IllegalArgumentException::class.java) { valid.copy(minSdk = 36).newerFor(valid.packageName, 2, 35, setOf(hash)) } }
    @Test fun arbitraryHostRejected() { rejected(valid.copy(apkUrl = "https://example.com/malicious.apk")) }
    @Test fun oldStoreAndRawBootstrapAreNotTrusted() {
        rejected(valid.copy(apkUrl="https://github.com/WynnDev-rill/Wynn-Store/releases/download/voclone-v1.2.0/VoClone.apk"))
        rejected(valid.copy(apkUrl="https://raw.githubusercontent.com/WynnDev-rill/VoClone/main/distribution/VoClone.apk"))
    }
    @Test fun untrustedRawPathRejected() { rejected(valid.copy(apkUrl = "https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/Other.apk")) }
    @Test fun cleartextRejected() { rejected(valid.copy(apkUrl = valid.apkUrl.replace("https:", "http:"))) }
    @Test fun unrelatedRepositoryRejected() { rejected(valid.copy(apkUrl = valid.apkUrl.replace("/VoClone/", "/Other/"))) }
    @Test fun credentialsAndTraversalRejected() {
        rejected(valid.copy(apkUrl = valid.apkUrl.replace("github.com", "attacker@github.com")))
        rejected(valid.copy(apkUrl = valid.apkUrl.replace("VoClone-1.2.0-arm64.apk", "../malicious.apk")))
        rejected(valid.copy(apkUrl = valid.apkUrl.replace("VoClone-1.2.0-arm64.apk", "%2e%2e/malicious.apk")))
    }
    @Test fun oversizedPayloadRejected() { rejected(valid.copy(sizeBytes = UpdateManifest.MAX_APK_SIZE + 1)) }
    @Test fun invalidChecksumsRejected() { rejected(valid.copy(sha256 = "xyz")); rejected(valid.copy(signerSha256 = "")) }
    @Test fun invalidVersionsRejected() { rejected(valid.copy(versionCode = 0)); rejected(valid.copy(versionName = "")) }
}
