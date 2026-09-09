package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceBudgetTest {
    @Test fun artworkBudgetRespectsBothHeapAndLowRamLimits() {
        val mib = 1024L * 1024
        assertEquals(8 * mib, artworkMemoryBudget(512 * mib, true))
        assertEquals(24 * mib, artworkMemoryBudget(512 * mib, false))
        assertEquals(4 * mib, artworkMemoryBudget(48 * mib, true))
        assertEquals(4 * mib, artworkMemoryBudget(48 * mib, false))
    }
}
