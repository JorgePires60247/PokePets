package com.example.pokepet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(navController: NavController, viewModel: PetViewModel) {
    val sheetState = rememberModalBottomSheetState()
    var selectedPokemon by remember { mutableStateOf<CaughtPokemon?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    // 1. ADICIONEI OS NOVOS POKEMONS À LISTA
    val allPokemon = remember {
        listOf(
            R.drawable.bulbasaur,
            R.drawable.charmender,
            R.drawable.charmeleon,
            R.drawable.charizard,
            R.drawable.pikachu_happy,

            R.drawable.p_riolu, R.drawable.p_magikarp, R.drawable.p_jigglypuff,
            R.drawable.p_spiritomb, R.drawable.p_octillery, R.drawable.p_starmie,
            R.drawable.p_bidoof, R.drawable.p_chatot, R.drawable.p_azurill,
            R.drawable.p_chinchou, R.drawable.p_ledian, R.drawable.p_garchomp,
            R.drawable.p_chandelure, R.drawable.p_whismur, R.drawable.p_absol,
            R.drawable.p_beedrill, R.drawable.p_cramorant, R.drawable.p_dragonite,
            R.drawable.p_gardevoir, R.drawable.p_metagross, R.drawable.p_onix,
            R.drawable.p_petilil, R.drawable.p_psyduck, R.drawable.p_ribombee, R.drawable.p_mew
        )
    }

    if (showSheet && selectedPokemon != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            PokemonDetailContent(
                pokemon = selectedPokemon!!,
                isActive = viewModel.activeSpeciesId == selectedPokemon!!.pokemonId,
                onSwap = {
                    viewModel.swapActivePokemon(selectedPokemon!!) {
                        showSheet = false
                        navController.navigate("main_screen/${selectedPokemon!!.name}") {
                            popUpTo("main_screen") { inclusive = true }
                        }
                    }
                },
                onClose = { showSheet = false }
            )
        }
    }

    // Header ... (Mantém igual)
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Image(painterResource(R.drawable.pokedex_icon), null, Modifier.size(60.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Welcome to your PokeDex!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Captured: ${viewModel.caughtPokemonList.size} / ${allPokemon.size}", fontSize = 14.sp, color = Color.Gray)
            }
        }

        // Grelha
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allPokemon) { imageResId ->

                // 2. LÓGICA ATUALIZADA PARA INCLUIR CHARMELEON (4) E CHARIZARD (5)
                val caughtData = viewModel.caughtPokemonList.find { saved ->
                    saved.pokemonId == imageResId ||
                            (imageResId == R.drawable.bulbasaur && saved.pokemonId == 1) ||
                            (imageResId == R.drawable.charmender && saved.pokemonId == 2) ||
                            (imageResId == R.drawable.pikachu_happy && saved.pokemonId == 3) ||

                            // --- NOVAS REGRAS DE EVOLUÇÃO ---
                            (imageResId == R.drawable.charmeleon && saved.pokemonId == 4) ||
                            (imageResId == R.drawable.charizard && saved.pokemonId == 5)
                }

                PokedexCircleItem(
                    pokemonId = imageResId,
                    isCaught = caughtData != null,
                    isActive = viewModel.activeSpeciesId == caughtData?.pokemonId,
                    onClick = {
                        if (caughtData != null) {
                            selectedPokemon = caughtData
                            showSheet = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PokedexCircleItem(pokemonId: Int, isCaught: Boolean, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(if (isActive) Color(0xFFFFF176) else Color(0xFFF2F2F2))
            .clickable(enabled = isCaught) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = pokemonId),
            contentDescription = null,
            modifier = Modifier.size(70.dp),
            colorFilter = if (isCaught) null else ColorFilter.tint(Color(0xFFBDBDBD)),
            alpha = if (isCaught) 1f else 0.5f
        )
    }
}

@Composable
fun PokemonDetailContent(
    pokemon: CaughtPokemon,
    isActive: Boolean,
    onSwap: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        // 3. ATUALIZAR O DETALHE PARA MOSTRAR A IMAGEM CERTA
        val displayImage = when(pokemon.pokemonId) {
            1 -> R.drawable.bulbasaur
            2 -> R.drawable.charmender
            3 -> R.drawable.pikachu_happy
            4 -> R.drawable.charmeleon
            5 -> R.drawable.charizard
            else -> pokemon.pokemonId
        }

        Box(modifier = Modifier.size(120.dp).background(Color(0xFFF2F2F2), CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(displayImage), null, Modifier.size(90.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(pokemon.name, fontSize = 24.sp, fontWeight = FontWeight.Black)

        if (isActive) {
            Text("(Currently Active)", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            DetailRow("Rarity", pokemon.rarity)
            DetailRow("XP Bonus", "+${(pokemon.xpReward * 100).toInt()}%")
            DetailRow("Date", pokemon.dateCaught)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Close", color = Color.Black)
            }

            Button(
                onClick = onSwap,
                modifier = Modifier.weight(1f),
                enabled = !isActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color.Gray else Color(0xFF4CAF50)
                )
            ) {
                Text(if (isActive) "Active" else "Take Care", color = Color.White)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}