package com.hackerapps.jargon.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, entry.term)
                            putExtra(Intent.EXTRA_TEXT, entry.toShareText())
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(entry.toShareText()))
                        // Android 13+ shows its own copy confirmation; older versions show nothing.
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
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
