package com.example.pokepet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
            PokePetTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Criação do ViewModel compartilhado
                    val petViewModel: PetViewModel = viewModel()

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

                        // Fluxo de Hatching
                        composable("hatching_screen") {
                            PokePetScreen(
                                onNameConfirmed = { petName ->
                                    navController.navigate("main_screen/$petName")
                                }
                            )
                        }

                        // Tela Principal (PetMainScreen)
                        composable(
                            route = "main_screen/{petName}",
                            arguments = listOf(navArgument("petName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val petName = backStackEntry.arguments?.getString("petName") ?: "PokePet"
                            PetMainScreen(
                                navController = navController,
                                petName = petName,
                                viewModel = petViewModel
                            )
                        }



                        // Tela de Banheiro (BathroomScreen)
                        composable("bathroom_screen") {
                            BathroomScreen(navController = navController, viewModel = petViewModel)
                        }

                        composable("food_screen") {
                            FoodScreen(navController = navController, viewModel = petViewModel)
                        }

                        // Telas de funcionalidades (Movidas para dentro do NavHost)
                        composable("potions_screen") { PotionsScreen(navController = navController) }
                        composable("explore_screen") { ExploreScreen(navController = navController) }
                        composable("pokeballs_screen") { PokeballsScreen(navController = navController) }
                        composable("tools_screen") { ToolsScreen(navController = navController) }

                        // Tela do PokeCenter
                        composable("pokecenter_screen") { PokeCenterScreen(navController = navController) }
                    }
                }
            }
        }
    }
}