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

/** Where a launch from outside the app (launcher shortcut, widget) should land. */
sealed interface LaunchTarget {
    data object RandomEntry : LaunchTarget
    data class Entry(val id: String) : LaunchTarget
}

class MainActivity : ComponentActivity() {

    // Set when the activity is started from the "Random entry" launcher shortcut or the widget;
    // the NavHost consumes it and clears it back to null so it does not re-fire on recomposition.
    private var pendingLaunch by mutableStateOf<LaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity (rotation, process death) has already handled its launch intent.
        if (savedInstanceState == null) {
            pendingLaunch = launchTarget(intent)
        }
        enableEdgeToEdge()
        setContent {
            JargonTheme {
                JargonNavHost(
                    launchTarget = pendingLaunch,
                    onLaunchHandled = { pendingLaunch = null }
                )
            }
        }
    }

    // singleTop: a shortcut or widget tap while the app is already running is delivered here,
    // not onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTarget(intent)?.let { pendingLaunch = it }
    }

    private fun launchTarget(intent: Intent?): LaunchTarget? = when (intent?.action) {
        ACTION_RANDOM_ENTRY -> LaunchTarget.RandomEntry
        ACTION_VIEW_ENTRY -> intent.getStringExtra(EXTRA_ENTRY_ID)?.let { LaunchTarget.Entry(it) }
        else -> null
    }

    companion object {
        const val ACTION_RANDOM_ENTRY = "com.hackerapps.jargon.action.RANDOM_ENTRY"
        const val ACTION_VIEW_ENTRY = "com.hackerapps.jargon.action.VIEW_ENTRY"
        const val EXTRA_ENTRY_ID = "com.hackerapps.jargon.extra.ENTRY_ID"
    }
}
