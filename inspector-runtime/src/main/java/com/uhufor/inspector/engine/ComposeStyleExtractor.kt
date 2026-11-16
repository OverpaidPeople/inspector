package com.uhufor.inspector.engine

import android.content.res.Resources
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/** Reads only the selected unmerged node. Never borrows a parent's or child's style. */
internal object ComposeStyleExtractor {
    fun extract(node: SemanticsNode, resources: Resources? = null): Set<UiNodeStyleProperties> = buildSet {
        val config = node.config
        val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
        val semanticText = config.getOrNull(SemanticsProperties.EditableText)
            ?: config.getOrNull(SemanticsProperties.Text)?.let { texts ->
                AnnotatedString(texts.joinToString("\n") { it.text })
            }
        // Do not retrieve or expose the unmasked TextField layout for passwords.
        if (config.getOrNull(SemanticsProperties.Password) != null) {
            add(unknownText(null))
        } else if (semanticText != null || action != null) {
            val results = mutableListOf<TextLayoutResult>()
            val succeeded = runCatching { action?.invoke(results) == true }.getOrDefault(false)
            val result = if (succeeded) results.singleOrNull() else null
            val input = result?.layoutInput
            add(if (input != null) textStyle(input.text, input.style, resources).copy(
                layout = runCatching {
                    val coordinates = node.layoutInfo.coordinates
                    // Editable semantics can belong to an outer decoration container. Its origin
                    // is not necessarily the inner text origin; do not guess a baseline offset.
                    val decoratedField = config.getOrNull(SemanticsProperties.EditableText) != null &&
                        coordinates.size != result.size
                    if (!coordinates.isAttached || decoratedField) null else createTextLayoutInfo(
                        result.size.width.toFloat(), result.size.height.toFloat(), result.lineCount,
                        result.firstBaseline, result.lastBaseline,
                    ) { x, y -> coordinates.localToWindow(Offset(x, y)).let { it.x to it.y } }
                }.getOrNull()
            ) else unknownText(semanticText?.text))
        }
        add(runCatching {
            background(node.layoutInfo.getModifierInfo().map { it.modifier })
        }.getOrElse { unknownBackground() })
        add(runCatching {
            val info = node.layoutInfo
            ComposeAppearanceExtractor.extract(info.getModifierInfo().map {
                ComposeAppearanceExtractor.snapshot(it.modifier,
                    it.coordinates.size.width.toFloat(), it.coordinates.size.height.toFloat())
            }, info.density, info.layoutDirection)
        }.getOrElse { UiNodeStyleProperties.AppearanceStyle() })
    }

    internal fun unknownText(text: String?) = UiNodeStyleProperties.TextStyle(
        text, null, null, null, null
    )

    /** Resolve overlapping spans before checking uniformity, independently for each property. */
    internal fun textStyle(text: AnnotatedString, style: TextStyle, resources: Resources? = null): UiNodeStyleProperties.TextStyle {
        val boundaries = sortedSetOf(0, text.length)
        text.spanStyles.forEach { span ->
            boundaries += span.start.coerceIn(0, text.length)
            boundaries += span.end.coerceIn(0, text.length)
        }
        val runs = boundaries.zipWithNext().filter { (start, end) -> start < end }.map { (start, _) ->
            text.spanStyles.filter { start >= it.start && start < it.end }
                .fold(style.toSpanStyle()) { base, span -> base.merge(span.item) }
        }.ifEmpty { listOf(style.toSpanStyle()) }
        fun <T> uniform(read: (SpanStyle) -> T?): T? = runs.map(read).distinct().singleOrNull()
        val paragraphBoundaries = sortedSetOf(0, text.length)
        text.paragraphStyles.forEach { paragraph ->
            paragraphBoundaries += paragraph.start.coerceIn(0, text.length)
            paragraphBoundaries += paragraph.end.coerceIn(0, text.length)
        }
        val paragraphs = paragraphBoundaries.zipWithNext().filter { (start, end) -> start < end }
            .map { (start, _) ->
                text.paragraphStyles.filter { start >= it.start && start < it.end }
                    .fold(style.toParagraphStyle()) { base, paragraph -> base.merge(paragraph.item) }
            }.ifEmpty { listOf(style.toParagraphStyle()) }
        val weight = uniform { it.fontWeight?.weight }
        return UiNodeStyleProperties.TextStyle(
            text = text.text,
            textColor = uniform {
                // Shader/gradient text cannot be represented by one ARGB value.
                when (val brush = it.brush) {
                    null -> it.color.takeIf(Color::isSpecified)?.toArgb()
                    is SolidColor -> brush.value.takeIf(Color::isSpecified)?.let { color ->
                        color.copy(alpha = color.alpha * it.alpha).toArgb()
                    }
                    else -> null
                }
            },
            textSize = null,
            textSizeSp = uniform { it.fontSize.takeIf { size -> size.isSp }?.value },
            fontWeight = weight,
            isBold = weight?.let { it >= FontWeight.Bold.weight },
            isItalic = uniform { it.fontStyle?.let { fontStyle -> fontStyle == FontStyle.Italic } },
            fontFamily = uniform { fontFamilyName(it.fontFamily, resources) },
            letterSpacing = uniform { it.letterSpacing.toStyleUnit() },
            lineHeight = paragraphs.map { it.lineHeight.toStyleUnit() }.distinct().singleOrNull(),
            textAlign = paragraphs.map {
                it.textAlign.takeUnless { align -> align == TextAlign.Unspecified }?.toString()
            }.distinct().singleOrNull(),
            includeFontPadding = paragraphs.map { it.platformStyle?.includeFontPadding }
                .distinct().singleOrNull(),
        )
    }

    private fun fontFamilyName(family: FontFamily?, resources: Resources?): String? = when (family) {
        null -> null
        FontFamily.Default -> "System default"
        is GenericFontFamily -> family.name
        is FontListFontFamily -> {
            // Names identify configured resources, not a guessed resolved fallback typeface.
            val names = family.fonts.map { font ->
                (font as? ResourceFont)?.let {
                    runCatching { resources?.getResourceName(it.resId) }.getOrNull()
                }
            }
            if (names.any { it == null }) null else names.distinct().joinToString(", ")
        }
        else -> null
    }

    internal fun unknownBackground() = UiNodeStyleProperties.ColorStyle(null, null)

    /** Configured local background paint, not a sample of composited screen pixels. */
    internal fun background(modifiers: List<Modifier>): UiNodeStyleProperties.ColorStyle {
        val values = modifiers.filterIsInstance<InspectableValue>()
        // Elements created before Inspector.install() retain an empty inspector callback.
        if (values.any { it.nameFallback == "BackgroundElement" }) return unknownBackground()
        // Image/custom drawing may cover a background; never report its color as the image's color.
        if (values.any { it.nameFallback in setOf("paint", "drawBehind", "drawWithContent", "drawWithCache") }) {
            return unknownBackground()
        }
        val backgrounds = values.filter { it.nameFallback == "background" }
        val background = backgrounds.singleOrNull() ?: return unknownBackground()
        val properties = background.inspectableElements.associate { it.name to it.value }
        val color = when (val brush = properties["brush"]) {
            null -> properties["color"] as? Color
            is SolidColor -> {
                val alpha = properties["alpha"] as? Float ?: return unknownBackground()
                brush.value.copy(alpha = brush.value.alpha * alpha)
            }
            else -> return unknownBackground()
        }?.takeIf { it.isSpecified } ?: return unknownBackground()
        return UiNodeStyleProperties.ColorStyle("Modifier.background", color.toArgb())
    }
}

internal fun TextUnit.toStyleUnit(): StyleTextUnit? = when {
    !value.isFinite() -> null
    isSp -> StyleTextUnit(value, "sp")
    isEm -> StyleTextUnit(value, "em")
    else -> null
}
