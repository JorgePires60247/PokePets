package com.example.pokepet

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.roundToInt
import kotlin.math.sqrt

// --- NOVOS MODELOS DE DADOS ---

enum class PokemonRarity(val chance: Int) {
    COMMON(70),    // 70% chance

    UNCOMMON(40),  // 40% chance
    RARE(20),      // 20% chance
    LEGENDARY(5)   // 5% chance
}

data class PokemonSpawnConfig(
    val pokemonIcon: Int,
    val rarity: PokemonRarity,
    val xpReward: Float
)

data class MapBallEvent(
    val x: Float,
    val y: Float,
    val pokemonData: PokemonSpawnConfig,
    val isIdentified: MutableState<Boolean> = mutableStateOf(false)
)

data class MapRegion(
    val id: String,
    val backgroundRes: Int,
    val spawnList: List<PokemonSpawnConfig>
)

object MapData {
    val regions = listOf(
        MapRegion(
            id = "forest",
            backgroundRes = R.drawable.ic_map1,
            spawnList = listOf(
                PokemonSpawnConfig(R.drawable.p_azurill, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_chinchou, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_psyduck, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_petilil, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_beedrill, PokemonRarity.RARE, 0.15f),
                PokemonSpawnConfig(R.drawable.p_ribombee, PokemonRarity.RARE, 0.15f),
                PokemonSpawnConfig(R.drawable.p_cramorant, PokemonRarity.RARE, 0.15f),
                PokemonSpawnConfig(R.drawable.p_dragonite, PokemonRarity.RARE, 0.40f),
                PokemonSpawnConfig(R.drawable.p_mew, PokemonRarity.LEGENDARY, 0.60f)
            )
        ),
        MapRegion(
            id = "village",
            backgroundRes = R.drawable.ic_map2,
            spawnList = listOf(
                PokemonSpawnConfig(R.drawable.p_azurill, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_bidoof, PokemonRarity.COMMON, 0.04f),
                PokemonSpawnConfig(R.drawable.p_ledian, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_magikarp, PokemonRarity.COMMON, 0.03f),
                PokemonSpawnConfig(R.drawable.p_chatot, PokemonRarity.RARE, 0.12f),
                PokemonSpawnConfig(R.drawable.p_jigglypuff, PokemonRarity.RARE, 0.12f),
                PokemonSpawnConfig(R.drawable.p_petilil, PokemonRarity.RARE, 0.10f),
                PokemonSpawnConfig(R.drawable.p_cramorant, PokemonRarity.RARE, 0.15f),
                PokemonSpawnConfig(R.drawable.p_riolu, PokemonRarity.RARE, 0.20f),
                PokemonSpawnConfig(R.drawable.p_starmie, PokemonRarity.RARE, 0.35f)
            )
        ),
        MapRegion(
            id = "woods",
            backgroundRes = R.drawable.ic_map3,
            spawnList = listOf(
                PokemonSpawnConfig(R.drawable.p_magikarp, PokemonRarity.COMMON, 0.03f),
                PokemonSpawnConfig(R.drawable.p_ledian, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_whismur, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_onix, PokemonRarity.RARE, 0.18f),
                PokemonSpawnConfig(R.drawable.p_absol, PokemonRarity.RARE, 0.25f),
                PokemonSpawnConfig(R.drawable.p_riolu, PokemonRarity.RARE, 0.20f),
                PokemonSpawnConfig(R.drawable.p_gardevoir, PokemonRarity.RARE, 0.30f),
                PokemonSpawnConfig(R.drawable.p_dragonite, PokemonRarity.RARE, 0.45f),
                PokemonSpawnConfig(R.drawable.p_garchomp, PokemonRarity.RARE, 0.50f)
            )
        ),
        MapRegion(
            id = "ocean",
            backgroundRes = R.drawable.ic_map4,
            spawnList = listOf(
                PokemonSpawnConfig(R.drawable.p_magikarp, PokemonRarity.COMMON, 0.03f),
                PokemonSpawnConfig(R.drawable.p_azurill, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_psyduck, PokemonRarity.COMMON, 0.06f),
                PokemonSpawnConfig(R.drawable.p_bidoof, PokemonRarity.COMMON, 0.04f),
                PokemonSpawnConfig(R.drawable.p_chatot, PokemonRarity.UNCOMMON, 0.08f),
                PokemonSpawnConfig(R.drawable.p_chinchou, PokemonRarity.RARE, 0.15f),
                PokemonSpawnConfig(R.drawable.p_octillery, PokemonRarity.RARE, 0.20f),
                PokemonSpawnConfig(R.drawable.p_cramorant, PokemonRarity.RARE, 0.15f),
                PokemonSpawnConfig(R.drawable.p_starmie, PokemonRarity.RARE, 0.35f)
            )
        ),
        MapRegion(
            id = "cave",
            backgroundRes = R.drawable.ic_map5,
            spawnList = listOf(
                PokemonSpawnConfig(R.drawable.p_whismur, PokemonRarity.COMMON, 0.05f),
                PokemonSpawnConfig(R.drawable.p_bidoof, PokemonRarity.COMMON, 0.04f),
                PokemonSpawnConfig(R.drawable.p_riolu, PokemonRarity.RARE, 0.20f),
                PokemonSpawnConfig(R.drawable.p_jigglypuff, PokemonRarity.RARE, 0.12f),
                PokemonSpawnConfig(R.drawable.p_onix, PokemonRarity.RARE, 0.18f),
                PokemonSpawnConfig(R.drawable.p_chandelure, PokemonRarity.RARE, 0.25f),
                PokemonSpawnConfig(R.drawable.p_absol, PokemonRarity.RARE, 0.25f),
                PokemonSpawnConfig(R.drawable.p_spiritomb, PokemonRarity.RARE, 0.30f),
                PokemonSpawnConfig(R.drawable.p_metagross, PokemonRarity.RARE, 0.50f),
                PokemonSpawnConfig(R.drawable.p_garchomp, PokemonRarity.RARE, 0.55f)
            )
        )
    )
}

// Função para escolher Pokémon baseado na raridade
fun selectPokemonByRarity(list: List<PokemonSpawnConfig>): PokemonSpawnConfig {
    val roll = (1..100).random()
    val targetRarity = when {
        roll <= PokemonRarity.LEGENDARY.chance -> PokemonRarity.LEGENDARY
        roll <= (PokemonRarity.LEGENDARY.chance + PokemonRarity.RARE.chance) -> PokemonRarity.RARE
        else -> PokemonRarity.COMMON
    }

    // Tenta encontrar um pokemon dessa raridade, senão escolhe um qualquer da lista
    return list.filter { it.rarity == targetRarity }.randomOrNull() ?: list.random()
}

@Composable
fun MapScreen(navController: NavController, viewModel: PetViewModel, regionId: String = "forest") {

    val currentRegion = remember(regionId) {
        MapData.regions.find { it.id == regionId } ?: MapData.regions.first()
    }

    // Gerar eventos com lógica de raridade
    val mapEvents = remember(regionId) {
        generateRandomBallPositions(count = 5, minDistance = 0.12f).map { pos ->
            MapBallEvent(pos.x, pos.y, selectPokemonByRarity(currentRegion.spawnList))
        }
    }

    // Estados
    var activeEvent by remember { mutableStateOf<MapBallEvent?>(null) }
    var showIdentifierDialog by remember { mutableStateOf(false) }
    var showCaptureDialog by remember { mutableStateOf(false) }
    var pendingItem by remember { mutableStateOf<InventoryItem?>(null) }
    val shadowPositions = remember { mutableStateMapOf<MapBallEvent, Offset>() }

    val identifiersList = viewModel.inventory.filter { it.type == ItemType.IDENTIFIER }
    val hasPokeballs = viewModel.inventory.any {
        it.type == ItemType.POKEBALL || it.type == ItemType.ULTRABALL || it.type == ItemType.MASTERBALL
    }

    // --- DIÁLOGOS ---
    if (showIdentifierDialog && activeEvent != null && pendingItem != null) {
        AlertDialog(
            onDismissRequest = { showIdentifierDialog = false },
            title = { Text("Use Identifier?") },
            text = { Text("Reveal this ${activeEvent?.pokemonData?.rarity} Pokémon?") },
            confirmButton = {
                Button(onClick = {
                    activeEvent?.isIdentified?.value = true
                    viewModel.inventory.remove(pendingItem!!)
                    showIdentifierDialog = false
                    pendingItem = null
                }) { Text("Reveal") }
            },
            dismissButton = { TextButton(onClick = { showIdentifierDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCaptureDialog && activeEvent != null) {
        AlertDialog(
            onDismissRequest = { showCaptureDialog = false },
            title = { Text(if (hasPokeballs) "Catch Pokémon?" else "No Pokeballs!") },
            text = {
                val rarityName = activeEvent?.pokemonData?.rarity?.name
                Text(if (hasPokeballs) "Try to catch this $rarityName Pokémon?"
                else "Buy more Pokeballs at the PokeCenter!")
            },
            confirmButton = {
                if (hasPokeballs) {
                    Button(onClick = {
                        showCaptureDialog = false
                        navController.navigate("catch/${activeEvent?.pokemonData?.pokemonIcon}/${activeEvent?.pokemonData?.xpReward}")                    }) { Text("Catch!") }
                } else {
                    Button(onClick = {
                        showCaptureDialog = false
                        navController.navigate("energy_screen")
                    }) { Text("Go to Shop") }
                }
            },
            dismissButton = { TextButton(onClick = { showCaptureDialog = false }) { Text("Close") } }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        Image(
            painter = painterResource(id = currentRegion.backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        mapEvents.forEach { event ->
            val isThisSelected = activeEvent == event
            val revealAnim by animateFloatAsState(
                targetValue = if (event.isIdentified.value) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = ""
            )

            Box(
                modifier = Modifier
                    .size(if (isThisSelected) 120.dp else 40.dp)
                    .offset(
                        x = maxWidth * event.x - (if (isThisSelected) 40.dp else 0.dp),
                        y = maxHeight * event.y - (if (isThisSelected) 40.dp else 0.dp)
                    )
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        shadowPositions[event] = Offset(pos.x + coords.size.width/2, pos.y + coords.size.height/2)
                    },
                contentAlignment = Alignment.Center
            ) {
                // Pokébola
                Image(
                    painter = painterResource(id = R.drawable.map_pball),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp).alpha(if (isThisSelected) 0f else 1f).clickable { activeEvent = event }
                )

                if (isThisSelected) {
                    Image(
                        painter = painterResource(id = event.pokemonData.pokemonIcon),
                        contentDescription = null,
                        modifier = Modifier.size(110.dp).scale(revealAnim).clickable { showCaptureDialog = true },
                        // Filtro preto se não identificado
                        colorFilter = if (!event.isIdentified.value) ColorFilter.tint(Color.Black) else null
                    )

                    // Pequena Badge de Raridade se identificado
                    if (event.isIdentified.value) {
                        Text(
                            text = event.pokemonData.rarity.name,
                            color = when(event.pokemonData.rarity) {
                                PokemonRarity.LEGENDARY -> Color.Yellow
                                PokemonRarity.RARE -> Color.Cyan
                                else -> Color.White
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopCenter).background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp)).padding(2.dp)
                        )
                    }
                }
            }
        }

        // UI de Identifiers (mantida do teu original)
        if (identifiersList.isNotEmpty()) {
            val identifierCount = identifiersList.size
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Identifiers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp)).padding(4.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.size(65.dp)) {
                    DraggableIdentifier(item = identifiersList.first(), onDropped = { finalPos ->
                        activeEvent?.let { event ->
                            val target = shadowPositions[event] ?: Offset.Zero
                            val dist = sqrt(((finalPos.x - target.x)*(finalPos.x - target.x) + (finalPos.y - target.y)*(finalPos.y - target.y)).toDouble())
                            if (!event.isIdentified.value && dist < 180.0) {
                                pendingItem = identifiersList.first()
                                showIdentifierDialog = true
                            }
                        }
                    })
                    Box(modifier = Modifier.align(Alignment.TopEnd).background(Color.Red, CircleShape).border(1.dp, Color.White, CircleShape).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("x$identifierCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.3f), CircleShape)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
@Composable
fun DraggableIdentifier(item: InventoryItem, onDropped: (Offset) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var globalPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier.size(55.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .onGloballyPositioned { globalPos = it.positionInRoot() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onDropped(globalPos); offsetX = 0f; offsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        Image(painterResource(item.icon), null, Modifier.fillMaxSize())
    }
}

fun generateRandomBallPositions(count: Int, minDistance: Float): List<Offset> {
    val list = mutableListOf<Offset>()
    val random = kotlin.random.Random(System.currentTimeMillis())
    var attempts = 0
    while (list.size < count && attempts < 200) {
        val newX = random.nextFloat() * 0.7f + 0.15f
        val newY = random.nextFloat() * 0.5f + 0.2f
        if (list.none { sqrt(((it.x - newX)*(it.x - newX) + (it.y - newY)*(it.y - newY)).toDouble()) < minDistance }) {
            list.add(Offset(newX, newY))
        }
        attempts++
    }
    return list
}