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
import com.teuapp.ui.FirstPageScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokePetTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login_screen"
                    ) {
                        // Login & Signup
                        composable("login_screen") {
                            LoginScreen(navController = navController)
                        }
                        composable("signup_screen") {
                            SignUpScreen(navController = navController)
                        }

                        // Hatching flow
                        composable("hatching_screen") {
                            PokePetScreen(
                                onNameConfirmed = { petName ->
                                    navController.navigate("main_screen/$petName")
                                }
                            )
                        }


                        // First page with navigation options
                        composable("first_page_screen") {
                            FirstPageScreen(
                                onPokeCenterClick = { navController.navigate("pokecenter_screen") },
                                onFoodClick = { navController.navigate("food_screen") },
                                onHygieneClick = { navController.navigate("bathroom_screen") }
                            )
                        }

                        // Main screen with pet name
                        composable(
                            route = "main_screen/{petName}",
                            arguments = listOf(navArgument("petName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val petName = backStackEntry.arguments?.getString("petName") ?: "PokePet"
                            PetMainScreen(navController = navController, petName = petName)
                        }

                        // Other feature screens
                        composable("camera_screen") { CameraScreen(navController = navController) }
                        composable("bathroom_screen") { BathroomScreen(navController = navController) }
                        composable("potions_screen") { PotionsScreen(navController = navController) }
                        composable("explore_screen") { ExploreScreen(navController = navController) }
                        composable("pokeballs_screen") { PokeballsScreen(navController = navController) }
                        composable("tools_screen") { ToolsScreen(navController = navController) }
                        composable("food_screen") { FoodScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
