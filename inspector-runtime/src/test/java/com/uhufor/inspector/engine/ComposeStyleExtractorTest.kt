package com.uhufor.inspector.engine

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ComposeStyleExtractorTest {
    private var previousInspectorFlag = false

    @Before fun enableInspectorInfo() {
        previousInspectorFlag = isDebugInspectorInfoEnabled
        isDebugInspectorInfoEnabled = true
    }

    @After fun restoreInspectorInfo() {
        isDebugInspectorInfoEnabled = previousInspectorFlag
    }

    private val base = TextStyle(
        color = Color.Red, fontSize = 18.sp,
        fontWeight = FontWeight.Medium, fontStyle = FontStyle.Normal
    )

    @Test fun plainTextPreservesContentAndSpWithoutPixelConversion() {
        val result = ComposeStyleExtractor.textStyle(AnnotatedString("안녕\nCompose"), base)
        assertEquals("안녕\nCompose", result.text)
        assertEquals(Color.Red.toArgb(), result.textColor)
        assertEquals(18f, result.textSizeSp)
        assertNull(result.textSize)
        assertEquals(500, result.fontWeight)
        assertEquals(false, result.isItalic)
    }

    @Test fun mixedSpansInvalidateOnlyPropertiesThatDiffer() {
        val text = AnnotatedString("abcd", listOf(
            AnnotatedString.Range(SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold), 1, 3)
        ))
        val result = ComposeStyleExtractor.textStyle(text, base)
        assertNull(result.textColor)
        assertNull(result.fontWeight)
        assertNull(result.isBold)
        assertEquals(18f, result.textSizeSp)
        assertEquals(false, result.isItalic)
    }

    @Test fun overlappingFullCoverageSpansResolveEffectiveStyle() {
        val text = AnnotatedString("abcd", listOf(
            AnnotatedString.Range(SpanStyle(color = Color.Blue), 0, 4),
            AnnotatedString.Range(SpanStyle(fontStyle = FontStyle.Italic), 0, 4)
        ))
        val result = ComposeStyleExtractor.textStyle(text, base)
        assertEquals(Color.Blue.toArgb(), result.textColor)
        assertEquals(true, result.isItalic)
    }

    @Test fun unspecifiedAndEmValuesRemainUnknown() {
        val result = ComposeStyleExtractor.textStyle(AnnotatedString("a"), TextStyle(fontSize = 2.em))
        assertNull(result.textColor)
        assertNull(result.textSizeSp)
        assertNull(result.fontWeight)
        assertNull(result.isItalic)
    }

    @Test fun shaderTextIsNotReportedAsSolidColor() {
        val result = ComposeStyleExtractor.textStyle(
            AnnotatedString("a"), TextStyle(brush = Brush.horizontalGradient(listOf(Color.Red, Color.Blue)))
        )
        assertNull(result.textColor)
    }

    @Test fun zeroLengthSpanDoesNotContaminateVisibleText() {
        val text = AnnotatedString("abcd", listOf(
            AnnotatedString.Range(SpanStyle(color = Color.Blue), 2, 2)
        ))
        assertEquals(Color.Red.toArgb(), ComposeStyleExtractor.textStyle(text, base).textColor)
    }

    private fun background(modifier: Modifier) = ComposeStyleExtractor.background(
        modifier.foldIn(emptyList()) { elements, element -> elements + element }
    ).backgroundColor

    @Test fun solidBackgroundPreservesAlpha() {
        val color = Color(0x80112233)
        assertEquals(color.toArgb(), background(Modifier.background(color)))
    }

    @Test fun solidBrushIncludesExplicitAlpha() {
        assertEquals(Color.Red.copy(alpha = 0.5f).toArgb(), background(
            Modifier.background(SolidColor(Color.Red), alpha = 0.5f)
        ))
    }

    @Test fun gradientAndLayeredBackgroundsRemainUnknown() {
        assertNull(background(Modifier.background(Brush.horizontalGradient(listOf(Color.Red, Color.Blue)))))
        assertNull(background(Modifier.background(Color.Red).background(Color.Blue)))
        assertNull(background(Modifier))
    }

    @Test fun backgroundWithCustomDrawingRemainsUnknown() {
        assertNull(background(Modifier.background(Color.Red).drawBehind { drawRect(Color.Blue) }))
    }

    @Test fun unavailableInspectorInfoRemainsUnknown() {
        isDebugInspectorInfoEnabled = false
        val modifier = Modifier.background(Color.Red)
        isDebugInspectorInfoEnabled = true
        assertNull(background(modifier))
        assertNull(background(modifier.background(Color.Blue)))
    }

    @Test fun typographyPreservesSourceUnitsFamilyAlignmentAndPadding() {
        val style = ComposeStyleExtractor.textStyle(AnnotatedString("one\ntwo"), base.copy(
            fontFamily = FontFamily.Monospace, lineHeight = 1.5.em,
            letterSpacing = 0.5.sp, textAlign = TextAlign.Center,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ))
        assertEquals("monospace", style.fontFamily)
        assertEquals(StyleTextUnit(1.5f, "em"), style.lineHeight)
        assertEquals(StyleTextUnit(0.5f, "sp"), style.letterSpacing)
        assertEquals("Center", style.textAlign)
        assertEquals(false, style.includeFontPadding)
    }

    @Test fun mixedParagraphHeightsAndAlignmentRemainUnknown() {
        val text = AnnotatedString("one\ntwo", paragraphStyles = listOf(
            AnnotatedString.Range(ParagraphStyle(lineHeight = 30.sp, textAlign = TextAlign.End), 4, 7)
        ))
        val style = ComposeStyleExtractor.textStyle(text, base.copy(lineHeight = 24.sp, textAlign = TextAlign.Start))
        assertNull(style.lineHeight)
        assertNull(style.textAlign)
        assertEquals(18f, style.textSizeSp)
    }

    @Test fun mixedFontsAndSpacingRemainUnknown() {
        val text = AnnotatedString("abcd", listOf(
            AnnotatedString.Range(SpanStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.em), 1, 3)
        ))
        val style = ComposeStyleExtractor.textStyle(text, base.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.sp))
        assertNull(style.fontFamily)
        assertNull(style.letterSpacing)
    }

    @Test fun unspecifiedTypographyIsNotGuessed() {
        val style = ComposeStyleExtractor.textStyle(AnnotatedString("a"), TextStyle())
        assertNull(style.fontFamily)
        assertNull(style.letterSpacing)
        assertNull(style.lineHeight)
        assertNull(style.textAlign)
        assertNull(style.includeFontPadding)
    }
}
