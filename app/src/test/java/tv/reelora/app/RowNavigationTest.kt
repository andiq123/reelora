package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RowNavigationTest {
    @Test fun verticalMovementUsesScreenPositionInsteadOfCatalogIndex() {
        val firstRow = (0..2).map { VisibleMovie(it, it * 100, 100) }
        val scrolledRow = (5..7).map { VisibleMovie(it, (it - 5) * 100, 100) }
        assertEquals(2, nearestVisibleMovie(250, 0, 300, firstRow))
        assertEquals(7, nearestVisibleMovie(250, 0, 300, scrolledRow))
        assertEquals(5, nearestVisibleMovie(50, 0, 300, scrolledRow))
    }

    @Test fun clippedCardsDoNotPullDestinationSideways() {
        val row = listOf(VisibleMovie(4, -70, 100), VisibleMovie(5, 40, 100),
            VisibleMovie(6, 150, 100), VisibleMovie(7, 260, 100))
        assertEquals(6, nearestVisibleMovie(290, 0, 300, row))
        assertEquals(5, nearestVisibleMovie(0, 0, 300, row))
    }

    @Test fun shortOrNarrowRowsStillHaveAReachableTarget() {
        assertEquals(0, nearestVisibleMovie(500, 0, 300, listOf(VisibleMovie(0, 40, 100))))
        assertEquals(3, nearestVisibleMovie(50, 0, 80, listOf(VisibleMovie(3, -10, 100))))
        assertNull(nearestVisibleMovie(0, 0, 300, emptyList()))
    }
}
