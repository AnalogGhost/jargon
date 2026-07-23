package com.hackerapps.jargon.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JargonAssetTest {

    private val entries: List<DictionaryEntry> by lazy {
        val file = File("src/main/assets/jargon.json")
        assertTrue("jargon.json asset is missing", file.exists())
        Json.decodeFromString(file.readText())
    }

    @Test
    fun `has a substantial number of entries`() {
        assertTrue("expected at least 2000 entries, got ${entries.size}", entries.size >= 2000)
    }

    @Test
    fun `entry ids are unique`() {
        val ids = entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every entry has a non-blank term, sortKey, and definition`() {
        entries.forEach { entry ->
            assertTrue("entry ${entry.id} has a blank term", entry.term.isNotBlank())
            assertTrue("entry ${entry.id} has a blank sortKey", entry.sortKey.isNotBlank())
            assertTrue("entry ${entry.id} has a blank definition", entry.definition.isNotBlank())
        }
    }

    @Test
    fun `entries are sorted by sortKey`() {
        val sortKeys = entries.map { it.sortKey }
        assertEquals(sortKeys, sortKeys.sorted())
    }

    @Test
    fun `nearly all seeAlso references resolve to a real entry`() {
        // A handful of upstream cross-references don't exactly match their target's
        // term text (e.g. "ID10T" referencing the entry titled "ID10T error") -- that's
        // a content quirk, not a parser bug, and the app's cross-reference tap falls
        // back to search for those. This guards against the resolution rate collapsing,
        // which would signal a real parser regression.
        val termsLower = entries.map { it.term.lowercase() }.toSet()
        val allRefs = entries.flatMap { it.seeAlso }
        val resolved = allRefs.count { it.lowercase() in termsLower }
        val rate = resolved.toDouble() / allRefs.size
        assertTrue("seeAlso resolution rate dropped to ${rate * 100}%", rate >= 0.99)
    }

    @Test
    fun `a well-known entry parses as expected`() {
        val hacker = entries.find { it.id == "hacker" }
        assertTrue("expected an entry with id 'hacker'", hacker != null)
        assertEquals("hacker", hacker!!.term)
        assertTrue(hacker.definition.isNotBlank())
    }
}
