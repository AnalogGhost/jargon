package com.hackerapps.jargon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{entryId}"
private const val ROUTE_ABOUT = "about"

@Composable
fun JargonNavHost() {
    val navController = rememberNavController()
    val viewModel: JargonViewModel = viewModel()

    val onTermClick: (String) -> Unit = { term ->
        val target = viewModel.findByTermText(term)
        if (target != null) {
            navController.navigate("detail/${target.id}")
        } else {
            viewModel.onSearchQueryChange(term)
            navController.navigate(ROUTE_LIST) {
                popUpTo(ROUTE_LIST) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            EntryListScreen(
                viewModel = viewModel,
                onEntryClick = { entry -> navController.navigate("detail/${entry.id}") },
                onAboutClick = { navController.navigate(ROUTE_ABOUT) }
            )
        }
        composable(ROUTE_ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")
            val entry = entryId?.let { viewModel.findById(it) }
            if (entry != null) {
                val favoriteIds by viewModel.favoriteIds.collectAsState()
                EntryDetailScreen(
                    entry = entry,
                    isFavorite = entry.id in favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavorite(entry.id) },
                    onTermClick = onTermClick,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
