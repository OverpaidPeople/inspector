package com.uhufor.inspector.engine

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

internal data class ModifierStyleSnapshot(
    val name: String?, val properties: Map<String, Any?>,
    val width: Float, val height: Float,
)

internal object ComposeAppearanceExtractor {
    fun snapshot(modifier: Modifier, width: Float, height: Float): ModifierStyleSnapshot {
        val value = modifier as? InspectableValue
        return ModifierStyleSnapshot(value?.nameFallback,
            value?.inspectableElements?.associate { it.name to it.value }.orEmpty(), width, height)
    }

    fun extract(
        modifiers: List<ModifierStyleSnapshot>, density: Density, direction: LayoutDirection,
    ): UiNodeStyleProperties.AppearanceStyle {
        val borders = modifiers.filter { it.name == "border" }
        val border = borders.singleOrNull()?.properties
        val layers = modifiers.filter { it.name == "graphicsLayer" }
        // Never execute a graphicsLayer block: it is application code and may mutate state.
        val alphas = layers.map { it.properties["alpha"] as? Float }
        val alpha = if (alphas.isNotEmpty() && alphas.all { it != null && it.isFinite() && it in 0f..1f }) {
            alphas.filterNotNull().fold(1f, Float::times)
        } else null
        val shapeSources = modifiers.filter {
            it.name in setOf("background", "border") ||
                (it.name == "graphicsLayer" && it.properties["clip"] == true)
        }
        val shapes = shapeSources.map { snapshot ->
            runCatching {
                val shape = snapshot.properties["shape"] as? Shape ?: return@runCatching null
                when (val outline = shape.createOutline(Size(snapshot.width, snapshot.height), direction, density)) {
                    is Outline.Rectangle -> listOf(0f, 0f, 0f, 0f)
                    is Outline.Rounded -> with(outline.roundRect) {
                        val corners = listOf(topLeftCornerRadius, topRightCornerRadius, bottomRightCornerRadius, bottomLeftCornerRadius)
                        if (corners.all { it.x == it.y && it.x.isFinite() }) corners.map { it.x } else null
                    }
                    is Outline.Generic -> null
                }
            }.getOrNull()
        }
        val radii = if (modifiers.any { it.name == "BackgroundElement" } ||
            layers.any { it.properties.containsKey("block") }) null
        else shapes.takeIf { it.isNotEmpty() && it.none { value -> value == null } }
            ?.distinct()?.singleOrNull()
        val color = (border?.get("color") as? Color)
            ?: (border?.get("brush") as? SolidColor)?.value
        val width = (border?.get("width") as? Dp)?.value?.takeIf { it.isFinite() && it >= 0f }
        return UiNodeStyleProperties.AppearanceStyle(
            cornerRadiiPx = radii,
            borderWidthDp = width,
            borderColor = color?.takeIf { it.isSpecified }?.toArgb(),
            layerAlpha = alpha,
        )
    }
}
