package com.hackerapps.jargon.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.hackerapps.jargon.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Jargon", style = MaterialTheme.typography.titleLarge)
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Text(
                "A fully offline reader for the Jargon File — the hacker culture " +
                    "dictionary that gave the word \"hacker\" its original meaning. " +
                    "Nothing in this app ever touches the network.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text("App license", style = MaterialTheme.typography.titleMedium)
            Text(
                "This app's source code is licensed under the GNU General Public " +
                    "License v3.0 or later. See the LICENSE file in the source repository " +
                    "for the full text.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Text("Dictionary content", style = MaterialTheme.typography.titleMedium)
            Text(
                "Entries are drawn from the Jargon File, originally compiled by Eric S. " +
                    "Raymond and Guy L. Steele, and continued today as the Community Edition " +
                    "at github.com/agiacalone/jargonfile. Content is licensed under " +
                    "Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
