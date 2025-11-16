package com.example.pokepet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pokepet.ui.theme.PokePetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokePetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Create a NavController
                    val navController = rememberNavController()

                    // 2. Set up the NavHost with routes
                    NavHost(
                        navController = navController,
                        startDestination = "hatching_screen" // The first screen to show
                    ) {
                        // Define the "hatching" screen
                        composable("hatching_screen") {
                            PokePetScreen(
                                onNameConfirmed = { petName ->
                                    // When name is confirmed, navigate to the main screen
                                    navController.navigate("main_screen/$petName")
                                }
                            )
                        }

                        // Define the "main" screen, which accepts a petName argument
                        composable(
                            route = "main_screen/{petName}",
                            arguments = listOf(navArgument("petName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            // Extract the name from the route and pass it to the screen
                            val petName = backStackEntry.arguments?.getString("petName") ?: "PokePet"
                            PetMainScreen(navController = navController, petName = petName)
                        }

                        composable("camera_screen") {
                            CameraScreen(navController = navController)
                        }

                        composable("bathroom_screen") {
                            BathroomScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}