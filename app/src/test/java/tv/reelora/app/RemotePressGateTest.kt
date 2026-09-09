package tv.reelora.app

import org.junit.Assert.*
import org.junit.Test

class RemotePressGateTest {
    @Test fun onlyFreshCompletePressCanActivateNewSurface() {
        val gate = RemotePressGate()
        assertTrue(gate.consume(23, true, 20, 1))
        assertTrue(gate.consume(23, false, 0, 1))
        assertTrue(gate.consume(23, true, 0, 2, ready = false))
        assertTrue(gate.consume(23, false, 0, 2))
        assertFalse(gate.consume(23, true, 0, 3))
        assertTrue(gate.consume(23, true, 40, 3))
        assertFalse(gate.consume(23, false, 0, 3))
        assertTrue(gate.consume(23, false, 0, 3))
        assertFalse(gate.consume(23, true, 0, 4))
        assertTrue(gate.consume(66, false, 0, 4))
    }
}
