package com.hackerapps.jargon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun entry(id: String, term: String, definition: String = "def of $term") =
    DictionaryEntry(id = id, term = term, sortKey = term.lowercase(), definition = definition)

class EntryFiltersTest {

    private val hacker = entry("hacker", "hacker", "someone who enjoys programming")
    private val cracker = entry("cracker", "cracker", "a malicious meddler, see hacker sense 8")
    private val kludge = entry("kludge", "kludge", "an inelegant solution")
    private val all = listOf(hacker, cracker, kludge)

    @Test
    fun `blank query returns all entries unchanged`() {
        assertEquals(all, filterByQuery(all, ""))
        assertEquals(all, filterByQuery(all, "   "))
    }

    @Test
    fun `term matches are case-insensitive and come before definition-only matches`() {
        val result = filterByQuery(all, "HACKER")
        assertEquals(listOf(hacker, cracker), result)
    }

    @Test
    fun `definition-only matches are not duplicated as term matches`() {
        val result = filterByQuery(all, "malicious")
        assertEquals(listOf(cracker), result)
    }

    @Test
    fun `no matches returns an empty list`() {
        assertEquals(emptyList<DictionaryEntry>(), filterByQuery(all, "zzznotaword"))
    }

    @Test
    fun `favorites filter passes everything through when disabled`() {
        val result = filterFavorites(all, favoriteIds = setOf(hacker.id), showFavoritesOnly = false)
        assertEquals(all, result)
    }

    @Test
    fun `favorites filter keeps only favorited entries when enabled`() {
        val result = filterFavorites(all, favoriteIds = setOf(hacker.id, kludge.id), showFavoritesOnly = true)
        assertEquals(listOf(hacker, kludge), result)
    }

    @Test
    fun `favorites filter returns empty list when nothing is favorited`() {
        val result = filterFavorites(all, favoriteIds = emptySet(), showFavoritesOnly = true)
        assertEquals(emptyList<DictionaryEntry>(), result)
    }

    @Test
    fun `findByTermText matches case-insensitively`() {
        assertEquals(hacker, findByTermText(all, "Hacker"))
        assertEquals(hacker, findByTermText(all, "HACKER"))
    }

    @Test
    fun `findByTermText returns null when no entry has that exact term`() {
        assertNull(findByTermText(all, "hack"))
        assertNull(findByTermText(all, "hackers"))
    }
}
