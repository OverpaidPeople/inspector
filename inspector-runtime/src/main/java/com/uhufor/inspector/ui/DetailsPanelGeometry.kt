package com.uhufor.inspector.ui

import kotlin.math.roundToInt

internal data class DetailsPanelSize(val widthDp: Float, val heightDp: Float) {
    fun isValid() = widthDp.isFinite() && heightDp.isFinite() && widthDp > 0 && heightDp > 0
}

internal enum class ResizeCorner(val left: Boolean, val top: Boolean) {
    TOP_LEFT(true, true), TOP_RIGHT(false, true), BOTTOM_LEFT(true, false), BOTTOM_RIGHT(false, false);

    val viewTag: String get() = "inspector_resize_${name.lowercase()}"
}

internal data class PanelFrame(val x: Int, val y: Int, val width: Int, val height: Int) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height
}

/** All geometry is in window pixels; stored sizes describe only the card, in dp. */
internal class DetailsPanelGeometry(
    val screenWidth: Int,
    val screenHeight: Int,
    val density: Float,
    val uiScale: Float,
    val horizontalChrome: Int,
    val verticalChrome: Int,
) {
    val minWidth: Int get() = pixels(MIN_WIDTH_DP * uiScale, horizontalChrome, screenWidth)
    val minHeight: Int get() = pixels(MIN_HEIGHT_DP * uiScale, verticalChrome, screenHeight)

    private fun pixels(dp: Float, chrome: Int, maximum: Int): Int =
        (dp * density + chrome).coerceIn(1f, maximum.coerceAtLeast(1).toFloat()).roundToInt()

    fun initialSize(preferred: DetailsPanelSize?): Pair<Int, Int> {
        val size = preferred?.takeIf { it.isValid() }
            ?: DetailsPanelSize(DEFAULT_WIDTH_DP * uiScale, DEFAULT_HEIGHT_DP * uiScale)
        return pixels(size.widthDp, horizontalChrome, screenWidth).coerceAtLeast(minWidth) to
            pixels(size.heightDp, verticalChrome, screenHeight).coerceAtLeast(minHeight)
    }

    fun fit(frame: PanelFrame): PanelFrame {
        val width = frame.width.coerceIn(minWidth, screenWidth.coerceAtLeast(1))
        val height = frame.height.coerceIn(minHeight, screenHeight.coerceAtLeast(1))
        return PanelFrame(frame.x.coerceIn(0, (screenWidth - width).coerceAtLeast(0)),
            frame.y.coerceIn(0, (screenHeight - height).coerceAtLeast(0)), width, height)
    }

    /** The opposite corner stays fixed, including when the moving edge reaches a screen edge. */
    fun resize(start: PanelFrame, corner: ResizeCorner, dx: Float, dy: Float): PanelFrame {
        val frame = fit(start)
        if (!dx.isFinite() || !dy.isFinite()) return frame
        val left = if (corner.left) (frame.x.toFloat() + dx)
            .coerceIn(0f, (frame.right - minWidth).coerceAtLeast(0).toFloat()).roundToInt() else frame.x
        val right = if (!corner.left) (frame.right.toFloat() + dx)
            .coerceIn((frame.x + minWidth).toFloat(), screenWidth.coerceAtLeast(1).toFloat()).roundToInt() else frame.right
        val top = if (corner.top) (frame.y.toFloat() + dy)
            .coerceIn(0f, (frame.bottom - minHeight).coerceAtLeast(0).toFloat()).roundToInt() else frame.y
        val bottom = if (!corner.top) (frame.bottom.toFloat() + dy)
            .coerceIn((frame.y + minHeight).toFloat(), screenHeight.coerceAtLeast(1).toFloat()).roundToInt() else frame.bottom
        return PanelFrame(left, top, right - left, bottom - top)
    }

    fun storedSize(frame: PanelFrame) = DetailsPanelSize(
        (frame.width - horizontalChrome).coerceAtLeast(1) / density,
        (frame.height - verticalChrome).coerceAtLeast(1) / density,
    )

    companion object {
        const val DEFAULT_WIDTH_DP = 148f
        const val DEFAULT_HEIGHT_DP = 180f
        const val MIN_WIDTH_DP = 120f
        const val MIN_HEIGHT_DP = 120f
    }
}
