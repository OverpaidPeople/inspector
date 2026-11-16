package com.uhufor.inspector.ui

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

internal enum class DetailsSection(val preferenceKey: String) {
    DETAILS("details"), DISTANCE("distance"), MARGIN_PADDING("margin_padding"), TEXT("text"),
    ACTIONS("actions"), APPEARANCE("appearance"), ALIGNMENT("alignment");

    val headerTag get() = "inspector_section_$preferenceKey"
    val contentTag get() = "${headerTag}_content"
}

@Stable
internal class DetailsSectionExpansion(
    initialCollapsed: Set<DetailsSection> = emptySet(),
    private val onChange: (Set<DetailsSection>) -> Unit = {},
) {
    var collapsed by mutableStateOf(initialCollapsed.toSet())
        private set

    fun isExpanded(section: DetailsSection) = section !in collapsed

    fun toggle(section: DetailsSection) {
        collapsed = if (section in collapsed) collapsed - section else collapsed + section
        onChange(collapsed)
    }
}

internal class DetailsSectionStore(context: Context, name: String = DetailsPanelSizeStore.PREFERENCES_NAME) {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun load(): Set<DetailsSection> = runCatching {
        val keys = preferences.getStringSet(KEY, emptySet()).orEmpty()
        DetailsSection.entries.filterTo(mutableSetOf()) { it.preferenceKey in keys }
    }.getOrDefault(emptySet())

    fun save(collapsed: Set<DetailsSection>) {
        preferences.edit { putStringSet(KEY, collapsed.map { it.preferenceKey }.toSet()) }
    }

    private companion object { const val KEY = "collapsed_sections" }
}
