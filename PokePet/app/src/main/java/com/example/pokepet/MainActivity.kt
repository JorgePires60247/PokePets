package com.example.pokepet

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // PokePetTheme configurado sem temas dinâmicos para consistência visual
            PokePetTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Inicialização do ViewModel compartilhado para persistência entre ecrãs
                    val petViewModel: PetViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "login_screen" // Define o Login como ponto de partida
                    ) {
                        // --- AUTENTICAÇÃO ---
                        composable("login_screen") {
                            LoginScreen(navController = navController, petViewModel = petViewModel)
                        }
                        composable("signup_screen") {
                            SignUpScreen(navController = navController, petViewModel = petViewModel)
                        }

                        // --- FLUXO INICIAL (HATCHING) ---
                        composable("hatching_screen") {
                            PokePetScreen(
                                onNameConfirmed = { petName, speciesId ->
                                    // Passamos o nome E o ID da espécie para o ViewModel
                                    petViewModel.createPokemonFromHatch(petName, speciesId) { success, _ ->
                                        if (success) {
                                            navController.navigate("main_screen/$petName") {
                                                popUpTo("hatching_screen") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // --- ECRÃ PRINCIPAL ---
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

                        // --- ATIVIDADES E CUIDADOS ---
                        composable("bathroom_screen") {
                            BathroomScreen(navController = navController, viewModel = petViewModel)
                        }

                        composable("food_screen") {
                            FoodScreen(navController = navController, viewModel = petViewModel)
                        }

                        // --- EXPLORAÇÃO E LOJA (POKECENTER) ---
                        composable("energy_screen") {
                            EnergyScreen(navController = navController, viewModel = petViewModel)
                        }

                        composable("pokedex_screen") {
                            PokedexScreen(navController = navController, viewModel = petViewModel)
                        }

                        composable("map_screen") {
                            MapScreen(navController = navController, viewModel = petViewModel)
                        }

                        // --- MINIJOGO DE CAPTURA ---
                        composable(
                            route = "catch/{pokemonId}/{xpReward}",
                            arguments = listOf(
                                navArgument("pokemonId") { type = NavType.IntType },
                                navArgument("xpReward") { type = NavType.FloatType }
                            )
                        ) { backStackEntry ->
                            val pokemonId = backStackEntry.arguments?.getInt("pokemonId") ?: 0
                            val xpReward = backStackEntry.arguments?.getFloat("xpReward") ?: 0f

                            CatchScreen(
                                navController = navController,
                                viewModel = petViewModel,
                                pokemonId = pokemonId,
                                xpReward = xpReward
                            )
                        }
                    }
                }
            }
        }
    }
}