package com.hackerapps.jargon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first

private const val ROUTE_HOME = "home"
private const val ROUTE_ABOUT = "about"

// Back steps through the entries opened in the detail pane one at a time. The default
// (PopUntilScaffoldValueChange) would skip every entry in two-pane mode, where opening one
// doesn't change which panes are showing.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val BACK_BEHAVIOR = BackNavigationBehavior.PopUntilContentChange

/**
 * The dictionary is a list-detail layout: one pane at a time on phones, the list beside the entry
 * on tablets, foldables and landscape large screens. The detail pane's content is the open entry's
 * id; with no entry open, two-pane mode shows the word of the day there instead of a blank pane.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun JargonNavHost(
    launchTarget: LaunchTarget? = null,
    onLaunchHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val viewModel: JargonViewModel = viewModel()

    // Triggered by the "Random entry" launcher shortcut or a widget tap. The dictionary loads
    // asynchronously, so wait for the first non-empty entry list before navigating. A launch
    // replaces whatever was open, the same as starting the app fresh on that entry.
    LaunchedEffect(launchTarget) {
        if (launchTarget == null) return@LaunchedEffect
        viewModel.entries.first { it.isNotEmpty() }
        val target = when (launchTarget) {
            LaunchTarget.RandomEntry -> viewModel.randomEntry()
            is LaunchTarget.Entry -> viewModel.findById(launchTarget.id)
        }
        if (target != null) {
            navController.popBackStack(ROUTE_HOME, inclusive = false)
            while (navigator.canNavigateBack(BACK_BEHAVIOR)) navigator.navigateBack(BACK_BEHAVIOR)
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, target.id)
        }
        onLaunchHandled()
    }

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            DictionaryPanes(
                viewModel = viewModel,
                navigator = navigator,
                onAboutClick = { navController.navigate(ROUTE_ABOUT) }
            )
        }
        composable(ROUTE_ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun DictionaryPanes(
    viewModel: JargonViewModel,
    navigator: ThreePaneScaffoldNavigator<String>,
    onAboutClick: () -> Unit
) {
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val openId = navigator.currentDestination?.content
    val twoPane = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    val openEntry: (String) -> Unit = { id ->
        if (id != openId) navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
    }

    val onTermClick: (String) -> Unit = { term ->
        val target = viewModel.findByTermText(term)
        if (target != null) {
            openEntry(target.id)
        } else {
            // No exact entry: search for it instead, back on the list.
            viewModel.onSearchQueryChange(term)
            if (!twoPane) navigator.navigateTo(ListDetailPaneScaffoldRole.List)
        }
    }

    BackHandler(navigator.canNavigateBack(BACK_BEHAVIOR)) {
        navigator.navigateBack(BACK_BEHAVIOR)
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                EntryListScreen(
                    viewModel = viewModel,
                    onEntryClick = { entry -> openEntry(entry.id) },
                    onAboutClick = onAboutClick,
                    selectedId = if (twoPane) openId else null
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val entry = openId?.let { viewModel.findById(it) }
                val wordOfTheDay = if (entry == null && twoPane) viewModel.wordOfTheDay() else null
                val shown = entry ?: wordOfTheDay
                when {
                    shown != null -> EntryDetailScreen(
                        entry = shown,
                        isFavorite = shown.id in favoriteIds,
                        onToggleFavorite = { viewModel.toggleFavorite(shown.id) },
                        onTermClick = onTermClick,
                        onBack = if (navigator.canNavigateBack(BACK_BEHAVIOR)) {
                            { navigator.navigateBack(BACK_BEHAVIOR) }
                        } else {
                            null
                        },
                        label = if (entry == null) "Word of the day" else null
                    )
                    // Still loading the dictionary (the list pane shows its own spinner).
                    entries.isEmpty() && twoPane -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    )
}
