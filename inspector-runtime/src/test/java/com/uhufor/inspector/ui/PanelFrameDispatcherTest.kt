package com.uhufor.inspector.ui

import org.junit.Assert.*
import org.junit.Test

class PanelFrameDispatcherTest {
    private val first = PanelFrame(10, 20, 200, 300)
    private val last = PanelFrame(10, 20, 250, 380)

    @Test fun moveBurstPostsOnlyOneFrameAndAppliesLatestBounds() {
        val posted = mutableListOf<Runnable>()
        val applied = mutableListOf<PanelFrame>()
        val dispatcher = PanelFrameDispatcher({ posted += it }, {}, { applied += it })
        repeat(100) { dispatcher.request(first.copy(width = 200 + it)) }
        dispatcher.request(last)
        assertEquals(1, posted.size)
        assertTrue(applied.isEmpty())
        posted.single().run()
        assertEquals(listOf(last), applied)
    }

    @Test fun releaseFlushesFinalTouchWithoutApplyingQueuedCallbackTwice() {
        var posted: Runnable? = null
        var removed = 0
        val applied = mutableListOf<PanelFrame>()
        val dispatcher = PanelFrameDispatcher({ posted = it }, { removed++ }, { applied += it })
        dispatcher.request(first)
        dispatcher.request(last)
        dispatcher.flush()
        posted!!.run()
        assertEquals(1, removed)
        assertEquals(listOf(last), applied)
    }

    @Test fun cancelledOrRemovedWindowDoesNotReceiveQueuedUpdates() {
        var posted: Runnable? = null
        val applied = mutableListOf<PanelFrame>()
        val dispatcher = PanelFrameDispatcher({ posted = it }, {}, { applied += it })
        dispatcher.request(first)
        dispatcher.cancel()
        posted!!.run()
        assertTrue(applied.isEmpty())
    }

    @Test fun nextDisplayFrameCanScheduleAnotherUpdate() {
        val posted = mutableListOf<Runnable>()
        val applied = mutableListOf<PanelFrame>()
        val dispatcher = PanelFrameDispatcher({ posted += it }, {}, { applied += it })
        dispatcher.request(first)
        posted.first().run()
        dispatcher.request(last)
        assertEquals(2, posted.size)
        posted.last().run()
        assertEquals(listOf(first, last), applied)
    }
}
