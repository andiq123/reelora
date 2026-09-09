package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DockNavigationTest {
    @Test fun returnTargetTracksIdentityAcrossDockChanges() {
        assertEquals(2, dockEntryIndex(listOf("a", "b", "c"), "c"))
        assertEquals(0, dockEntryIndex(listOf("c", "a", "b"), "c"))
        assertEquals(0, dockEntryIndex(listOf("a", "b"), "c"))
        assertEquals(3, dockEntryIndex(listOf("a", "b", "c"), "search"))
        assertEquals(4, dockEntryIndex(listOf("a", "b", "c"), "hidden"))
        assertEquals(5, dockEntryIndex(listOf("a", "b", "c"), "settings"))
        assertEquals(2, dockEntryIndex(emptyList(), "settings"))
        assertEquals(0, dockEntryIndex(emptyList(), null))
    }
}
