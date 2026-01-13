package com.example.pokepet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class PetViewModel : ViewModel() {
    // Estados Vitais
    var health by mutableFloatStateOf(0.5f)
    var hygiene by mutableFloatStateOf(0.1f) // Começa baixo após hatch
    var food by mutableFloatStateOf(0.1f)    // Começa baixo após hatch

    // Progressão
    var currentXP by mutableFloatStateOf(0f) // Começa no mínimo
    var currentLevel by mutableIntStateOf(1)

    val isPokeCenterUnlocked get() = currentLevel >= 2

    fun feed() {
        food = 1f // Sobe para o máximo
        gainXP(0.25f) // Ganha 25% de XP
    }

    fun clean() {
        hygiene = 1f // Sobe para o máximo
        gainXP(0.25f) // Ganha 25% de XP
    }

    private fun gainXP(amount: Float) {
        val nextXP = currentXP + amount
        if (nextXP >= 1f) {
            currentLevel++
            currentXP = 0f // Reset para o próximo nível
        } else {
            currentXP = nextXP
        }
    }
}