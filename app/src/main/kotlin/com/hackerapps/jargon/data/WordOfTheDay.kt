package com.hackerapps.jargon.data

import java.time.LocalDate

/**
 * Picks the same "word of the day" in the app and the widget, with no shared storage: the choice
 * is a pure function of the local calendar day. Only entries with a substantial definition are
 * eligible, so the widget never lands on a one-line "See bit rot." redirect. Mirrors the iOS app's
 * WordOfTheDay, so both apps show the same word on the same day.
 */
object WordOfTheDay {
    const val MINIMUM_DEFINITION_LENGTH = 120

    private val REFERENCE_DAY = LocalDate.of(2001, 1, 1).toEpochDay()

    fun candidates(entries: List<DictionaryEntry>): List<DictionaryEntry> =
        entries.filter { it.definition.length >= MINIMUM_DEFINITION_LENGTH }

    fun entryFor(date: LocalDate, entries: List<DictionaryEntry>): DictionaryEntry? {
        val pool = candidates(entries)
        if (pool.isEmpty()) return null
        return pool[index(dayNumber(date), pool.size)]
    }

    /** Days since 2001-01-01, so the word changes at local midnight rather than UTC midnight. */
    fun dayNumber(date: LocalDate): Long = date.toEpochDay() - REFERENCE_DAY

    /**
     * Scrambles consecutive days across the pool so successive words aren't alphabetical
     * neighbours (splitmix64 finalizer).
     */
    fun index(day: Long, count: Int): Int {
        var z = day.toULong() + 0x9E3779B97F4A7C15uL
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        z = z xor (z shr 31)
        return (z % count.toULong()).toInt()
    }
}
