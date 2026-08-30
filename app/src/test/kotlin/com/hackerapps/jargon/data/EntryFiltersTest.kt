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

    @Test
    fun `findByTermText does not trim surrounding whitespace`() {
        assertNull(findByTermText(all, " hacker"))
        assertNull(findByTermText(all, "hacker "))
    }

    @Test
    fun `findByTermText returns the first entry when two share a term`() {
        val first = entry("h1", "hacker", "first sense")
        val second = entry("h2", "hacker", "second sense")
        assertEquals(first, findByTermText(listOf(first, second), "hacker"))
    }

    @Test
    fun `filterByQuery ignores surrounding whitespace in the query`() {
        assertEquals(filterByQuery(all, "hacker"), filterByQuery(all, "  hacker  "))
        assertEquals(listOf(hacker), filterByQuery(all, "  hacker  ").take(1))
    }

    @Test
    fun `filterByQuery matches a substring of a term`() {
        assertEquals(listOf(kludge), filterByQuery(all, "ludge"))
    }

    @Test
    fun `filterByQuery preserves input order within the term-match group`() {
        val hackA = entry("a", "hack-a", "x")
        val hackB = entry("b", "hack-b", "y")
        val hackC = entry("c", "hack-c", "z")
        val entries = listOf(hackC, hackA, hackB)
        assertEquals(entries, filterByQuery(entries, "hack"))
    }

    @Test
    fun `filterByQuery returns every term match before any definition-only match`() {
        val alpha = entry("alpha", "alpha", "this one mentions zeta in its definition")
        val zeta = entry("zeta", "zeta", "no self reference")
        val zetaTwo = entry("zeta-two", "zeta-two", "also nothing")
        val entries = listOf(alpha, zeta, zetaTwo)
        assertEquals(listOf(zeta, zetaTwo, alpha), filterByQuery(entries, "zeta"))
    }

    @Test
    fun `filterByQuery on an empty entry list returns an empty list`() {
        assertEquals(emptyList<DictionaryEntry>(), filterByQuery(emptyList(), "hacker"))
    }

    @Test
    fun `filterFavorites preserves entry order`() {
        val result = filterFavorites(all, favoriteIds = setOf(kludge.id, hacker.id), showFavoritesOnly = true)
        assertEquals(listOf(hacker, kludge), result)
    }
}
