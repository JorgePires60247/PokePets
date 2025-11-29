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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch


// Note the new parameter: onNameConfirmed
@Composable
fun PokePetScreen(onNameConfirmed: (String) -> Unit = {}) {
    var isHatched by remember { mutableStateOf(false) }
    var petName by remember { mutableStateOf("") }
    val rotation = remember { Animatable(0f) }
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
                text = "Tap the egg to make it hatch!",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AsyncImage(
            model = if (isHatched) R.drawable.happy else R.drawable.egg,
            contentDescription = if (isHatched) "Hatched Pet" else "Egg",
            modifier = Modifier
                .size(150.dp)
                .rotate(rotation.value)
                .clickable {
                    if (!isHatched) {
                        scope.launch {
                            val cycles = 4
                            repeat(cycles) {
                                rotation.animateTo(
                                    15f,
                                    animationSpec = tween(
                                        durationMillis = 80
                                    )
                                )
                                rotation.animateTo(
                                    -15f,
                                    animationSpec = tween(
                                        durationMillis = 160
                                    )
                                )
                            }
                            rotation.animateTo(
                                0f,
                                animationSpec = tween(durationMillis = 80)
                            )
                            isHatched = true
                        }
                    }
                }
        )

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
                modifier = Modifier.width(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- NEW: CONFIRM BUTTON ---
            Button(
                onClick = { onNameConfirmed(petName.ifBlank { "PokePet" }) },
                // Enable button only if the name field is not empty
                enabled = petName.isNotBlank()
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
