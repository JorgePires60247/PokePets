package com.example.pokepet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PokePetScreen(onNameConfirmed: (String, Int) -> Unit = { _, _ -> }) {
    var isHatched by remember { mutableStateOf(false) }
    var petName by remember { mutableStateOf("") }
    var tapCount by remember { mutableIntStateOf(0) }
    val targetTaps = 10

    // 1. Guardamos o ID da ESPÉCIE (1, 2 ou 3), e não o R.drawable direto
    var hatchedSpeciesId by remember { mutableIntStateOf(PokemonCatalog.BULBASAUR) }

    // 2. Lista de IDs possíveis (1, 2, 3)
    val availableIds = remember {
        listOf(
            PokemonCatalog.BULBASAUR,
            PokemonCatalog.CHARMANDER,
            PokemonCatalog.PIKACHU
        )
    }

    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Lógica da Imagem: Se nasceu, pede ao Catálogo a imagem do ID sorteado.
    val currentImage = remember(tapCount, isHatched, hatchedSpeciesId) {
        if (isHatched) {
            // AQUI ESTÁ O TRUQUE: Convertemos o ID (1, 2, 3) na imagem correta
            PokemonCatalog.getPokemonImage(hatchedSpeciesId)
        } else {
            when {
                tapCount < 2 -> R.drawable.egg
                tapCount < 4 -> R.drawable.egg2
                tapCount < 6 -> R.drawable.egg3
                else -> R.drawable.egg4
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (!isHatched) "Welcome to your PokePet!" else "A wild Pokémon appeared!",
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!isHatched) {
            Text(
                text = "Tap the egg to hatch it!",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = if (tapCount > 0) Color(0xFFE91E63) else Color.Gray
            )

            LinearProgressIndicator(
                progress = { tapCount.toFloat() / targetTaps },
                modifier = Modifier.width(150.dp).padding(top = 8.dp),
                color = Color(0xFFE91E63),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = currentImage,
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale.value)
                    .rotate(rotation.value)
                    .clickable(enabled = !isHatched) {
                        tapCount++
                        scope.launch {
                            // Animação de abanar
                            scale.animateTo(1.1f, tween(50))
                            scale.animateTo(1f, tween(50))

                            rotation.animateTo(10f, tween(40))
                            rotation.animateTo(-10f, tween(40))
                            rotation.animateTo(0f, tween(40))

                            // Se atingiu o total de toques
                            if (tapCount >= targetTaps) {
                                // 3. SORTEIO ALEATÓRIO DE IDs (1, 2 ou 3)
                                hatchedSpeciesId = availableIds.random()

                                scale.animateTo(1.5f, tween(200))
                                isHatched = true
                                scale.animateTo(1f, tween(200))
                            }
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isHatched) {
            // Mostra qual pokemon saiu no texto (Opcional)
            val pokemonName = when(hatchedSpeciesId) {
                PokemonCatalog.CHARMANDER -> "Charmander"
                PokemonCatalog.BULBASAUR -> "Bulbasaur"
                else -> "Pikachu"
            }
            Text(text = "It's a $pokemonName! Name it:", fontSize = 16.sp)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Pet Name") },
                placeholder = { Text("Buddy") },
                singleLine = true,
                modifier = Modifier.width(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalName = if (petName.isBlank()) "Buddy" else petName

                    // --- CORREÇÃO DO ERRO ---
                    // Agora passamos o Nome E o ID sorteado
                    onNameConfirmed(finalName, hatchedSpeciesId)
                }
            ) {
                Text("Adopt Partner")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PokePetScreenPreview() {
    PokePetScreen()
}
