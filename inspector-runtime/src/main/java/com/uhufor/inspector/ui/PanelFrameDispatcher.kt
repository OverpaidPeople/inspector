package com.uhufor.inspector.ui

/** Collapse a burst of MOVE events into one window update per display frame. */
internal class PanelFrameDispatcher(
    private val postFrame: (Runnable) -> Unit,
    private val removeFrame: (Runnable) -> Unit,
    private val applyFrame: (PanelFrame) -> Unit,
) {
    private var pending: PanelFrame? = null
    private var posted = false
    private val callback = Runnable { flush() }

    fun request(frame: PanelFrame) {
        pending = frame
        if (!posted) {
            posted = true
            postFrame(callback)
        }
    }

    fun flush() {
        if (posted) removeFrame(callback)
        posted = false
        val frame = pending
        pending = null
        if (frame != null) applyFrame(frame)
    }

    fun cancel() {
        if (posted) removeFrame(callback)
        posted = false
        pending = null
    }
}
