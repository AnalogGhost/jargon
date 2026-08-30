package com.hackerapps.jargon.ui

import com.hackerapps.jargon.data.DictionaryEntry

/**
 * Plain-text rendering of an entry for the Share and Copy actions: term, an optional
 * pronunciation/part-of-speech line, the definition, and a short source credit (the dictionary
 * content is CC BY-SA, so a shared snippet should say where it came from).
 */
fun DictionaryEntry.toShareText(): String = buildString {
    append(term)
    val subtitle = listOfNotNull(pronunciation, partOfSpeech).joinToString("  ·  ")
    if (subtitle.isNotEmpty()) {
        append("  ")
        append(subtitle)
    }
    append("\n\n")
    append(definition)
    append("\n\n— The Jargon File")
}
