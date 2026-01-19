package com.example.pokepet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
    val targetTaps = 10 // Número de toques necessários

    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

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
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- O OVO INTERATIVO ---
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = if (isHatched) R.drawable.happy else R.drawable.egg,
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale.value)
                    .rotate(rotation.value)
                    .clickable(enabled = !isHatched) {
                        tapCount++
                        scope.launch {
                            // Feedback de escala (pulsação) ao tocar
                            scale.animateTo(1.1f, tween(50))
                            scale.animateTo(1f, tween(50))

                            // Abanar o ovo
                            rotation.animateTo(10f, tween(40))
                            rotation.animateTo(-10f, tween(40))
                            rotation.animateTo(0f, tween(40))

                            if (tapCount >= targetTaps) {
                                // Efeito final de eclosão (escala maior)
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
            Text(
                text = "Would you like to name it?",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Pet Name") },
                singleLine = true,
                modifier = Modifier.width(200.dp),
                placeholder = { Text("Pikachu") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalName = if (petName.isBlank()) "Pikachu" else petName
                    onNameConfirmed(finalName)
                },
                enabled = true
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
