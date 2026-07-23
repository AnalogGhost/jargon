package com.hackerapps.jargon.data

import kotlinx.serialization.Serializable

@Serializable
data class DictionaryEntry(
    val id: String,
    val term: String,
    val sortKey: String,
    val pronunciation: String? = null,
    val partOfSpeech: String? = null,
    val definition: String,
    val etymology: String? = null,
    val history: String? = null,
    val seeAlso: List<String> = emptyList()
)
