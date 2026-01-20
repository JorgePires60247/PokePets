package com.example.pokepet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
fun PokePetScreen(onNameConfirmed: (String) -> Unit = {}) {
    var isHatched by remember { mutableStateOf(false) }
    var petName by remember { mutableStateOf("Pikachu") }
    var tapCount by remember { mutableIntStateOf(0) }
    val targetTaps = 10

    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val currentEggImage = remember(tapCount, isHatched) {
        if (isHatched) {
            R.drawable.happy
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
            text = if (!isHatched) "Welcome to your PokePet!" else "Your pet has hatched!",
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
            // Barra de progresso visual (opcional, mas ajuda)
            LinearProgressIndicator(
                progress = { tapCount.toFloat() / targetTaps },
                modifier = Modifier.width(150.dp).padding(top = 8.dp),
                color = Color(0xFFE91E63)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = currentEggImage, // Usa a imagem dinâmica calculada acima
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale.value)
                    .rotate(rotation.value)
                    .clickable(enabled = !isHatched) {
                        tapCount++
                        scope.launch {
                            scale.animateTo(1.1f, tween(50))
                            scale.animateTo(1f, tween(50))

                            rotation.animateTo(10f, tween(40))
                            rotation.animateTo(-10f, tween(40))
                            rotation.animateTo(0f, tween(40))

                            if (tapCount >= targetTaps) {
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
            Text(text = "Would you like to name it?", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Pet Name") },
                singleLine = true,
                modifier = Modifier.width(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalName = if (petName.isBlank()) "Pikachu" else petName
                    onNameConfirmed(finalName)
                }
            ) {
                Text("Confirm")
            }
        }
    }
}
// Preview to test the hatching screen in isolation
@Preview(showBackground = true)
@Composable
fun PokePetScreenPreview() {
    PokePetScreen()
}
