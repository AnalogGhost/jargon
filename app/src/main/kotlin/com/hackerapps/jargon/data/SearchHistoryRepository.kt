package com.hackerapps.jargon.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

class SearchHistoryRepository(private val context: Context) {

    private val recentSearchesKey = stringPreferencesKey("recent_searches")

    val recentSearches: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        decode(prefs[recentSearchesKey])
    }

    suspend fun record(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val updated = updatedSearchHistory(decode(prefs[recentSearchesKey]), query)
            prefs[recentSearchesKey] = Json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { it.remove(recentSearchesKey) }
    }

    private fun decode(stored: String?): List<String> =
        stored?.let { runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
}
