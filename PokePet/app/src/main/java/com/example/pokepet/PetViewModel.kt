package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

// Modelos de Dados
enum class ItemType { FULL_HEAL, POTION, FULL_HEART, FULL_CLEAN, FULL_HUNGER, POKEBALL, ULTRABALL, MASTERBALL, IDENTIFIER, MAP }

data class InventoryItem(val id: Long = System.currentTimeMillis() + (0..9999).random(), val type: ItemType, val name: String, @DrawableRes val icon: Int)

// Modelo para os detalhes da Pokédex
data class CaughtPokemon(
    val pokemonId: Int,
    val name: String,
    val rarity: String,
    val xpReward: Float,
    val dateCaught: String,
    )

class PetViewModel : ViewModel() {
    // Estados Vitais
    var health by mutableFloatStateOf(0.7f)
    var hygiene by mutableFloatStateOf(0.7f)
    var food by mutableFloatStateOf(0.7f)
    var coins by mutableIntStateOf(200)

    // Progressão
    var currentXP by mutableFloatStateOf(0f)
    var currentLevel by mutableIntStateOf(1)

    // Tutorial e Flags
    var hasSeenPokeCenterTutorial by mutableStateOf(false)
    var hasShownPokeCenterUnlockWarning by mutableStateOf(false)
    var hasSeenPokedexTutorial by mutableStateOf(false)

    fun markPokedexTutorialAsSeen() { hasSeenPokedexTutorial = true }

    // Inventário e Desbloqueios
    val inventory = mutableStateListOf<InventoryItem>()
    val isPokeCenterUnlocked by derivedStateOf { currentLevel >= 2 }

    var caughtPokemonList = mutableStateListOf<CaughtPokemon>()

    // Lista de IDs (derivada) para compatibilidade com silhuetas
    val caughtPokemonIds by derivedStateOf { caughtPokemonList.map { it.pokemonId } }

    init { startDegradationLoop() }

    private fun startDegradationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(10000)
                food = (food - 0.01f).coerceAtLeast(0f)
                hygiene = (hygiene - 0.01f).coerceAtLeast(0f)
                if (food <= 0.1f || hygiene <= 0.1f) health = (health - 0.02f).coerceAtLeast(0f)
            }
        }
    }

    // Função atualizada para aceitar o objeto completo
    fun addToPokedex(pokemon: CaughtPokemon) {
        if (!caughtPokemonIds.contains(pokemon.pokemonId)) {
            caughtPokemonList.add(pokemon)
        }
    }

    fun gainXP(amount: Float) {
        val total = currentXP + amount
        if (total >= 1.0f) {
            currentLevel++
            currentXP = total - 1.0f
            coins += 100
        } else {
            currentXP = total
        }
    }

    fun feed() {
        food = 1f
        coins += 10
        updateHealth()
        gainXP(0.25f) }
    fun clean() {
        hygiene = 1f
        coins += 10
        updateHealth()
        gainXP(0.25f) }

    private fun updateHealth() {
        if (food > 0.8f && hygiene > 0.8f) health = min(health + 0.1f, 1f)
    }

    fun buyItem(type: ItemType, price: Int, name: String, icon: Int) {
        if (coins >= price) {
            coins -= price
            inventory.add(InventoryItem(type = type, name = name, icon = icon))
            currentXP += 0.1f

        }
    }

    fun useItem(item: InventoryItem) {
        when (item.type) {
            ItemType.FULL_HEAL -> { health = 1f; food = 1f; hygiene = 1f }
            ItemType.POTION -> health = min(health + 0.3f, 1f)
            ItemType.FULL_HEART -> health = 1f
            ItemType.FULL_CLEAN -> hygiene = 1f
            ItemType.FULL_HUNGER -> food = 1f
            else -> {}
        }
        inventory.remove(item)
    }
}