package com.uhufor.inspector.ui

import android.content.Context
import androidx.core.content.edit

internal class DetailsPanelSizeStore(context: Context, name: String = PREFERENCES_NAME) {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun load(): DetailsPanelSize? = runCatching {
        DetailsPanelSize(preferences.getFloat("width_dp", Float.NaN),
            preferences.getFloat("height_dp", Float.NaN)).takeIf { it.isValid() }
    }.getOrNull()

    fun save(size: DetailsPanelSize) {
        if (!size.isValid()) return
        // Write once when a drag ends, rather than issuing disk writes for every MOVE.
        preferences.edit {
            putFloat("width_dp", size.widthDp)
            putFloat("height_dp", size.heightDp)
        }
    }

    companion object {
        const val PREFERENCES_NAME = "com.uhufor.inspector.details_panel"
    }
}
