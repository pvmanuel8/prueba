package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Rutas de navegación de la app
 */
sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object Editor : Screen("editor/{imageId}") {
        fun createRoute(imageId: String) = "editor/$imageId"
    }
    data object Batch : Screen("batch")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

/**
 * NavGraph principal de la aplicación
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Gallery.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Pantalla de galería (inicio)
        composable(Screen.Gallery.route) {
            com.example.myapplication.ui.screens.gallery.GalleryScreen(
                onImageSelected = { imageId ->
                    navController.navigate(Screen.Editor.createRoute(imageId))
                },
                onBatchProcessing = {
                    navController.navigate(Screen.Batch.route)
                },
                onHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        // Pantalla de editor
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("imageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId") ?: ""
            com.example.myapplication.ui.screens.editor.EditorScreen(
                imageId = imageId,
                onBack = { navController.popBackStack() }
            )
        }

        // Pantalla de procesamiento por lotes
        composable(Screen.Batch.route) {
            // Obtener imágenes del caché global
            val images = com.example.myapplication.util.ImageCache.getAll()

            com.example.myapplication.ui.screens.batch.BatchScreen(
                onBack = { navController.popBackStack() },
                preloadedImages = images
            )
        }

        // Pantalla de historial
        composable(Screen.History.route) {
            com.example.myapplication.ui.screens.history.HistoryScreen(
                onBack = { navController.popBackStack() },
                onProjectSelected = { imageId ->
                    navController.navigate(Screen.Editor.createRoute(imageId))
                }
            )
        }
    }
}