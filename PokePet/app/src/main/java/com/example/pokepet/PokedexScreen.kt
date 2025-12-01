package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

data class Pokemon(@DrawableRes val imageRes: Int)

// Create a dummy list of Pokemon using existing drawables as placeholders
private val pokedexEntries = listOf(
    Pokemon(R.drawable.happy),
    Pokemon(R.drawable.clean_icon),
    Pokemon(R.drawable.duche),
    Pokemon(R.drawable.clean_page_icon),
    Pokemon(R.drawable.energy_page_icon),
    Pokemon(R.drawable.hunger_icon),
    Pokemon(R.drawable.hp_icon),
    Pokemon(R.drawable.happiness_icon),
    Pokemon(R.drawable.chuveirocagua),
    Pokemon(R.drawable.chuveirosagua),
    Pokemon(R.drawable.tap),
    Pokemon(R.drawable.google_logo),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                AsyncImage( // FIXED: Use AsyncImage for potentially animated drawables
                    model = R.drawable.happy, // Placeholder for Rotom
                    contentDescription = "Pokedex Icon",
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Welcome to your PokeDex!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pokemon Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) { 
                items(pokedexEntries) { pokemon ->
                    PokemonGridItem(pokemon = pokemon)
                }
            }
        }
    }
}

@Composable
fun PokemonGridItem(pokemon: Pokemon) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF2F2F2)) 
    ) {
        AsyncImage( // FIXED: Use AsyncImage to support GIFs
            model = pokemon.imageRes,
            contentDescription = "Pokemon Entry",
            modifier = Modifier
                .size(72.dp)
                .padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PokedexScreenPreview() {
    PokedexScreen(navController = rememberNavController())
}
