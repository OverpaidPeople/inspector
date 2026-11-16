package com.uhufor.inspector.engine

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.RectF
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.uhufor.inspector.UnitMode
import com.uhufor.inspector.ui.ElementDetails
import com.uhufor.inspector.ui.AlignmentDetails
import com.uhufor.inspector.ui.OverlayCanvas
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ComposeStyleIntegrationTest {
    @get:Rule val rule = createComposeRule()
    private var previousInspectorFlag = false

    @Before fun enableInspection() {
        previousInspectorFlag = isDebugInspectorInfoEnabled
        isDebugInspectorInfoEnabled = true
    }

    @After fun restoreInspection() {
        isDebugInspectorInfoEnabled = previousInspectorFlag
    }

    private fun styles(tag: String): Set<UiNodeStyleProperties> {
        val node = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        return rule.runOnIdle { ComposeStyleExtractor.extract(node) }
    }

    private fun textStyle(tag: String) = styles(tag).filterIsInstance<UiNodeStyleProperties.TextStyle>().single()
    private fun background(tag: String) = styles(tag).filterIsInstance<UiNodeStyleProperties.ColorStyle>().single().backgroundColor

    @Test fun realTextAndTextFieldExposeLayoutStylesWithoutAppModifiers() {
        val expected = TextStyle(color = Color(0xCC123456), fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic,
            lineHeight = 30.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center, platformStyle = PlatformTextStyle(includeFontPadding = false))
        rule.setContent {
            Column {
                BasicText("안녕 Compose", Modifier.testTag("text").background(Color.Yellow), style = expected)
                BasicTextField("입력 값", {}, Modifier.testTag("field"), textStyle = expected)
            }
        }
        for (tag in listOf("text", "field")) {
            val style = textStyle(tag)
            assertEquals(if (tag == "text") "안녕 Compose" else "입력 값", style.text)
            assertEquals(Color(0xCC123456).toArgb(), style.textColor)
            assertEquals(21f, style.textSizeSp)
            assertEquals(600, style.fontWeight)
            assertEquals(true, style.isItalic)
            assertEquals(StyleTextUnit(30f, "sp"), style.lineHeight)
            assertEquals(StyleTextUnit(0.5f, "sp"), style.letterSpacing)
            assertEquals("monospace", style.fontFamily)
            assertEquals("Center", style.textAlign)
            assertEquals(false, style.includeFontPadding)
            assertNotNull(style.layout)
        }
        assertEquals(Color.Yellow.toArgb(), background("text"))
    }

    @Test fun gradientImageMultipleBackgroundsAndParentColorsAreNotGuessed() {
        rule.setContent {
            Column(Modifier.background(Color.Green)) {
                BasicText("child", Modifier.testTag("child"))
                Box(Modifier.size(24.dp).testTag("gradient").background(
                    Brush.horizontalGradient(listOf(Color.Red, Color.Blue))))
                Box(Modifier.size(24.dp).testTag("multiple").background(Color.Red).background(Color.Blue))
                Image(ColorPainter(Color.Blue), null,
                    Modifier.size(24.dp).testTag("image").background(Color.Red))
            }
        }
        for (tag in listOf("child", "gradient", "multiple", "image")) assertNull(background(tag))
    }

    @Test fun missingFailedAndThrowingLayoutActionsKeepTextButUnknownStyle() {
        rule.setContent {
            Column {
                Box(Modifier.size(24.dp).testTag("missing").semantics { text = AnnotatedString("fallback") })
                Box(Modifier.size(24.dp).testTag("false").semantics {
                    text = AnnotatedString("fallback")
                    getTextLayoutResult { false }
                })
                Box(Modifier.size(24.dp).testTag("throw").semantics {
                    text = AnnotatedString("fallback")
                    getTextLayoutResult { error("unavailable") }
                })
            }
        }
        for (tag in listOf("missing", "false", "throw")) {
            val style = textStyle(tag)
            assertEquals("fallback", style.text)
            assertNull(style.textColor)
            assertNull(style.textSizeSp)
            assertNull(style.isBold)
            assertNull(style.isItalic)
        }
    }

    @Test fun passwordDoesNotInvokeLayoutCallbackOrExposeContent() {
        var called = false
        rule.setContent {
            Box(Modifier.size(24.dp).testTag("password").semantics {
                password()
                text = AnnotatedString("secret")
                getTextLayoutResult { called = true; false }
            })
        }
        assertNull(textStyle("password").text)
        assertFalse(called)
    }

    @Test fun ambiguousLayoutResultsDoNotSelectFirstStyle() {
        var layout: TextLayoutResult? = null
        rule.setContent {
            Column {
                BasicText("source", onTextLayout = { layout = it }, style = TextStyle(color = Color.Red))
                Box(Modifier.size(24.dp).testTag("ambiguous").semantics {
                    text = AnnotatedString("fallback")
                    getTextLayoutResult { results ->
                        layout?.let { results.add(it); results.add(it) }
                        true
                    }
                })
            }
        }
        val result = textStyle("ambiguous")
        assertEquals("fallback", result.text)
        assertNull(result.textColor)
        assertNull(result.textSizeSp)
    }

    @Test fun panelShowsUnavailableValuesExplicitly() {
        val selection = SelectionState(1, RectF(0f, 0f, 100f, 50f), null,
            UiNodeProperties(UiNodeType.COMPOSE, "test", Size(100, 50), RectF(), RectF(), RectF(),
                styles = setOf(ComposeStyleExtractor.unknownText("visible content"),
                    ComposeStyleExtractor.unknownBackground())))
        rule.setContent {
            ElementDetails(selection, UnitMode.DP, false, {}, {}, { _, _, _, _, _, _, _, _ -> }, { _, _, _ -> })
        }
        rule.onNodeWithText("visible content").assertExists()
        // Text color, source size, weight, italic and background color all remain visible.
        assertTrue(rule.onAllNodesWithText("확인 불가").fetchSemanticsNodes().size >= 5)
        rule.onNodeWithText("Baseline guide").assertIsNotEnabled()
        rule.onNodeWithText("Text bounds guide").assertIsNotEnabled()
    }

    @Test fun paddedTextGeometryAndAppearanceMatchActualLayout() {
        var density = 0f
        var actualLayout: TextLayoutResult? = null
        val shape = RoundedCornerShape(8.dp)
        rule.setContent {
            density = LocalDensity.current.density
            BasicText("first\nsecond", Modifier.testTag("padded").background(Color.Red, shape)
                .border(2.dp, Color.Blue, shape).graphicsLayer(alpha = 0.75f).padding(12.dp),
                style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp), onTextLayout = { actualLayout = it })
        }
        val node = rule.onNodeWithTag("padded").fetchSemanticsNode()
        val styles = styles("padded")
        val appearance = styles.filterIsInstance<UiNodeStyleProperties.AppearanceStyle>().single()
        assertEquals(listOf(8f * density, 8f * density, 8f * density, 8f * density), appearance.cornerRadiiPx)
        assertEquals(2f, appearance.borderWidthDp)
        assertEquals(Color.Blue.toArgb(), appearance.borderColor)
        assertEquals(0.75f, appearance.layerAlpha)
        val geometry = styles.filterIsInstance<UiNodeStyleProperties.TextStyle>().single().layout!!
        assertEquals(node.boundsInWindow.left + 12f * density, geometry.outline.first().startX, 0.1f)
        assertEquals(node.boundsInWindow.top + 12f * density, geometry.outline.first().startY, 0.1f)
        assertEquals(actualLayout!!.firstBaseline, geometry.firstBaselinePx, 0.1f)
        assertEquals(actualLayout!!.lastBaseline, geometry.lastBaselinePx, 0.1f)
        assertEquals(2, geometry.baselines.size)
        assertEquals(2, geometry.lineCount)
    }

    @Test fun decoratedInputRetainsTypographyWithoutGuessingInnerTextCoordinates() {
        rule.setContent {
            BasicTextField("decorated", {}, Modifier.testTag("decorated"),
                textStyle = TextStyle(fontSize = 19.sp, color = Color.Blue),
                decorationBox = { inner -> Box(Modifier.padding(16.dp)) { inner() } })
        }
        val style = textStyle("decorated")
        assertEquals(19f, style.textSizeSp)
        assertEquals(Color.Blue.toArgb(), style.textColor)
        assertNull(style.layout)
    }

    @Test fun alignmentPanelTogglesEachGuideIndependently() {
        val layout = createTextLayoutInfo(100f, 40f, 2, 15f, 35f) { x, y -> x to y }
        val text = ComposeStyleExtractor.unknownText("test").copy(layout = layout)
        val selection = SelectionState(1, RectF(0f, 0f, 100f, 40f), null,
            UiNodeProperties(UiNodeType.COMPOSE, "guides", Size(100, 40), RectF(), RectF(), RectF(), styles = setOf(text)))
        var latest = TextGuideOptions()
        rule.setContent {
            var options by remember { mutableStateOf(TextGuideOptions()) }
            Column {
                AlignmentDetails(selection, UnitMode.DP, options) { options = it; latest = it }
            }
        }
        rule.onNodeWithText("Baseline guide").performClick().assertIsOn()
        rule.runOnIdle { assertEquals(TextGuideOptions(baselines = true), latest) }
        rule.onNodeWithText("Text bounds guide").performClick().assertIsOn()
        rule.runOnIdle { assertEquals(TextGuideOptions(baselines = true, bounds = true), latest) }
    }

    @Test fun asymmetricCornersRespectPhysicalRtlOrder() {
        var density = 0f
        val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomEnd = 12.dp, bottomStart = 16.dp)
        rule.setContent {
            density = LocalDensity.current.density
            Column {
                Box(Modifier.testTag("ltr").size(100.dp).background(Color.Red, shape))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.testTag("rtl").size(100.dp).background(Color.Red, shape))
                }
            }
        }
        fun corners(tag: String) = styles(tag).filterIsInstance<UiNodeStyleProperties.AppearanceStyle>().single().cornerRadiiPx
        assertEquals(listOf(4f, 8f, 12f, 16f).map { it * density }, corners("ltr"))
        assertEquals(listOf(8f, 4f, 16f, 12f).map { it * density }, corners("rtl"))
    }

    @Test fun guideRendererDrawsOnlyEnabledGuidesAtMeasuredCoordinates() {
        lateinit var host: View
        rule.setContent { host = LocalView.current; BasicText("host") }
        rule.runOnIdle {
            val layout = createTextLayoutInfo(100f, 50f, 2, 15f, 40f) { x, y -> x + 20f to y + 30f }
            val text = ComposeStyleExtractor.unknownText("test").copy(layout = layout)
            val selection = SelectionState(1, RectF(0f, 0f, 200f, 120f), null,
                UiNodeProperties(UiNodeType.COMPOSE, "render", Size(200, 120), RectF(), RectF(), RectF(), styles = setOf(text)))
            val bitmap = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val overlay = OverlayCanvas(host.context)
            overlay.drawTextGuides(canvas, selection, TextGuideOptions())
            assertEquals(android.graphics.Color.TRANSPARENT, bitmap.getPixel(60, 45))
            overlay.drawTextGuides(canvas, selection, TextGuideOptions(baselines = true))
            assertEquals(android.graphics.Color.MAGENTA, bitmap.getPixel(60, 45))
            assertEquals(android.graphics.Color.TRANSPARENT, bitmap.getPixel(60, 30))
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            overlay.drawTextGuides(canvas, selection, TextGuideOptions(bounds = true))
            assertEquals(android.graphics.Color.YELLOW, bitmap.getPixel(60, 30))
            assertEquals(android.graphics.Color.TRANSPARENT, bitmap.getPixel(60, 45))
            bitmap.recycle()
        }
    }

    @Test fun scanAndHitTestKeepGeometryAndShareExtractedStyles() {
        lateinit var host: View
        rule.setContent {
            host = LocalView.current
            BasicText("geometry", Modifier.testTag("target").size(120.dp, 48.dp),
                style = TextStyle(color = Color.Red, fontSize = 17.sp))
        }
        val node = rule.onNodeWithTag("target").fetchSemanticsNode()
        rule.runOnIdle {
            val root = host.rootView
            val bounds = node.boundsInWindow
            val scan = ComposeHitTester.scanAllElements(root).first {
                it.properties.id == node.id.toString()
            }
            assertEquals(bounds.width, scan.bounds.width(), 0.1f)
            assertEquals(bounds.height, scan.bounds.height(), 0.1f)
            assertEquals(node.layoutInfo.width, scan.properties.size.width)
            val hit = ComposeHitTester.hitTest(root, bounds.center.x.toInt(), bounds.center.y.toInt())!!
            assertEquals(node.id.toString(), hit.properties.id)
            assertEquals(scan.bounds, hit.bounds)
            assertEquals(scan.properties.styles, hit.properties.styles)
        }
    }

    @Test fun hitTestExtractsStylesOnlyForTheFinalCandidate() {
        lateinit var host: View
        var parentReads = 0
        var childReads = 0
        rule.setContent {
            host = LocalView.current
            Box(Modifier.size(160.dp).semantics {
                text = AnnotatedString("parent")
                getTextLayoutResult { parentReads++; false }
            }) {
                Box(Modifier.size(80.dp).testTag("winner").semantics {
                    text = AnnotatedString("child")
                    getTextLayoutResult { childReads++; false }
                })
            }
        }
        val node = rule.onNodeWithTag("winner", useUnmergedTree = true).fetchSemanticsNode()
        rule.runOnIdle {
            val bounds = node.boundsInWindow
            val hit = ComposeHitTester.hitTest(host.rootView, bounds.center.x.toInt(), bounds.center.y.toInt())!!
            assertEquals(node.id.toString(), hit.properties.id)
            assertEquals(0, parentReads)
            assertEquals(1, childReads)
            assertEquals("child", hit.properties.styles.filterIsInstance<UiNodeStyleProperties.TextStyle>().single().text)
        }
    }

    @Test fun viewTextAndSolidBackgroundRemainSupportedAndGradientIsUnknown() {
        lateinit var view: TextView
        rule.setContent {
            AndroidView(factory = { context ->
                TextView(context).apply {
                    text = "XML text"
                    setTextColor(Color.Blue.toArgb())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    background = ColorDrawable(Color.Red.toArgb())
                    view = this
                }
            }, modifier = Modifier.size(120.dp, 48.dp))
        }
        rule.runOnIdle {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val selection = ViewHitTester.hitTest(view, location[0] + 1, location[1] + 1)!!
            val text = selection.properties.styles.filterIsInstance<UiNodeStyleProperties.TextStyle>().single()
            assertEquals("XML text", text.text)
            assertEquals(Color.Blue.toArgb(), text.textColor)
            assertEquals(view.textSize, text.textSize)
            assertEquals(view.lineHeight.toFloat(), text.lineHeightPx)
            assertEquals(StyleTextUnit(view.letterSpacing, "em"), text.letterSpacing)
            assertEquals(view.includeFontPadding, text.includeFontPadding)
            assertNotNull(text.layout)
            assertEquals(Color.Red.toArgb(), selection.properties.styles.filterIsInstance<UiNodeStyleProperties.ColorStyle>().single().backgroundColor)
            view.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.Red.toArgb(), Color.Blue.toArgb()))
            assertNull(ViewHitTester.hitTest(view, location[0] + 1, location[1] + 1)!!.properties.styles.filterIsInstance<UiNodeStyleProperties.ColorStyle>().single().backgroundColor)
        }
    }
}
