package com.uhufor.inspector.ui

import android.content.Context
import android.graphics.RectF
import android.os.SystemClock
import android.util.Size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.test.platform.app.InstrumentationRegistry
import com.uhufor.inspector.UnitMode
import com.uhufor.inspector.engine.SelectionState
import com.uhufor.inspector.engine.UiNodeActionProperties
import com.uhufor.inspector.engine.UiNodeProperties
import com.uhufor.inspector.engine.UiNodeStyleProperties
import com.uhufor.inspector.engine.UiNodeType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetailsSectionsTest {
    @get:Rule val rule = createComposeRule()

    private fun selection(id: Int, text: String, type: UiNodeType = UiNodeType.VIEW) = SelectionState(
        id, RectF(0f, 0f, 100f, 50f), null,
        UiNodeProperties(type, "selected-$id", Size(100, 50), RectF(), RectF(), RectF(),
            actions = setOf(UiNodeActionProperties.CLICKABLE),
            styles = setOf(
                UiNodeStyleProperties.TextStyle(text, 0xFF112233.toInt(), 16f, false, false),
                UiNodeStyleProperties.ColorStyle("ColorDrawable", 0xFFEEEEEE.toInt()),
                UiNodeStyleProperties.AppearanceStyle(listOf(8f, 8f, 8f, 8f), 2f, 0xFF112233.toInt(), 1f),
            )),
    )

    @Test fun allTitlesCollapseOnlyTheirOwnContentAndRemainAvailable() {
        rule.setContent {
            ElementDetails(selection(1, "visible text"), UnitMode.DP, false, {}, {},
                { _, _, _, _, _, _, _, _ -> }, { _, _, _ -> }, modifier = Modifier.size(320.dp, 860.dp))
        }
        DetailsSection.entries.forEach { section ->
            rule.onNodeWithTag(section.headerTag).performScrollTo().assertIsOn().performClick().assertIsOff()
            rule.onNodeWithTag(section.contentTag).assertDoesNotExist()
            rule.onNodeWithTag(section.headerTag).assertExists()
        }
        // Details is the ID group, not a parent switch that hides all other section titles.
        rule.onNodeWithText("ID").assertDoesNotExist()
        rule.onNodeWithTag(DetailsSection.DISTANCE.headerTag).performClick().assertIsOn()
        rule.onNodeWithTag(DetailsSection.DISTANCE.contentTag).assertExists()
        rule.onNodeWithTag(DetailsSection.MARGIN_PADDING.headerTag).assertIsOff()
        rule.onNodeWithText("visible text").assertDoesNotExist()
    }

    @Test fun collapseStateSurvivesSelectionAndUnitChanges() {
        val current = mutableStateOf(selection(1, "First item"))
        val unit = mutableStateOf(UnitMode.DP)
        rule.setContent {
            ElementDetails(current.value, unit.value, false, {}, {},
                { _, _, _, _, _, _, _, _ -> }, { _, _, _ -> }, modifier = Modifier.size(320.dp, 860.dp))
        }
        rule.onNodeWithTag(DetailsSection.DISTANCE.headerTag).performScrollTo().performClick()
        rule.onNodeWithTag(DetailsSection.TEXT.headerTag).performScrollTo().performClick()
        rule.runOnIdle { current.value = selection(2, "Second item", UiNodeType.COMPOSE); unit.value = UnitMode.PX }
        rule.onNodeWithTag(DetailsSection.DISTANCE.headerTag).assertIsOff()
        rule.onNodeWithTag(DetailsSection.TEXT.headerTag).assertIsOff()
        rule.onNodeWithText("Second item").assertDoesNotExist()
        rule.onNodeWithTag(DetailsSection.TEXT.headerTag).performScrollTo().performClick()
        rule.onNodeWithText("Second item").assertExists()
    }

    @Test fun editActionDoesNotCollapseTheSectionAndCancelKeepsOtherSectionsCollapsed() {
        val editing = mutableStateOf(false)
        rule.setContent {
            ElementDetails(selection(1, "Editable text"), UnitMode.DP, editing.value, { editing.value = it }, {},
                { _, _, _, _, _, _, _, _ -> }, { _, _, _ -> }, modifier = Modifier.size(320.dp, 860.dp))
        }
        rule.onNodeWithTag(DetailsSection.DISTANCE.headerTag).performScrollTo().performClick()
        rule.onAllNodesWithText("Edit")[0].performScrollTo().performClick()
        rule.onNodeWithText("Edit Margin/Padding").assertExists()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.onNodeWithTag(DetailsSection.MARGIN_PADDING.headerTag).assertIsOn()
        rule.onNodeWithTag(DetailsSection.DISTANCE.headerTag).assertIsOff()
        rule.onNodeWithTag(DetailsSection.DISTANCE.contentTag).assertDoesNotExist()
    }

    @Test fun savedSectionsReloadWithoutChangingPanelSizeAndIgnoreInvalidKeys() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "inspector_sections_test_${SystemClock.uptimeMillis()}"
        try {
            val size = DetailsPanelSize(250f, 350f)
            DetailsPanelSizeStore(context, name).save(size)
            val sections = setOf(DetailsSection.DISTANCE, DetailsSection.MARGIN_PADDING)
            val state = DetailsSectionExpansion(onChange = DetailsSectionStore(context, name)::save)
            sections.forEach(state::toggle)
            assertEquals(sections, DetailsSectionStore(context, name).load())
            assertEquals(size, DetailsPanelSizeStore(context, name).load())
            val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            preferences.edit { putStringSet("collapsed_sections", setOf("distance", "future_section")) }
            assertEquals(setOf(DetailsSection.DISTANCE), DetailsSectionStore(context, name).load())
            preferences.edit { putInt("collapsed_sections", 1) }
            assertTrue(DetailsSectionStore(context, name).load().isEmpty())
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
