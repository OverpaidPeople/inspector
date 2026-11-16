package com.uhufor.inspector.engine

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ParagraphStyle
import android.view.View
import android.widget.TextView

internal object ViewStyleExtractor {
    fun textStyle(view: TextView): UiNodeStyleProperties.TextStyle {
        val spans = view.text as? Spanned
        val styledCharacters = spans?.getSpans(0, spans.length, CharacterStyle::class.java)?.isNotEmpty() == true
        val styledParagraphs = spans?.getSpans(0, spans.length, ParagraphStyle::class.java)?.isNotEmpty() == true
        return UiNodeStyleProperties.TextStyle(
            text = view.text.toString(),
            textColor = view.currentTextColor.takeUnless { styledCharacters },
            textSize = view.textSize.takeUnless { styledCharacters },
            isBold = view.typeface?.isBold.takeUnless { styledCharacters },
            isItalic = view.typeface?.isItalic.takeUnless { styledCharacters },
            fontWeight = view.typeface?.weight.takeUnless { styledCharacters },
            // Typeface does not expose a trustworthy family/resource name.
            lineHeightPx = view.lineHeight.toFloat().takeUnless { styledCharacters || styledParagraphs },
            letterSpacing = StyleTextUnit(view.letterSpacing, "em").takeUnless { styledCharacters },
            textAlign = view.layout?.alignment?.toString().takeUnless { styledParagraphs },
            includeFontPadding = view.includeFontPadding,
            layout = layout(view),
        )
    }

    private fun layout(view: TextView): TextLayoutInfo? = runCatching {
        val layout = view.layout ?: return null
        // Window transforms need a full coordinate mapping. Do not approximate rotated/scaled Views.
        var ancestor: View? = view
        while (ancestor != null) {
            if (!ancestor.matrix.isIdentity) return null
            ancestor = ancestor.parent as? View
        }
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val originX = location[0] + view.totalPaddingLeft - view.scrollX
        val originY = location[1] + view.totalPaddingTop - view.scrollY
        createTextLayoutInfo(layout.width.toFloat(), layout.height.toFloat(), layout.lineCount,
            layout.getLineBaseline(0).toFloat(), layout.getLineBaseline(layout.lineCount - 1).toFloat()
        ) { x, y -> originX + x to originY + y }
    }.getOrNull()

    fun appearance(view: View): UiNodeStyleProperties.AppearanceStyle {
        val corners = if (view.clipToOutline) null else when (val drawable = view.background) {
            is ColorDrawable -> listOf(0f, 0f, 0f, 0f)
            is GradientDrawable -> if (drawable.shape != GradientDrawable.RECTANGLE) null else {
                val radii = drawable.cornerRadii
                if (radii == null) List(4) { drawable.cornerRadius } else {
                    radii.toList().chunked(2).takeIf { pairs ->
                        pairs.size == 4 && pairs.all { it.size == 2 && it[0] == it[1] }
                    }?.map { it[0] }
                }
            }
            else -> null
        }
        // Drawable stroke details are not exposed reliably by public APIs.
        return UiNodeStyleProperties.AppearanceStyle(cornerRadiiPx = corners, layerAlpha = view.alpha)
    }
}
