package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceBudgetTest {
    @Test fun retainedBackdropRejectsLargeOrInvalidBuffers() {
        assertTrue(retainBackdrop(1920, 1080))
        assertTrue(retainBackdrop(1280, 720))
        assertFalse(retainBackdrop(3840, 2160))
        assertFalse(retainBackdrop(0, 1080))
        assertFalse(retainBackdrop(1920, -1))
        assertFalse(retainBackdrop(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun artworkBudgetRespectsBothHeapAndLowRamLimits() {
        val mib = 1024L * 1024
        assertEquals(8 * mib, artworkMemoryBudget(512 * mib, true))
        assertEquals(24 * mib, artworkMemoryBudget(512 * mib, false))
        assertEquals(4 * mib, artworkMemoryBudget(48 * mib, true))
        assertEquals(4 * mib, artworkMemoryBudget(48 * mib, false))
    }
}
