package com.uhufor.inspector.engine

import android.graphics.RectF
import android.util.Size

internal data class SelectionState(
    val id: Int,
    val bounds: RectF,
    val parentBounds: RectF?,
    val properties: UiNodeProperties,
)

internal enum class UiNodeType(val value: String) {
    VIEW("XML"),
    COMPOSE("Compose")
}

internal enum class UiNodeActionProperties(val value: String) {
    CLICKABLE("Clickable"),
    LONG_CLICKABLE("Long Clickable"),
    SELECTABLE("Selectable"),
    CHECKABLE("Checkable"),
    FOCUSABLE("Focusable"),
}

internal sealed interface UiNodeStyleProperties {
    data class AppearanceStyle(
        // Physical corner order: top-left, top-right, bottom-right, bottom-left.
        val cornerRadiiPx: List<Float>? = null,
        val borderWidthDp: Float? = null,
        val borderColor: Int? = null,
        // Product of readable local graphics layers, excluding ancestors and fill alpha.
        val layerAlpha: Float? = null,
    ) : UiNodeStyleProperties

    data class ColorStyle(
        val backgroundType: String?,
        val backgroundColor: Int?,
    ) : UiNodeStyleProperties

    data class TextStyle(
        val text: String?,
        val textColor: Int?,
        val textSize: Float?,
        val isBold: Boolean?,
        val isItalic: Boolean?,
        // View textSize remains pixels for the existing editor; Compose preserves source sp.
        val textSizeSp: Float? = null,
        val fontWeight: Int? = null,
        val fontFamily: String? = null,
        val lineHeight: StyleTextUnit? = null,
        val lineHeightPx: Float? = null,
        val letterSpacing: StyleTextUnit? = null,
        val textAlign: String? = null,
        val includeFontPadding: Boolean? = null,
        val layout: TextLayoutInfo? = null,
    ) : UiNodeStyleProperties
}

internal data class StyleTextUnit(val value: Float, val unit: String)

internal data class GuideLine(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

internal data class TextLayoutInfo(
    val widthPx: Float,
    val heightPx: Float,
    val lineCount: Int,
    val firstBaselinePx: Float,
    val lastBaselinePx: Float,
    // Coordinates are in window space for Compose, screen space for Views (like selections).
    val outline: List<GuideLine>,
    val baselines: List<GuideLine>,
)

internal data class TextGuideOptions(val baselines: Boolean = false, val bounds: Boolean = false)

internal data class UiNodeProperties(
    val type: UiNodeType,
    val id: String,
    val size: Size,
    val distance: RectF,
    val margin: RectF,
    val padding: RectF,
    val actions: Set<UiNodeActionProperties> = emptySet(),
    val styles: Set<UiNodeStyleProperties> = emptySet(),
) {
    val isClickable: Boolean
        get() = actions.contains(UiNodeActionProperties.CLICKABLE)

    val isLongClickable: Boolean
        get() = actions.contains(UiNodeActionProperties.LONG_CLICKABLE)
}
