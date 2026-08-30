package com.hackerapps.jargon.ui

import com.hackerapps.jargon.data.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryShareTest {

    private fun entry(
        term: String = "hacker",
        definition: String = "One who enjoys exploring the details of programmable systems.",
        pronunciation: String? = null,
        partOfSpeech: String? = null,
    ) = DictionaryEntry(
        id = term,
        term = term,
        sortKey = term.lowercase(),
        pronunciation = pronunciation,
        partOfSpeech = partOfSpeech,
        definition = definition,
    )

    @Test
    fun `share text contains the term and the full definition`() {
        val text = entry().toShareText()
        assertTrue(text.startsWith("hacker"))
        assertTrue(text.contains("One who enjoys exploring the details of programmable systems."))
    }

    @Test
    fun `share text includes a source credit`() {
        assertTrue(entry().toShareText().trimEnd().endsWith("— The Jargon File"))
    }

    @Test
    fun `pronunciation and part of speech are rendered on the term line when present`() {
        val text = entry(pronunciation = "/hak'r/", partOfSpeech = "n.").toShareText()
        assertEquals("hacker  /hak'r/  ·  n.", text.lineSequence().first())
    }

    @Test
    fun `term line has no trailing separator when pronunciation and part of speech are absent`() {
        val firstLine = entry().toShareText().lineSequence().first()
        assertEquals("hacker", firstLine)
        assertFalse(firstLine.contains("·"))
    }

    @Test
    fun `definition is separated from the term line by a blank line`() {
        val lines = entry(partOfSpeech = "n.").toShareText().lines()
        assertEquals("hacker  n.", lines[0])
        assertEquals("", lines[1])
        assertEquals("One who enjoys exploring the details of programmable systems.", lines[2])
    }
}
