package com.uhufor.inspector.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DesignInspectionTest {
    private var previous = false
    @Before fun enableMetadata() { previous = isDebugInspectorInfoEnabled; isDebugInspectorInfoEnabled = true }
    @After fun restoreMetadata() { isDebugInspectorInfoEnabled = previous }

    private fun appearance(modifier: Modifier, direction: LayoutDirection = LayoutDirection.Ltr) =
        ComposeAppearanceExtractor.extract(modifier.foldIn(emptyList()) { entries, element ->
            entries + ComposeAppearanceExtractor.snapshot(element, 200f, 100f)
        }, Density(2f), direction)

    @Test fun roundedBackgroundBorderAndLayerAlphaAreReadIndependently() {
        val shape = RoundedCornerShape(8.dp)
        val value = appearance(Modifier.background(Color.Red, shape)
            .border(2.dp, Color.Blue, shape).graphicsLayer(alpha = 0.6f))
        assertEquals(listOf(16f, 16f, 16f, 16f), value.cornerRadiiPx)
        assertEquals(2f, value.borderWidthDp)
        assertEquals(Color.Blue.toArgb(), value.borderColor)
        assertEquals(0.6f, value.layerAlpha)
    }

    @Test fun gradientAndMultipleBordersDoNotExposeOneColorOrWidth() {
        val gradient = appearance(Modifier.border(2.dp, Brush.horizontalGradient(listOf(Color.Red, Color.Blue)), RoundedCornerShape(4.dp)))
        assertEquals(2f, gradient.borderWidthDp)
        assertNull(gradient.borderColor)
        val multiple = appearance(Modifier.border(1.dp, Color.Red).border(2.dp, Color.Blue))
        assertNull(multiple.borderWidthDp)
        assertNull(multiple.borderColor)
    }

    @Test fun layerBlocksAreNeverExecutedAndOpacityStaysUnknown() {
        var invoked = false
        val style = appearance(Modifier.graphicsLayer { invoked = true; alpha = 0.2f })
        assertFalse(invoked)
        assertNull(style.layerAlpha)
        assertNull(style.cornerRadiiPx)
    }

    @Test fun inconsistentShapesRemainUnknown() {
        assertNull(appearance(Modifier.background(Color.Red, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Blue, RoundedCornerShape(12.dp))).cornerRadiiPx)
        assertNull(appearance(Modifier).layerAlpha)
        assertNull(appearance(Modifier).cornerRadiiPx)
    }

    @Test fun localLayerAlphasMultiplyWithoutBorrowingAncestorOpacity() {
        assertEquals(0.25f, appearance(Modifier.graphicsLayer(alpha = 0.5f).graphicsLayer(alpha = 0.5f)).layerAlpha)
    }

    @Test fun geometryUsesMappedTextOriginAndOnlyAvailableBaselines() {
        val geometry = createTextLayoutInfo(100f, 60f, 3, 15f, 55f) { x, y -> 20f + x to 40f + y }!!
        assertEquals(GuideLine(20f, 40f, 120f, 40f), geometry.outline.first())
        assertEquals(listOf(GuideLine(20f, 55f, 120f, 55f), GuideLine(20f, 95f, 120f, 95f)), geometry.baselines)
        assertEquals(3, geometry.lineCount)
    }

    @Test fun geometryRejectsInvalidCoordinatesAndDeduplicatesSingleLine() {
        assertNull(createTextLayoutInfo(10f, 20f, 1, 15f, 15f) { _, _ -> Float.NaN to 0f })
        val layout = createTextLayoutInfo(10f, 20f, 1, 15f, 15f) { x, y -> x to y }!!
        assertEquals(1, layout.baselines.size)
    }
}
