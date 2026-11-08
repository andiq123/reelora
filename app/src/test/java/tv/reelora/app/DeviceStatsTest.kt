package tv.reelora.app

import android.net.NetworkCapabilities
import java.time.LocalDate
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceStatsTest {
    private val gib = 1024L * 1024 * 1024

    @Test fun readingsRoundSoSmallChangesDoNotRecompose() {
        assertEquals(roundToTenthGib(gib + 1), roundToTenthGib(gib + 2))
        assertEquals("1.0 GB", formatBytes(roundToTenthGib(gib + 1)))
    }

    @Test fun bytesAndSpeedsUseCompactUnits() {
        assertEquals("0.8 GB", formatBytes(850L * 1024 * 1024))
        assertEquals("5.4 GB", formatBytes(gib * 54 / 10))
        assertEquals("18 Mb/s", formatSpeed(18_400_000))
        assertEquals("2.5 Mb/s", formatSpeed(2_500_000))
        assertEquals("640 kb/s", formatSpeed(640_000))
    }

    @Test fun signalMapsToBars() {
        assertEquals(3, wifiBars(-50))
        assertEquals(2, wifiBars(-65))
        assertEquals(1, wifiBars(-75))
        assertEquals(0, wifiBars(-90))
        assertEquals(3, wifiBars(NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED))
    }

    @Test fun usedFractionIsBounded() {
        assertEquals(.25f, usedFraction(3, 4), .001f)
        assertEquals(0f, usedFraction(0, 0), .001f)
        assertEquals(0f, usedFraction(9, 4), .001f)
    }

    @Test fun resolutionNamesCommonModes() {
        assertEquals("4K", resolutionLabel(3840, 2160))
        assertEquals("1080p", resolutionLabel(1920, 1080))
        assertEquals("1024 × 768", resolutionLabel(1024, 768))
    }

    @Test fun displayNamesOnlyTheBestHdrFormat() {
        val dolbyVision = android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION
        val hdr10 = android.view.Display.HdrCapabilities.HDR_TYPE_HDR10
        assertEquals("Dolby Vision", bestHdrLabel(intArrayOf(hdr10, dolbyVision)))
        assertEquals("HDR10", bestHdrLabel(intArrayOf(hdr10)))
        assertEquals("", bestHdrLabel(IntArray(0)))
    }

    @Test fun cpuUsageComesFromIdleResidency() {
        assertEquals(25, cpuUsagePercent(idleMicros = 12_000_000, wallMicros = 4_000_000, cores = 4))
        assertEquals(0, cpuUsagePercent(idleMicros = 20_000_000, wallMicros = 4_000_000, cores = 4))
        assertNull(cpuUsagePercent(idleMicros = 0, wallMicros = 4_000_000, cores = 4))
        assertNull(cpuUsagePercent(idleMicros = 100, wallMicros = 0, cores = 4))
    }

    @Test fun textRunsSeparateNumbersFromWords() {
        assertEquals(listOf(TextRun("14:05", true)), textRuns("14:05"))
        assertEquals(listOf(TextRun("0.5", true), TextRun(" GB", false)), textRuns("0.5 GB"))
        assertEquals(listOf(TextRun("8", true), TextRun("° / ", false), TextRun("-2", true), TextRun("°", false)), textRuns("8° / -2°"))
        assertEquals(listOf(TextRun("Wi-Fi", false)), textRuns("Wi-Fi"))
        assertEquals(emptyList<TextRun>(), textRuns(""))
    }

    @Test fun numbersRollTowardTheirChange() {
        assertTrue(numberRose("19", "20"))
        assertEquals(false, numberRose("20", "19"))
        assertEquals(false, numberRose("1", "-1"))
    }

    @Test fun homeDateFollowsLanguage() {
        val date = LocalDate.of(2026, 11, 8)
        assertEquals("Sun 8 Nov", formatHomeDate(date, false))
        assertTrue(formatHomeDate(date, true).startsWith("Dum"))
    }

    @Test fun titleLogoPrefersEnglishPng() {
        val logos = JSONArray("""[{"iso_639_1":"fr","file_path":"/fr.png"},{"iso_639_1":"en","file_path":"/en.svg"},{"iso_639_1":"en","file_path":"/en.png"}]""")
        assertEquals("/en.png", pickTitleLogo(logos))
        assertEquals("/only.png", pickTitleLogo(JSONArray("""[{"iso_639_1":null,"file_path":"/only.png"}]""")))
        assertNull(pickTitleLogo(JSONArray("[]")))
        assertNull(pickTitleLogo(null))
    }

    @Test fun logoLuminanceIgnoresTransparentPixels() {
        val white = 0xFFFFFFFF.toInt()
        val clearBlack = 0x00000000
        assertEquals(1f, logoLuminance(listOf(white, clearBlack))!!, .001f)
        assertEquals(0f, logoLuminance(listOf(0xFF000000.toInt()))!!, .001f)
        assertNull(logoLuminance(listOf(clearBlack)))
    }
}
