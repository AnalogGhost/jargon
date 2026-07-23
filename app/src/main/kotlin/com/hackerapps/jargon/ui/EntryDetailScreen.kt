package com.hackerapps.jargon.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hackerapps.jargon.data.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entry: DictionaryEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTermClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry.term) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val subtitle = listOfNotNull(entry.pronunciation, entry.partOfSpeech)
                .joinToString("  ·  ")
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }

            entry.etymology?.let {
                LinkedText(
                    text = it,
                    terms = entry.seeAlso,
                    onTermClick = onTermClick,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            LinkedText(
                text = entry.definition,
                terms = entry.seeAlso,
                onTermClick = onTermClick,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp)
            )

            entry.history?.let {
                LinkedText(
                    text = it,
                    terms = entry.seeAlso,
                    onTermClick = onTermClick,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (entry.seeAlso.isNotEmpty()) {
                LinkedText(
                    text = "See also: " + entry.seeAlso.joinToString(", "),
                    terms = entry.seeAlso,
                    onTermClick = onTermClick,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
