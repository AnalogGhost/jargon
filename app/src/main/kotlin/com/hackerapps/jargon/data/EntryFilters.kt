package com.hackerapps.jargon.data

fun filterByQuery(entries: List<DictionaryEntry>, query: String): List<DictionaryEntry> {
    if (query.isBlank()) return entries
    val q = query.trim()
    val termMatches = entries.filter { it.term.contains(q, ignoreCase = true) }
    val definitionMatches = entries.filter {
        it !in termMatches && it.definition.contains(q, ignoreCase = true)
    }
    return termMatches + definitionMatches
}

fun filterFavorites(
    entries: List<DictionaryEntry>,
    favoriteIds: Set<String>,
    showFavoritesOnly: Boolean
): List<DictionaryEntry> =
    if (showFavoritesOnly) entries.filter { it.id in favoriteIds } else entries

fun findByTermText(entries: List<DictionaryEntry>, term: String): DictionaryEntry? =
    entries.find { it.term.equals(term, ignoreCase = true) }
