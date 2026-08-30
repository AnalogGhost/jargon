package com.hackerapps.jargon.data

const val MAX_RECENT_SEARCHES = 8

/**
 * Returns [current] with [query] inserted at the front: trimmed, blank queries ignored, and any
 * existing case-insensitive duplicate removed so the list stays most-recent-first with no repeats
 * and no more than [MAX_RECENT_SEARCHES] entries.
 */
fun updatedSearchHistory(current: List<String>, query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return current
    return (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
        .take(MAX_RECENT_SEARCHES)
}
