package com.hackerapps.jargon.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DictionaryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadEntries(): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        context.assets.open("jargon.json").bufferedReader().use { reader ->
            json.decodeFromString<List<DictionaryEntry>>(reader.readText())
        }
    }
}
