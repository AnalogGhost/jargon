package com.hackerapps.jargon.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackerapps.jargon.data.DictionaryEntry
import com.hackerapps.jargon.data.DictionaryRepository
import com.hackerapps.jargon.data.FavoritesRepository
import com.hackerapps.jargon.data.SearchHistoryRepository
import com.hackerapps.jargon.data.filterByQuery
import com.hackerapps.jargon.data.filterFavorites
import com.hackerapps.jargon.data.findByTermText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Local search over ~2,300 entries is fast, but re-running it on every keystroke still churns the
// list; wait for a short typing pause first. Clearing the field (empty query) applies immediately.
private const val SEARCH_DEBOUNCE_MS = 200L

class JargonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DictionaryRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val searchHistoryRepository = SearchHistoryRepository(application)

    private val _entries = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    val entries: StateFlow<List<DictionaryEntry>> = _entries.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val recentSearches: StateFlow<List<String>> = searchHistoryRepository.recentSearches
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(FlowPreview::class)
    private val debouncedQuery: Flow<String> = _searchQuery
        .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }

    private val filteredEntries: StateFlow<List<DictionaryEntry>> =
        combine(_entries, debouncedQuery) { entries, query ->
            filterByQuery(entries, query)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visibleEntries: StateFlow<List<DictionaryEntry>> =
        combine(filteredEntries, favoriteIds, _showFavoritesOnly) { entries, favIds, favOnly ->
            filterFavorites(entries, favIds, favOnly)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _loadError.value = null
            try {
                _entries.value = repository.loadEntries()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _loadError.value = e.message ?: "Failed to load dictionary"
            }
        }
    }

    fun retryLoad() = loadEntries()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Records the current query in the recent-search history (call on an explicit search submit). */
    fun commitSearch() {
        val query = _searchQuery.value.trim()
        if (query.isNotEmpty()) {
            viewModelScope.launch { searchHistoryRepository.record(query) }
        }
    }

    fun onRecentSearchSelected(query: String) {
        _searchQuery.value = query
        commitSearch()
    }

    fun clearSearchHistory() {
        viewModelScope.launch { searchHistoryRepository.clear() }
    }

    fun toggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(id) }
    }

    fun findById(id: String): DictionaryEntry? = entries.value.find { it.id == id }

    fun findByTermText(term: String): DictionaryEntry? = findByTermText(entries.value, term)

    fun randomEntry(): DictionaryEntry? = entries.value.randomOrNull()
}
