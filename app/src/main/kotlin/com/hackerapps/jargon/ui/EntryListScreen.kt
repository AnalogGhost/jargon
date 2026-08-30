package com.hackerapps.jargon.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hackerapps.jargon.data.DictionaryEntry
import kotlinx.coroutines.launch

private val SCRUBBER_LETTERS = listOf('#') + ('A'..'Z').toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    viewModel: JargonViewModel,
    onEntryClick: (DictionaryEntry) -> Unit,
    onAboutClick: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val visibleEntries by viewModel.visibleEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchFieldFocused by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jargon") },
                actions = {
                    IconButton(onClick = onAboutClick) {
                        Icon(Icons.Filled.Info, contentDescription = "About")
                    }
                    IconButton(onClick = { viewModel.toggleShowFavoritesOnly() }) {
                        Icon(
                            if (showFavoritesOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (showFavoritesOnly) "Show all entries" else "Show favorites only"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.randomEntry()?.let(onEntryClick)
                    }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Random entry")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loadError != null && entries.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load the dictionary", style = MaterialTheme.typography.titleMedium)
                        Text(
                            loadError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                        Button(onClick = { viewModel.retryLoad() }) { Text("Retry") }
                    }
                }
            }

            entries.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        label = { Text("Search") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.commitSearch()
                            keyboardController?.hide()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .onFocusChanged { searchFieldFocused = it.isFocused }
                    )

                    if (searchFieldFocused && searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                        RecentSearches(
                            searches = recentSearches,
                            onSelect = {
                                viewModel.onRecentSearchSelected(it)
                                keyboardController?.hide()
                            },
                            onClear = viewModel::clearSearchHistory
                        )
                    }

                    if (visibleEntries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (showFavoritesOnly) "No favorites yet" else "No entries found",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                                items(visibleEntries, key = { it.id }) { entry ->
                                    ListItem(
                                        headlineContent = { Text(entry.term) },
                                        supportingContent = {
                                            Text(entry.definition.take(80), maxLines = 1)
                                        },
                                        trailingContent = {
                                            if (entry.id in favoriteIds) {
                                                Icon(Icons.Filled.Star, contentDescription = "Favorite")
                                            }
                                        },
                                        modifier = Modifier.clickable { onEntryClick(entry) }
                                    )
                                    HorizontalDivider()
                                }
                            }
                            AlphabetScrubber(entries = visibleEntries, listState = listState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onClear) { Text("Clear") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(searches, key = { it }) { query ->
                SuggestionChip(
                    onClick = { onSelect(query) },
                    label = { Text(query, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
private fun AlphabetScrubber(
    entries: List<DictionaryEntry>,
    listState: LazyListState
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxHeight().width(28.dp).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SCRUBBER_LETTERS.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable {
                        scope.launch { listState.scrollToItem(indexForLetter(entries, letter)) }
                    }
            )
        }
    }
}

private fun indexForLetter(entries: List<DictionaryEntry>, letter: Char): Int {
    if (entries.isEmpty()) return 0
    if (letter == '#') return 0
    val key = letter.lowercaseChar()
    val index = entries.indexOfFirst { (it.sortKey.firstOrNull() ?: '#') >= key }
    return if (index >= 0) index else entries.lastIndex
}
