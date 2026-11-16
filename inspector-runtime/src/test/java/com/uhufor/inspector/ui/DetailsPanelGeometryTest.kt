package com.uhufor.inspector.ui

import org.junit.Assert.*
import org.junit.Test

class DetailsPanelGeometryTest {
    private fun geometry(width: Int = 1000, height: Int = 1600, density: Float = 1f, scale: Float = 1f) =
        DetailsPanelGeometry(width, height, density, scale, 8, 8)

    @Test fun defaultsKeepExistingSizeAndUiScale() {
        assertEquals(156 to 188, geometry().initialSize(null))
        assertEquals(304 to 368, geometry(density = 2f).initialSize(null))
        assertEquals(230 to 278, geometry(scale = 1.5f).initialSize(null))
    }

    @Test fun preferredContentSizeIsIndependentOfDisplayDensityAndFontScale() {
        val preferred = DetailsPanelSize(260f, 340f)
        assertEquals(528 to 688, geometry(density = 2f).initialSize(preferred))
        assertEquals(788 to 1028, geometry(density = 3f, scale = 1.2f).initialSize(preferred))
        assertEquals(preferred, geometry(density = 2f).storedSize(PanelFrame(20, 40, 528, 688)))
    }

    @Test fun allFourCornersKeepTheOppositeCornerFixed() {
        val start = PanelFrame(200, 300, 300, 400)
        val g = geometry()
        assertEquals(PanelFrame(220, 330, 280, 370), g.resize(start, ResizeCorner.TOP_LEFT, 20f, 30f))
        assertEquals(PanelFrame(200, 330, 320, 370), g.resize(start, ResizeCorner.TOP_RIGHT, 20f, 30f))
        assertEquals(PanelFrame(220, 300, 280, 430), g.resize(start, ResizeCorner.BOTTOM_LEFT, 20f, 30f))
        assertEquals(PanelFrame(200, 300, 320, 430), g.resize(start, ResizeCorner.BOTTOM_RIGHT, 20f, 30f))
    }

    @Test fun resizeClampsToScreenAndMinimumReadableSize() {
        val start = PanelFrame(200, 300, 300, 400)
        val g = geometry()
        assertEquals(PanelFrame(0, 0, 500, 700), g.resize(start, ResizeCorner.TOP_LEFT, -10000f, -10000f))
        assertEquals(PanelFrame(200, 300, 800, 1300), g.resize(start, ResizeCorner.BOTTOM_RIGHT, 10000f, 10000f))
        assertEquals(PanelFrame(372, 572, 128, 128), g.resize(start, ResizeCorner.TOP_LEFT, 10000f, 10000f))
        assertEquals(PanelFrame(200, 300, 128, 128), g.resize(start, ResizeCorner.BOTTOM_RIGHT, -10000f, -10000f))
    }

    @Test fun rotationClampsVisibleSizeWithoutOverwritingPreferredSize() {
        val preferred = DetailsPanelSize(500f, 600f)
        assertEquals(320 to 240, geometry(320, 240, density = 2f).initialSize(preferred))
        assertEquals(1008 to 1208, geometry(1200, 2000, density = 2f).initialSize(preferred))
        assertEquals(PanelFrame(0, 0, 100, 80), geometry(100, 80).fit(PanelFrame(500, 500, 400, 400)))
    }

    @Test fun invalidSavedValuesAndTouchCoordinatesAreIgnored() {
        val g = geometry()
        assertEquals(g.initialSize(null), g.initialSize(DetailsPanelSize(Float.NaN, 500f)))
        assertEquals(g.initialSize(null), g.initialSize(DetailsPanelSize(-1f, 500f)))
        val start = PanelFrame(200, 300, 300, 400)
        assertEquals(start, g.resize(start, ResizeCorner.BOTTOM_RIGHT, Float.NaN, 1f))
        assertEquals(1000 to 1600, g.initialSize(DetailsPanelSize(Float.MAX_VALUE, Float.MAX_VALUE)))
    }
}
