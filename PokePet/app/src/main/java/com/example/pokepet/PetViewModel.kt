package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

// 1. Definição de todos os tipos de itens disponíveis
enum class ItemType {
    FULL_HEAL, POTION, FULL_HEART, FULL_CLEAN, FULL_HUNGER, // Potions
    POKEBALL, ULTRABALL, MASTERBALL,                       // Pokeballs
    IDENTIFIER,                              // Tools
    MAP                                                    // Special
}

// 2. Modelo de dados para o Item no Inventário
data class InventoryItem(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val type: ItemType,
    val name: String,
    @DrawableRes val icon: Int
)

class PetViewModel : ViewModel() {
    // --- ESTADOS VITAIS ---
    var health by mutableFloatStateOf(0.5f)
    var hygiene by mutableFloatStateOf(0.7f)
    var food by mutableFloatStateOf(0.7f)
    var coins by mutableIntStateOf(3000)

    // --- PROGRESSÃO ---
    var currentXP by mutableFloatStateOf(0f)
    var currentLevel by mutableIntStateOf(1)

    // --- INVENTÁRIO ---
    val inventory = mutableStateListOf<InventoryItem>()

    // --- DESBLOQUEIOS ---
    val isPokeCenterUnlocked by derivedStateOf { currentLevel >= 2 }

    init {
        // Inicia o loop que consome os recursos do pet ao longo do tempo
        startDegradationLoop()
    }

    // --- LÓGICA DE TEMPO REAL ---

    private fun startDegradationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                // Usar coerção para evitar valores impossíveis
                food = (food - 0.02f).coerceAtLeast(0f)
                hygiene = (hygiene - 0.01f).coerceAtLeast(0f)

                if (food <= 0f || hygiene <= 0f) {
                    health = (health - 0.05f).coerceAtLeast(0f)
                }
            }
        }
    }

    // --- AÇÕES DE CUIDADO ---

    fun feed() {
        food = 1f
        updateHealthLogic()
        gainXP(0.50f)
    }

    fun clean() {
        hygiene = 1f
        updateHealthLogic()
        gainXP(0.50f)
    }

    // --- SISTEMA DE COMPRAS ---

    fun buyItem(type: ItemType, price: Int, name: String, @DrawableRes icon: Int) {
        val countOfType = inventory.count { it.type == type }

        // Define o limite: Mapa é 1, o resto é 10
        val limit = if (type == ItemType.MAP) 1 else 10

        if (coins >= price && countOfType < limit) {
            coins -= price
            inventory.add(InventoryItem(type = type, name = name, icon = icon))
        }
    }

    // --- SISTEMA DE USO DE ITENS ---

    fun useItem(item: InventoryItem, onMapClick: () -> Unit = {}) {
        when (item.type) {
            ItemType.MAP -> {
                onMapClick()
                return
            }
            ItemType.FULL_HEAL -> { health = 1f; hygiene = 1f; food = 1f }
            ItemType.POTION -> {
                if (food < hygiene) food = min(food + 0.25f, 1f)
                else hygiene = min(hygiene + 0.25f, 1f)
                updateHealthLogic()
            }
            ItemType.FULL_HEART -> health = 1f
            ItemType.FULL_CLEAN -> { hygiene = 1f; updateHealthLogic() }
            ItemType.FULL_HUNGER -> { food = 1f; updateHealthLogic() }
            else -> {

            }
        }

        inventory.remove(item)
    }

    // --- LÓGICA INTERNA (PRIVATE) ---

    private fun updateHealthLogic() {
        // Regra: Ambas vitais a 100% -> Health 100%.
        // Apenas uma a 100% -> Health +25%
        if (hygiene >= 1f && food >= 1f) {
            health = 1f
        } else if (hygiene >= 1f || food >= 1f) {
            health = min(health + 0.25f, 1f)
        }
    }

    private fun gainXP(amount: Float) {
        val nextXP = currentXP + amount
        if (nextXP >= 1f) {
            currentLevel++
            currentXP = 0f
            coins += 50 // Bónus de moedas ao subir de nível
        } else {
            currentXP = nextXP
        }
    }
}