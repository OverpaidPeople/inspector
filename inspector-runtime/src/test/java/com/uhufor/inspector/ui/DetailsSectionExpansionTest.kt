package com.uhufor.inspector.ui

import org.junit.Assert.*
import org.junit.Test

class DetailsSectionExpansionTest {
    @Test fun everySectionStartsExpanded() {
        val state = DetailsSectionExpansion()
        assertTrue(DetailsSection.entries.all(state::isExpanded))
    }

    @Test fun togglingASectionDoesNotAffectItsNeighbors() {
        val saved = mutableListOf<Set<DetailsSection>>()
        val state = DetailsSectionExpansion(onChange = { saved += it })
        state.toggle(DetailsSection.DETAILS)
        state.toggle(DetailsSection.MARGIN_PADDING)
        assertTrue(state.isExpanded(DetailsSection.DISTANCE))
        assertFalse(state.isExpanded(DetailsSection.DETAILS))
        assertFalse(state.isExpanded(DetailsSection.MARGIN_PADDING))
        state.toggle(DetailsSection.DETAILS)
        assertTrue(state.isExpanded(DetailsSection.DETAILS))
        assertEquals(setOf(DetailsSection.MARGIN_PADDING), state.collapsed)
        assertEquals(setOf(DetailsSection.DETAILS), saved.first())
        assertEquals(state.collapsed, saved.last())
    }

    @Test fun initialPreferencesAreCopiedRatherThanMutated() {
        val initial = mutableSetOf(DetailsSection.DISTANCE)
        val state = DetailsSectionExpansion(initial)
        initial.clear()
        assertFalse(state.isExpanded(DetailsSection.DISTANCE))
        state.toggle(DetailsSection.TEXT)
        assertTrue(initial.isEmpty())
        assertEquals(setOf(DetailsSection.DISTANCE, DetailsSection.TEXT), state.collapsed)
    }
}
