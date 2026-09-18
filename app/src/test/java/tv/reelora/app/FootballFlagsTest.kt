package tv.reelora.app

import org.junit.Assert.*
import org.junit.Test

class FootballFlagsTest {
    @Test fun flagsUseCountryMetadataAndIgnoreUnknownValues() {
        assertEquals("🇪🇸", footballCountryFlag("Spain"))
        assertEquals("🇩🇪", footballCountryFlag("Germany"))
        assertEquals("🇺🇸", footballCountryFlag("USA"))
        assertEquals("🇰🇷", footballCountryFlag("South Korea"))
        assertEquals("🇷🇴", footballCountryFlag("RO"))
        assertNotEquals(footballCountryFlag("England"), footballCountryFlag("Scotland"))
        assertNull(footballCountryFlag(""))
        assertNull(footballCountryFlag("Europe"))
        assertNull(footballCountryFlag("Chelsea"))
    }
}
