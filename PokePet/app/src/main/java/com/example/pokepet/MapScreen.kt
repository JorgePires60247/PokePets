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

// Modelo de dados
data class MapBallEvent(
    val x: Float,
    val y: Float,
    val pokemonIcon: Int,
    val isIdentified: MutableState<Boolean> = mutableStateOf(false)
)

@Composable
fun MapScreen(navController: NavController, viewModel: PetViewModel) {
    val pokemonIcons = remember {
        listOf(
            R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5,
            R.drawable.p6, R.drawable.p7, R.drawable.p8, R.drawable.p9, R.drawable.p10,
            R.drawable.p11, R.drawable.p12, R.drawable.p13, R.drawable.p14, R.drawable.p15
        )
    }

    val mapEvents = remember {
        generateRandomBallPositions(count = 6, minDistance = 0.2f).map { pos ->
            MapBallEvent(pos.x, pos.y, pokemonIcons.random())
        }
    }

    // Estados de UI e Diálogos
    var activeEvent by remember { mutableStateOf<MapBallEvent?>(null) }
    var showIdentifierDialog by remember { mutableStateOf(false) }
    var showCaptureDialog by remember { mutableStateOf(false) }
    var pendingItem by remember { mutableStateOf<InventoryItem?>(null) }

    val shadowPositions = remember { mutableStateMapOf<MapBallEvent, Offset>() }
    val identifiersList = viewModel.inventory.filter { it.type == ItemType.IDENTIFIER }
    val identifierCount = identifiersList.size

    // --- DIÁLOGO 1: IDENTIFICAR ---
    if (showIdentifierDialog && activeEvent != null && pendingItem != null) {
        AlertDialog(
            onDismissRequest = { showIdentifierDialog = false },
            title = { Text("Use Identifier?") },
            text = { Text("Do you want to reveal this Pokémon's identity?") },
            confirmButton = {
                Button(onClick = {
                    activeEvent?.isIdentified?.value = true
                    viewModel.inventory.remove(pendingItem!!)
                    showIdentifierDialog = false
                    pendingItem = null
                }) { Text("Reveal") }
            },
            dismissButton = {
                TextButton(onClick = { showIdentifierDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- DIÁLOGO 2: CAPTURAR ---
    if (showCaptureDialog && activeEvent != null) {
        AlertDialog(
            onDismissRequest = { showCaptureDialog = false },
            title = { Text("Catch Pokémon?") },
            text = { Text("Do you want to try and catch this Pokémon?") },
            confirmButton = {
                Button(onClick = {
                    showCaptureDialog = false
                    navController.navigate("catch/${activeEvent?.pokemonIcon}")
                }) { Text("Catch!") }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureDialog = false }) { Text("Maybe later") }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        Image(
            painter = painterResource(id = R.drawable.ic_map1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        mapEvents.forEach { event ->
            val isThisSelected = activeEvent == event

            val revealAnim by animateFloatAsState(
                targetValue = if (event.isIdentified.value) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "reveal"
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
                // Pokébola (Invisível quando ativa)
                Image(
                    painter = painterResource(id = R.drawable.map_pball),
                    contentDescription = null,
                    modifier = Modifier
                        .size(35.dp)
                        .alpha(if (isThisSelected) 0f else 1f)
                        .clickable { activeEvent = event }
                )

                // Pokémon (Sombra/Revelado)
                if (isThisSelected) {
                    Image(
                        painter = painterResource(id = event.pokemonIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(110.dp)
                            .scale(revealAnim)
                            .clickable {
                                // Se o utilizador clicar no Pokémon já aberto, surge o popup de captura
                                showCaptureDialog = true
                            },
                        colorFilter = if (!event.isIdentified.value) ColorFilter.tint(Color.Black) else null
                    )
                }
            }
        }

        // --- Dock de Identifiers (Canto Inferior) ---
        if (identifierCount > 0) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Identifiers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp)).padding(4.dp))

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.size(65.dp)) {
                    DraggableIdentifier(
                        item = identifiersList.first(),
                        onDropped = { finalPos ->
                            activeEvent?.let { event ->
                                val target = shadowPositions[event] ?: Offset.Zero
                                val dist = sqrt(((finalPos.x - target.x)*(finalPos.x - target.x) + (finalPos.y - target.y)*(finalPos.y - target.y)).toDouble())
                                if (!event.isIdentified.value && dist < 180.0) {
                                    pendingItem = identifiersList.first()
                                    showIdentifierDialog = true
                                }
                            }
                        }
                    )
                    // Badge contador
                    Box(modifier = Modifier.align(Alignment.TopEnd).background(Color.Red, CircleShape)
                        .border(1.dp, Color.White, CircleShape).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("x$identifierCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        IconButton(onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.3f), CircleShape)) {
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