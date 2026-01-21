package com.example.pokepet

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

// --- CATALOGO E MODELOS DE DADOS ---

object PokemonCatalog {
    // MUDANÇA 1: Usamos IDs numéricos em vez de Strings para facilitar
    const val BULBASAUR = 1
    const val CHARMANDER = 2
    const val PIKACHU = 3

    /** Retorna o recurso de imagem com base no ID (Int) e no estado */
    fun getPokemonImage(speciesId: Int, state: String = "HAPPY"): Int {
        return when (speciesId) {
            CHARMANDER -> when (state) {
                "DIRTY" -> R.drawable.charmender
                "WET" -> R.drawable.charmender
                else -> R.drawable.charmender
            }
            BULBASAUR -> when (state) {
                "DIRTY" -> R.drawable.bulbasaur
                "WET" -> R.drawable.bulbasaur
                else -> R.drawable.bulbasaur
            }
            PIKACHU -> when (state) {
                "DIRTY" -> R.drawable.ic_dirty_pikachu
                "WET" -> R.drawable.pikachu_wet
                else -> R.drawable.pikachu_happy
            }
            else -> R.drawable.p_mew
        }
    }
}

enum class ItemType { FULL_HEAL, POTION, FULL_HEART, FULL_CLEAN, FULL_HUNGER, POKEBALL, ULTRABALL, MASTERBALL, IDENTIFIER, MAP }

data class InventoryItem(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val type: ItemType = ItemType.POTION,
    val name: String = "",
    @DrawableRes val icon: Int = 0
)

data class CaughtPokemon(
    val pokemonId: Int = 0,
    val name: String = "",
    val rarity: String = "",
    val xpReward: Float = 0f,
    val dateCaught: String = ""
)

data class PokemonState(
    val health: Float = 0.7f,
    val hygiene: Float = 0.7f,
    val food: Float = 0.7f,
    val currentXP: Float = 0f,
    val currentLevel: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)

@RequiresApi(Build.VERSION_CODES.O)
class PetViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference

    // --- ESTADOS REACTIVOS (UI) ---
    var activePokemonId by mutableStateOf<String?>(null)
    var activePokemonName by mutableStateOf("PokePet")

    // MUDANÇA 2: activeSpeciesId agora é um Int (padrão Bulbasaur = 1)
    var activeSpeciesId by mutableIntStateOf(PokemonCatalog.BULBASAUR)

    var health by mutableFloatStateOf(0.7f)
    var hygiene by mutableFloatStateOf(0.7f)
    var food by mutableFloatStateOf(0.7f)
    var coins by mutableIntStateOf(200)
    var currentXP by mutableFloatStateOf(0f)
    var currentLevel by mutableIntStateOf(1)

    var hasSeenPokeCenterTutorial by mutableStateOf(false)
    var hasShownPokeCenterUnlockWarning by mutableStateOf(false)
    var hasSeenPokedexTutorial by mutableStateOf(false)

    val inventory = mutableStateListOf<InventoryItem>()
    var caughtPokemonList = mutableStateListOf<CaughtPokemon>()

    val isPokeCenterUnlocked by derivedStateOf { currentLevel >= 2 }
    val caughtPokemonIds by derivedStateOf { caughtPokemonList.map { it.pokemonId } }

    init {
        if (auth.currentUser != null) {
            loadUserDataFromFirebase()
        }
        startDegradationLoop()
    }

    // --- FIREBASE: CARREGAMENTO E LOGIN ---

    private fun userRef() = auth.currentUser?.uid?.let { dbRef.child("users").child(it) }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadActivePokemon(onResult: (Boolean, String?) -> Unit) {
        val ref = userRef() ?: return onResult(false, "Not logged in")
        ref.child("activePokemonId").get().addOnSuccessListener { snap ->
            val pid = snap.getValue(String::class.java)
            if (pid.isNullOrBlank()) {
                onResult(false, null)
            } else {
                loadUserDataFromFirebase()
                onResult(true, null)
            }
        }.addOnFailureListener { onResult(false, it.message) }
    }

    fun loadUserDataFromFirebase() {
        val ref = userRef() ?: return
        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                activePokemonId = snap.child("activePokemonId").getValue(String::class.java)
                activePokemonName = snap.child("activePokemonName").getValue(String::class.java) ?: "PokePet"

                // MUDANÇA 3: Carregar como Inteiro (Int::class.java)
                activeSpeciesId = snap.child("activeSpeciesId").getValue(Int::class.java) ?: PokemonCatalog.BULBASAUR

                health = snap.child("health").getValue(Float::class.java) ?: 0.7f
                hygiene = snap.child("hygiene").getValue(Float::class.java) ?: 0.7f
                food = snap.child("food").getValue(Float::class.java) ?: 0.7f
                coins = snap.child("coins").getValue(Int::class.java) ?: 200
                currentLevel = snap.child("currentLevel").getValue(Int::class.java) ?: 1
                currentXP = snap.child("currentXP").getValue(Float::class.java) ?: 0f
            }
        }
    }

    fun saveUserData() {
        val ref = userRef() ?: return
        val data = mapOf(
            "activePokemonId" to activePokemonId,
            "activePokemonName" to activePokemonName,
            "activeSpeciesId" to activeSpeciesId, // Agora salva como número (ex: 1, 2, 3)
            "health" to health, "hygiene" to hygiene, "food" to food, "coins" to coins,
            "currentLevel" to currentLevel, "currentXP" to currentXP,
            "hasSeenPokeCenterTutorial" to hasSeenPokeCenterTutorial,
            "hasSeenPokedexTutorial" to hasSeenPokedexTutorial,
            "inventory" to inventory.toList(),
            "pokedex" to caughtPokemonList.toList()
        )
        ref.updateChildren(data)
    }

    // --- LÓGICA DE HATCHING ---

    /** * MUDANÇA 4: Agora RECEBE o speciesId vindo da UI, não sorteia aqui.
     * Isso garante que o que o utilizador viu no ovo é o que é salvo.
     */
    fun createPokemonFromHatch(petName: String, speciesId: Int, onResult: (Boolean, String?) -> Unit) {
        val ref = userRef() ?: return onResult(false, "Not logged in")

        activePokemonId = ref.child("pokemons").push().key
        activePokemonName = petName.ifBlank { "PokePet" }
        activeSpeciesId = speciesId // Usamos o ID que veio da UI (Int)

        health = 1.0f; hygiene = 1.0f; food = 1.0f; currentXP = 0f; currentLevel = 1
        saveUserData()
        onResult(true, null)
    }

    // --- ACÇÕES DE JOGO (Mantêm-se iguais) ---

    fun feed() { food = 1f; coins += 10; updateHealth(); gainXP(0.25f); saveUserData() }
    fun clean() { hygiene = 1f; coins += 10; updateHealth(); gainXP(0.25f); saveUserData() }

    fun buyItem(type: ItemType, price: Int, name: String, icon: Int) {
        if (coins >= price) {
            coins -= price
            inventory.add(InventoryItem(type = type, name = name, icon = icon))
            saveUserData()
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
        saveUserData()
    }

    fun gainXP(amount: Float) {
        val nextXP = currentXP + amount
        if (nextXP >= 1f) { currentLevel++; currentXP = nextXP - 1f; coins += 100 }
        else currentXP = nextXP
        saveUserData()
    }

    fun addToPokedex(pokemon: CaughtPokemon) {
        if (!caughtPokemonIds.contains(pokemon.pokemonId)) {
            caughtPokemonList.add(pokemon)
            saveUserData()
        }
    }

    private fun startDegradationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(15000)
                food = (food - 0.01f).coerceAtLeast(0f)
                hygiene = (hygiene - 0.01f).coerceAtLeast(0f)
                if (food <= 0.1f || hygiene <= 0.1f) health = (health - 0.02f).coerceAtLeast(0f)
            }
        }
    }

    private fun updateHealth() { if (food > 0.8f && hygiene > 0.8f) health = min(health + 0.1f, 1f) }
    fun clearLocalPokemon() { activePokemonId = null; activePokemonName = "PokePet"; inventory.clear(); caughtPokemonList.clear() }
    fun markPokedexTutorialAsSeen() { hasSeenPokedexTutorial = true; saveUserData() }
}