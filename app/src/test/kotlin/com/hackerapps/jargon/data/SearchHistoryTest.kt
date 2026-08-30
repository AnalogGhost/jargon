package com.hackerapps.jargon.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryTest {

    @Test
    fun `adds a new query to the front of an empty history`() {
        assertEquals(listOf("hacker"), updatedSearchHistory(emptyList(), "hacker"))
    }

    @Test
    fun `most recent query comes first`() {
        val history = updatedSearchHistory(updatedSearchHistory(emptyList(), "foo"), "bar")
        assertEquals(listOf("bar", "foo"), history)
    }

    @Test
    fun `re-searching an existing term moves it to the front without duplicating`() {
        val history = listOf("kludge", "hacker", "foo")
        assertEquals(listOf("hacker", "kludge", "foo"), updatedSearchHistory(history, "hacker"))
    }

    @Test
    fun `duplicate detection is case-insensitive`() {
        val history = listOf("Hacker", "kludge")
        assertEquals(listOf("HACKER", "kludge"), updatedSearchHistory(history, "HACKER"))
    }

    @Test
    fun `query is trimmed before storing`() {
        assertEquals(listOf("hacker"), updatedSearchHistory(emptyList(), "  hacker  "))
    }

    @Test
    fun `blank or whitespace-only queries are ignored`() {
        val history = listOf("hacker")
        assertEquals(history, updatedSearchHistory(history, ""))
        assertEquals(history, updatedSearchHistory(history, "   "))
    }

    @Test
    fun `history is capped at MAX_RECENT_SEARCHES, dropping the oldest`() {
        var history = emptyList<String>()
        for (i in 1..MAX_RECENT_SEARCHES + 3) {
            history = updatedSearchHistory(history, "q$i")
        }
        assertEquals(MAX_RECENT_SEARCHES, history.size)
        assertEquals("q${MAX_RECENT_SEARCHES + 3}", history.first())
        assertEquals("q4", history.last())
    }
}
