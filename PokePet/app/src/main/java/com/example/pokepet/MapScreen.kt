package com.example.pokepet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

// Modelo para guardar a posição de cada Pokébola
data class MapBall(val x: Float, val y: Float)

@Composable
fun MapScreen(navController: NavController) {
    // Gerar as posições das Pokébolas apenas uma vez
    val pokeballs = remember { generateRandomPositions(count = 6, minDistance = 0.2f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        // 1. Fundo do Mapa
        Image(
            painter = painterResource(id = R.drawable.ic_map1),
            contentDescription = "World Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Pokébolas Aleatórias
        pokeballs.forEach { ball ->
            Image(
                painter = painterResource(id = R.drawable.map_pball),
                contentDescription = "Map Pokéball",
                modifier = Modifier
                    .size(40.dp)
                    .offset(
                        x = maxWidth * ball.x,
                        y = maxHeight * ball.y
                    )
                    .clickable {
                        // Lógica futura: Ganhar moedas ou itens ao clicar
                        println("Clicaste numa Pokébola em ${ball.x}, ${ball.y}")
                    }
            )
        }

        // 3. Botão de Fechar
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White // Cor contrastante com o mapa
            )
        }
    }
}

/**
 * Função para gerar posições (0.0 a 1.0) evitando sobreposição.
 * @param count Quantidade de pokébolas
 * @param minDistance Distância mínima entre elas (0.1 = 10% do ecrã)
 */
fun generateRandomPositions(count: Int, minDistance: Float): List<MapBall> {
    val positions = mutableListOf<MapBall>()
    val random = Random(System.currentTimeMillis())

    var attempts = 0
    while (positions.size < count && attempts < 100) {
        val newX = random.nextFloat() * 0.8f + 0.1f // Evita as bordas (0.1 a 0.9)
        val newY = random.nextFloat() * 0.8f + 0.1f

        val isTooClose = positions.any { existing ->
            val dx = existing.x - newX
            val dy = existing.y - newY
            kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) < minDistance
        }

        if (!isTooClose) {
            positions.add(MapBall(newX, newY))
        }
        attempts++
    }
    return positions
}