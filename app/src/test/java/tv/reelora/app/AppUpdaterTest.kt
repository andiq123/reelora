package tv.reelora.app

import org.junit.Assert.*
import org.junit.Test

class AppUpdaterTest {
    @Test fun versionsCompareNumericallyAndRejectUnstableTags() {
        assertTrue(newerVersion("0.20.0", "0.19.4"))
        assertTrue(newerVersion("0.19.10", "0.19.9"))
        assertTrue(newerVersion("v1.0.0", "0.99.99"))
        assertFalse(newerVersion("0.20.0", "0.20.0"))
        assertFalse(newerVersion("0.19.4", "0.20.0"))
        assertFalse(newerVersion("latest", "0.20.0"))
        assertFalse(newerVersion("0.21.0-beta", "0.20.0"))
        assertNull(versionParts("999999999999.1.0"))
    }

    @Test fun updateAssetMustBeTheOfficialApkWithASha256Digest() {
        val url = "https://github.com/andiq123/reelora/releases/download/latest/ReeloraTV-latest.apk"
        val digest = "sha256:" + "a".repeat(64)
        val release = validateReleaseAsset("0.20.0", url, digest, 2_800_000)
        assertEquals("a".repeat(64), release.sha256)
        assertTrue(runCatching { validateReleaseAsset("0.20.0", url.replace("github.com", "example.com"), digest, 100) }.isFailure)
        assertTrue(runCatching { validateReleaseAsset("0.20.0", url, "", 100) }.isFailure)
        assertTrue(runCatching { validateReleaseAsset("0.20.0", url, digest, 0) }.isFailure)
        assertTrue(runCatching { validateReleaseAsset("0.20.0", url, digest, 100L * 1024 * 1024) }.isFailure)
    }
}
