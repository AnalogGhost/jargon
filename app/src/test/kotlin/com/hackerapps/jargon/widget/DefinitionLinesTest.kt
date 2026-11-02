package com.hackerapps.jargon.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefinitionLinesTest {

    @Test
    fun `taller widgets show more lines`() {
        val heights = listOf(80f, 110f, 180f, 290f, 440f)
        val lines = heights.map { definitionLines(it, showLabel = true, showSubtitle = false) }
        assertEquals(lines, lines.sorted())
        assertTrue(lines.last() > lines.first())
    }

    @Test
    fun `always shows at least one line`() {
        assertEquals(1, definitionLines(40f, showLabel = true, showSubtitle = true))
    }

    @Test
    fun `the label and subtitle take room from the definition`() {
        val bare = definitionLines(290f, showLabel = false, showSubtitle = false)
        val full = definitionLines(290f, showLabel = true, showSubtitle = true)
        assertTrue(full < bare)
    }
}
