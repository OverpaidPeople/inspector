package com.uhufor.inspector.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.uhufor.inspector.ui.components.InfoRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InfoRowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun longValuesStayStillAndRemainReadableWithManualScrolling() {
        val value = "A long resource or text value whose full content must remain accessible in a narrow PROP panel"
        var displayedValue by mutableStateOf(value)
        rule.setContent {
            Box(Modifier.width(148.dp)) { InfoRow("Text", displayedValue) }
        }
        val text = rule.onNodeWithText(value)
        fun offset() = text.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()
        assertEquals(0f, offset(), 0f)
        rule.mainClock.advanceTimeBy(10_000)
        assertEquals(0f, offset(), 0f)
        text.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertTrue(offset() > 0f)
        rule.runOnIdle { displayedValue = "New selection: $value" }
        val newOffset = rule.onNodeWithText("New selection: $value").fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange].value()
        assertEquals(0f, newOffset, 0f)
    }
}
