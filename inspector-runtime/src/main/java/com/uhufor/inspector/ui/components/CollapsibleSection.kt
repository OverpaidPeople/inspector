package com.uhufor.inspector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.uhufor.inspector.R
import com.uhufor.inspector.ui.DetailsSection
import com.uhufor.inspector.ui.DetailsSectionExpansion
import com.uhufor.inspector.ui.compose.dvdp
import com.uhufor.inspector.ui.compose.dvsp

@Composable
internal fun CollapsibleSection(
    section: DetailsSection,
    title: String,
    expansion: DetailsSectionExpansion,
    rightContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val expanded = expansion.isExpanded(section)
    val description = stringResource(if (expanded) R.string.inspector_section_expanded else R.string.inspector_section_collapsed)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Only this title area toggles. Adjacent Edit actions retain their own click target.
            Row(
                Modifier.weight(1f).testTag(section.headerTag)
                    .toggleable(value = expanded, role = Role.Button) { expansion.toggle(section) }
                    .semantics { stateDescription = description }
                    .padding(vertical = 2.dvdp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "▾" else "▸", fontSize = 11.dvsp,
                    modifier = Modifier.padding(end = 3.dvdp).clearAndSetSemantics {})
                SectionTitle(title, Modifier.weight(1f))
            }
            if (expanded) rightContent()
        }
        if (expanded) {
            Column(Modifier.fillMaxWidth().testTag(section.contentTag)) { content() }
        }
    }
}
