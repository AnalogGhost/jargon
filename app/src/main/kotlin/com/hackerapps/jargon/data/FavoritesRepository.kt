package com.hackerapps.jargon.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val context: Context) {

    private val favoriteIdsKey = stringSetPreferencesKey("favorite_ids")

    val favoriteIds: Flow<Set<String>> = context.favoritesDataStore.data.map { prefs ->
        prefs[favoriteIdsKey] ?: emptySet()
    }

    suspend fun toggleFavorite(id: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[favoriteIdsKey] ?: emptySet()
            prefs[favoriteIdsKey] = if (id in current) current - id else current + id
        }
    }
}
