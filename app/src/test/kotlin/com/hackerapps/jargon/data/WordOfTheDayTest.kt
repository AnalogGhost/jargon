package com.hackerapps.jargon.data

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordOfTheDayTest {

    private val entries: List<DictionaryEntry> by lazy {
        Json.decodeFromString(File("src/main/assets/jargon.json").readText())
    }

    private fun entry(id: String, definitionLength: Int) =
        DictionaryEntry(id = id, term = id, sortKey = id, definition = "x".repeat(definitionLength))

    @Test
    fun `day number counts from 2001-01-01`() {
        assertEquals(0L, WordOfTheDay.dayNumber(LocalDate.of(2001, 1, 1)))
        assertEquals(1L, WordOfTheDay.dayNumber(LocalDate.of(2001, 1, 2)))
        assertEquals(-1L, WordOfTheDay.dayNumber(LocalDate.of(2000, 12, 31)))
    }

    // Reference values from the iOS app's WordOfTheDay over the same bundled jargon.json, so
    // both apps keep showing the same word on the same day.
    @Test
    fun `picks match the iOS app`() {
        val pool = WordOfTheDay.candidates(entries)
        assertEquals(1974, pool.size)
        mapOf(0L to "naive", 1L to "blivet", 2L to "gobble", 9070L to "demogroup", 9435L to "Zork", -1L to "driver")
            .forEach { (day, id) -> assertEquals("day $day", id, pool[WordOfTheDay.index(day, pool.size)].id) }
    }

    @Test
    fun `only entries with a substantial definition are candidates`() {
        val short = entry("short", WordOfTheDay.MINIMUM_DEFINITION_LENGTH - 1)
        val long = entry("long", WordOfTheDay.MINIMUM_DEFINITION_LENGTH)
        assertEquals(listOf(long), WordOfTheDay.candidates(listOf(short, long)))
        assertEquals(long, WordOfTheDay.entryFor(LocalDate.of(2026, 11, 1), listOf(short, long)))
    }

    @Test
    fun `no candidates means no word`() {
        assertNull(WordOfTheDay.entryFor(LocalDate.now(), listOf(entry("short", 10))))
    }

    @Test
    fun `the same day always picks the same entry and the next day moves on`() {
        val today = LocalDate.of(2026, 11, 1)
        val first = WordOfTheDay.entryFor(today, entries)
        assertEquals(first, WordOfTheDay.entryFor(today, entries))
        assertNotEquals(first, WordOfTheDay.entryFor(today.plusDays(1), entries))
    }

    @Test
    fun `index stays in range`() {
        (-500L..500L).forEach { day ->
            assertTrue(WordOfTheDay.index(day, 7) in 0 until 7)
        }
    }
}
