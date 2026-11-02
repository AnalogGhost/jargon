package com.hackerapps.jargon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrubberLettersTest {

    @Test
    fun `shows every letter when they fit`() {
        assertEquals(27, scrubberLetters(27).size)
        assertEquals(27, scrubberLetters(40).size)
    }

    @Test
    fun `a short column keeps the ends and stays in order`() {
        val letters = scrubberLetters(14)
        assertEquals(14, letters.size)
        assertEquals('#', letters.first())
        assertEquals('Z', letters.last())
        assertEquals(letters.drop(1), letters.drop(1).sorted())
    }

    @Test
    fun `never fewer than the two ends`() {
        assertEquals(listOf('#', 'Z'), scrubberLetters(0))
        assertTrue(scrubberLetters(1).size == 2)
    }
}
