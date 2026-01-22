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

    const val BULBASAUR = 1
    const val CHARMANDER = 2
    const val PIKACHU = 3
    const val CHARMELEON = 4
    const val CHARIZARD = 5

    /** * Retorna o recurso de imagem.
     * Se o ID for 1, 2 ou 3, devolve a imagem específica (com estados HAPPY/DIRTY).
     * Se for outro número (ex: R.drawable.p_dragonite), devolve o próprio número.
     */
    fun getPokemonImage(speciesId: Int, state: String = "HAPPY"): Int {
        return when (speciesId) {
            CHARMANDER -> when (state) {
                "DIRTY" -> R.drawable.charmender
                "WET" -> R.drawable.charmender
                else -> R.drawable.charmender
            }

            CHARMELEON -> R.drawable.charmeleon
            CHARIZARD -> R.drawable.charizard

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

            else -> {
                speciesId
            }
        }
    }

    val availableIds = listOf(BULBASAUR, CHARMANDER, PIKACHU)
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
    val dateCaught: String = "",
    val uniqueId: String = ""
)

data class StoredPokemonData(
    val speciesId: Int = 1,
    val name: String = "",
    val health: Float = 1f,
    val hygiene: Float = 1f,
    val food: Float = 1f,
    val currentXP: Float = 0f,
    val currentLevel: Int = 1
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


    private fun checkEvolution() {
        // 1. Evoluir para Charmeleon (Nível 3)
        if (activeSpeciesId == PokemonCatalog.CHARMANDER && currentLevel >= 3) {
            triggerEvolution(PokemonCatalog.CHARMELEON, "Charmeleon")
        }
        // 2. Evoluir para Charizard (Nível 5)
        else if (activeSpeciesId == PokemonCatalog.CHARMELEON && currentLevel >= 5) {
            triggerEvolution(PokemonCatalog.CHARIZARD, "Charizard")
        }
    }

    private fun triggerEvolution(newSpeciesId: Int, newSpeciesName: String) {
        // 1. Atualizar o Pokémon Ativo
        activeSpeciesId = newSpeciesId

        // Se o nome for o da espécie antiga (ex: chamava-se "Charmander"), atualiza para "Charmeleon"
        // Se o utilizador deu um apelido (ex: "Foguinho"), mantém o apelido.
        val oldName = when(newSpeciesId) {
            PokemonCatalog.CHARMELEON -> "Charmander"
            PokemonCatalog.CHARIZARD -> "Charmeleon"
            else -> ""
        }
        if (activePokemonName == oldName) {
            activePokemonName = newSpeciesName
        }

        // 2. Adicionar à Pokédex (Para desbloquear o ícone na grelha)
        // Verificamos se já temos este registo para não duplicar desnecessariamente
        val alreadyInPokedex = caughtPokemonList.any { it.pokemonId == newSpeciesId }

        if (!alreadyInPokedex) {
            val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

            // Usamos o MESMO uniqueId do Pokémon atual, pois é o mesmo bicho que evoluiu
            val currentUniqueId = activePokemonId ?: java.util.UUID.randomUUID().toString()

            val evoEntry = CaughtPokemon(
                uniqueId = currentUniqueId,
                pokemonId = newSpeciesId,
                name = newSpeciesName,
                rarity = "Evolved",
                xpReward = 0f,
                dateCaught = date
            )
            addToPokedex(evoEntry)
        }

        // 3. Gravar tudo
        saveUserData()
    }

    fun loadUserDataFromFirebase() {
        val ref = userRef() ?: return
        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                // --- Carregar dados simples ---
                activePokemonId = snap.child("activePokemonId").getValue(String::class.java)
                activePokemonName = snap.child("activePokemonName").getValue(String::class.java) ?: "PokePet"

                // Proteção contra erro de tipo (String vs Int) que corrigimos antes
                activeSpeciesId = try {
                    snap.child("activeSpeciesId").getValue(Int::class.java) ?: PokemonCatalog.BULBASAUR
                } catch (e: Exception) {
                    PokemonCatalog.BULBASAUR
                }

                health = snap.child("health").getValue(Float::class.java) ?: 0.7f
                hygiene = snap.child("hygiene").getValue(Float::class.java) ?: 0.7f
                food = snap.child("food").getValue(Float::class.java) ?: 0.7f
                coins = snap.child("coins").getValue(Int::class.java) ?: 200
                currentLevel = snap.child("currentLevel").getValue(Int::class.java) ?: 1
                currentXP = snap.child("currentXP").getValue(Float::class.java) ?: 0f

                // --- NOVO: CARREGAR A POKEDEX ---
                val pokedexSnap = snap.child("pokedex")
                // Limpar a lista atual para não duplicar se chamarmos esta função 2 vezes
                caughtPokemonList.clear()

                for (child in pokedexSnap.children) {
                    // O Firebase converte automaticamente o JSON para a classe CaughtPokemon
                    val pokemon = child.getValue(CaughtPokemon::class.java)
                    if (pokemon != null) {
                        caughtPokemonList.add(pokemon)
                    }
                }

                // --- NOVO: CARREGAR O INVENTÁRIO (Opcional, mas recomendado) ---
                val inventorySnap = snap.child("inventory")
                inventory.clear()
                for (child in inventorySnap.children) {
                    val item = child.getValue(InventoryItem::class.java)
                    if (item != null) {
                        inventory.add(item)
                    }
                }
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPokemonFromHatch(petName: String, speciesId: Int, onResult: (Boolean, String?) -> Unit) {
        val ref = userRef() ?: return onResult(false, "Not logged in")

        // 1. Gerar ID Único
        val newUniqueId = ref.child("stored_pokemons").push().key ?: "gen_${System.currentTimeMillis()}"

        // 2. Definir como Ativo
        activePokemonId = newUniqueId
        activePokemonName = petName.ifBlank { "PokePet" }
        activeSpeciesId = speciesId

        // 3. Reset Stats
        health = 1.0f; hygiene = 1.0f; food = 1.0f; currentXP = 0f; currentLevel = 1

        // 4. Adicionar à Pokédex
        val speciesName = when(speciesId) {
            PokemonCatalog.BULBASAUR -> "Bulbasaur"
            PokemonCatalog.CHARMANDER -> "Charmander"
            PokemonCatalog.PIKACHU -> "Pikachu"
            else -> "Unknown"
        }
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        val starterEntry = CaughtPokemon(
            uniqueId = newUniqueId,
            pokemonId = speciesId,
            name = speciesName,
            rarity = "Starter",
            dateCaught = date,
            xpReward = 0f
        )
        addToPokedex(starterEntry)

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

    var showLevelUpCelebration by mutableStateOf(false)

    fun gainXP(amount: Float) {
        currentXP += amount
        if (currentXP >= 1.0f) {
            // Subiu de nível!
            currentLevel++
            currentXP = 0f

            checkEvolution()

            saveUserData()
        } else {
            saveUserData()
        }
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

    /**
     * Troca o Pokémon ativo pelo novo selecionado na Pokédex.
     * 1. Guarda o estado do atual na lista de "owned".
     * 2. Carrega o estado do novo (ou cria um novo estado se for a primeira vez).
     */
    fun swapActivePokemon(newPokemon: CaughtPokemon, onComplete: () -> Unit) {
        val ref = userRef() ?: return

        // 1. Validar ID do Pokémon atual
        // Se for nulo (primeira vez), geramos um ID baseado na espécie atual
        val safeCurrentId = activePokemonId ?: "starter_${activeSpeciesId}_${System.currentTimeMillis()}"

        // 2. Preparar os dados para guardar
        val currentData = StoredPokemonData(
            speciesId = activeSpeciesId,
            name = activePokemonName,
            health = health,
            hygiene = hygiene,
            food = food,
            currentXP = currentXP,
            currentLevel = currentLevel
        )

        // 3. Guardar o Pokémon ANTIGO
        ref.child("stored_pokemons").child(safeCurrentId).setValue(currentData)
            .addOnSuccessListener {

                // 4. Carregar o NOVO Pokémon
                // Usamos um ID único consistente: ID_Nome
                val newStorageId = "${newPokemon.pokemonId}_${newPokemon.name}"

                ref.child("stored_pokemons").child(newStorageId).get()
                    .addOnSuccessListener { snapshot ->
                        try {
                            if (snapshot.exists()) {
                                // Se já existe guardado, carregamos os stats
                                val data = snapshot.getValue(StoredPokemonData::class.java)
                                if (data != null) {
                                    activeSpeciesId = data.speciesId
                                    activePokemonName = data.name
                                    health = data.health
                                    hygiene = data.hygiene
                                    food = data.food
                                    currentXP = data.currentXP
                                    currentLevel = data.currentLevel
                                }
                            } else {
                                // Se é novo, começa fresco
                                activeSpeciesId = newPokemon.pokemonId
                                activePokemonName = newPokemon.name
                                health = 0.8f
                                hygiene = 0.8f
                                food = 0.8f
                                currentXP = 0f
                                currentLevel = 1
                            }

                            // Define o novo ID ativo e guarda tudo
                            activePokemonId = newStorageId
                            saveUserData()
                            onComplete()

                        } catch (e: Exception) {
                            // Se der erro ao ler, forçamos um estado novo para não travar
                            android.util.Log.e("SwapError", "Erro ao trocar: ${e.message}")
                            activeSpeciesId = newPokemon.pokemonId
                            activePokemonName = newPokemon.name
                            activePokemonId = newStorageId
                            saveUserData()
                            onComplete()
                        }
                    }
            }
            .addOnFailureListener {
                android.util.Log.e("SwapError", "Falha ao gravar anterior: ${it.message}")
            }
    }

    private fun updateHealth() { if (food > 0.8f && hygiene > 0.8f) health = min(health + 0.1f, 1f) }
    fun clearLocalPokemon() { activePokemonId = null; activePokemonName = "PokePet"; inventory.clear(); caughtPokemonList.clear() }
    fun markPokedexTutorialAsSeen() { hasSeenPokedexTutorial = true; saveUserData() }
}