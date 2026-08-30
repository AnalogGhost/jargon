package com.hackerapps.jargon.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hackerapps.jargon.ui.theme.JargonTheme

class MainActivity : ComponentActivity() {

    // Set when the activity is started from the "Random entry" launcher shortcut; the NavHost
    // consumes it and clears it back to false so it does not re-fire on recomposition.
    private var pendingRandomEntry by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRandomEntry = intent?.action == ACTION_RANDOM_ENTRY
        enableEdgeToEdge()
        setContent {
            JargonTheme {
                JargonNavHost(
                    launchRandomEntry = pendingRandomEntry,
                    onRandomEntryLaunched = { pendingRandomEntry = false }
                )
            }
        }
    }

    // singleTop: a shortcut tap while the app is already running is delivered here, not onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_RANDOM_ENTRY) {
            pendingRandomEntry = true
        }
    }

    companion object {
        const val ACTION_RANDOM_ENTRY = "com.hackerapps.jargon.action.RANDOM_ENTRY"
    }
}
