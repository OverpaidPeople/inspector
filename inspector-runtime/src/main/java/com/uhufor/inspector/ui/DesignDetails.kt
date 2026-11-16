package com.uhufor.inspector.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.uhufor.inspector.R
import com.uhufor.inspector.UnitMode
import com.uhufor.inspector.engine.SelectionState
import com.uhufor.inspector.engine.StyleTextUnit
import com.uhufor.inspector.engine.TextGuideOptions
import com.uhufor.inspector.engine.UiNodeStyleProperties
import com.uhufor.inspector.ui.components.InfoRow
import com.uhufor.inspector.ui.components.SectionTitle
import com.uhufor.inspector.ui.compose.dvdp
import com.uhufor.inspector.ui.compose.dvsp

@Composable
internal fun ExtendedTextProperties(style: UiNodeStyleProperties.TextStyle) {
    val unknown = stringResource(R.string.inspector_value_unavailable)
    fun StyleTextUnit.display() = "${value.toDisplayString()}$unit"
    InfoRow(stringResource(R.string.inspector_style_font_family), style.fontFamily ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_line_height), style.lineHeight?.display()
        ?: style.lineHeightPx?.let { "${it.toDisplayString()}px" } ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_letter_spacing), style.letterSpacing?.display() ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_text_align), style.textAlign ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_font_padding), when (style.includeFontPadding) {
        true -> stringResource(R.string.inspector_value_true)
        false -> stringResource(R.string.inspector_value_false)
        null -> unknown
    })
}

@Composable
internal fun AppearanceDetails(selection: SelectionState, unitMode: UnitMode) {
    val style = selection.properties.styles.filterIsInstance<UiNodeStyleProperties.AppearanceStyle>().firstOrNull()
    val background = selection.properties.styles.filterIsInstance<UiNodeStyleProperties.ColorStyle>().firstOrNull()
    if (style == null && background == null) return
    val density = LocalDensity.current.density
    val unknown = stringResource(R.string.inspector_value_unavailable)
    fun dimension(px: Float) = if (unitMode == UnitMode.DP) "${(px / density).toDisplayString()}dp"
        else "${px.toDisplayString()}px"
    Spacer(Modifier.height(8.dvdp))
    SectionTitle(stringResource(R.string.inspector_title_appearance))
    if (background != null) {
        background.backgroundType?.let { InfoRow(stringResource(R.string.inspector_style_bg_type), it) }
        InfoRow(stringResource(R.string.inspector_style_bg_color), background.backgroundColor?.toHexColor() ?: unknown,
            previewColor = background.backgroundColor)
    }
    if (style == null) return
    InfoRow(stringResource(R.string.inspector_style_radius), style.cornerRadiiPx?.let { corners ->
        if (corners.distinct().size == 1) dimension(corners.first())
        else corners.zip(listOf("TL", "TR", "BR", "BL")).joinToString(", ") { (value, side) -> "$side ${dimension(value)}" }
    } ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_border_width), style.borderWidthDp?.let {
        if (it == 0f) stringResource(R.string.inspector_style_hairline)
        else "${it.toDisplayString()}dp"
    } ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_border_color), style.borderColor?.toHexColor() ?: unknown,
        previewColor = style.borderColor)
    InfoRow(stringResource(R.string.inspector_style_layer_alpha), style.layerAlpha?.let {
        "${(it * 100).toDisplayString()}%"
    } ?: unknown)
}

private fun Int.toHexColor(): String = "#${Integer.toHexString(this).uppercase().padStart(8, '0')}"

@Composable
internal fun AlignmentDetails(
    selection: SelectionState, unitMode: UnitMode,
    options: TextGuideOptions, onChange: (TextGuideOptions) -> Unit,
) {
    val style = selection.properties.styles.filterIsInstance<UiNodeStyleProperties.TextStyle>()
        .firstOrNull() ?: return
    val layout = style.layout
    val density = LocalDensity.current.density
    val unknown = stringResource(R.string.inspector_value_unavailable)
    fun dimension(px: Float) = if (unitMode == UnitMode.DP) "${(px / density).toDisplayString()}dp"
        else "${px.toDisplayString()}px"
    Spacer(Modifier.height(8.dvdp))
    SectionTitle(stringResource(R.string.inspector_title_alignment))
    InfoRow(stringResource(R.string.inspector_style_text_bounds), layout?.let {
        "${dimension(it.widthPx)} × ${dimension(it.heightPx)}"
    } ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_lines), layout?.lineCount?.toString() ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_first_baseline), layout?.let { dimension(it.firstBaselinePx) } ?: unknown)
    InfoRow(stringResource(R.string.inspector_style_last_baseline), layout?.let { dimension(it.lastBaselinePx) } ?: unknown)
    Text(stringResource(R.string.inspector_baseline_origin), fontSize = 8.dvsp)
    GuideToggle(stringResource(R.string.inspector_baseline_guide), options.baselines, layout != null) {
        onChange(options.copy(baselines = it))
    }
    GuideToggle(stringResource(R.string.inspector_bounds_guide), options.bounds, layout != null) {
        onChange(options.copy(bounds = it))
    }
}

@Composable
private fun GuideToggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dvdp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 9.dvsp, modifier = Modifier.weight(1f))
        Text(stringResource(if (!enabled) R.string.inspector_value_unavailable
            else if (checked) R.string.inspector_guide_on else R.string.inspector_guide_off), fontSize = 9.dvsp)
    }
}
