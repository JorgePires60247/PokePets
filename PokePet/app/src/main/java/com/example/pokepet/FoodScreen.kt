package com.example.pokepet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt

sealed class BerryType(
    val id: Int,
    @DrawableRes val resId: Int,
    val label: String
) {
    object Chesto : BerryType(1, R.drawable.chesto_berry, "Chesto Berry")
    object Sitrus : BerryType(2, R.drawable.cheri_berry, "Sitrus Berry")
    object Oran : BerryType(3, R.drawable.oran_berry, "Oran Berry")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(navController: NavController, viewModel: PetViewModel) {
    // ESTADOS DE JOGO
    var currentInstruction by remember { mutableStateOf("Arrasta os ingredientes para a panela!") }
    val droppedBerries = remember { mutableStateSetOf<Int>() }
    var isMixed by remember { mutableStateOf(false) }      // Shake concluído
    var isStirred by remember { mutableStateOf(false) }   // Gesto de mexer concluído
    var isSeasoned by remember { mutableStateOf(false) }  // Toque rítmico concluído

    // ESTADO DO GESTO DE MEXER
    var stirDistance by remember { mutableFloatStateOf(0f) }
    val requiredStirDistance = 2000f // Quantidade de movimento necessária

    // ESTADOS DA BARRA DE TEMPERO
    var seasoningProgress by remember { mutableFloatStateOf(0f) }
    var movingUp by remember { mutableStateOf(true) }

    // ESTADOS VISUAIS
    val foodImageRes = remember { mutableIntStateOf(R.drawable.cooking_pot) }
    var potBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val berries = listOf(BerryType.Chesto, BerryType.Sitrus, BerryType.Oran)
    val allBerriesDropped by remember { derivedStateOf { droppedBerries.size == berries.size } }

    // 1. Animação da Barra de Tempero (Só ativa após mexer)
    LaunchedEffect(isStirred, isSeasoned) {
        if (isStirred && !isSeasoned) {
            while (true) {
                if (movingUp) {
                    seasoningProgress += 0.04f
                    if (seasoningProgress >= 1f) movingUp = false
                } else {
                    seasoningProgress -= 0.04f
                    if (seasoningProgress <= 0f) movingUp = true
                }
                delay(20)
            }
        }
    }

    // 2. Gestão de Fluxo, Instruções e Imagens
    LaunchedEffect(allBerriesDropped, isMixed, isStirred, isSeasoned) {
        when {
            !allBerriesDropped -> {
                currentInstruction = "Drag the berries down to the pot!"
                foodImageRes.intValue = R.drawable.cooking_pot
            }
            allBerriesDropped && !isMixed -> {
                currentInstruction = "Shake the phone to mix them all!"
            }
            isMixed && !isStirred -> {
                foodImageRes.intValue = R.drawable.hot_curry
                currentInstruction = "Mix the curry with your finger to cool it down!"
            }
            isStirred && !isSeasoned -> {
                currentInstruction = "Time it just right to finish!"
            }
            isSeasoned -> {
                foodImageRes.intValue = R.drawable.curry_bowl
                currentInstruction = "All done! Click on the PokePet to feed it."
            }
        }
    }

    // 3. Detector de Shake
    if (allBerriesDropped && !isMixed) {
        ShakeDetector(onShakeDetected = { isMixed = true })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cozinha") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Zona de Ingredientes
                if (!isMixed) {
                    Text("Ingredientes", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        berries.forEach { berry ->
                            DraggableBerryItem(
                                berry = berry,
                                isDropped = berry.id in droppedBerries,
                                potBounds = potBounds,
                                onDropSuccess = { droppedBerries.add(berry.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(text = currentInstruction, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))

                // BARRA VISUAL DE PROGRESSO (MEXER OU TEMPERAR)
                Spacer(modifier = Modifier.height(20.dp))
                if (isMixed && !isStirred) {
                    // Barra de progresso do gesto de mexer
                    LinearProgressIndicator(
                        progress = { stirDistance / requiredStirDistance },
                        modifier = Modifier.width(240.dp).height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFFFFA500)
                    )
                } else if (isStirred && !isSeasoned) {
                    // Mini-jogo do Tempero
                    Box(modifier = Modifier.width(240.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.LightGray)) {
                        Box(modifier = Modifier.fillMaxHeight().width(60.dp).align(Alignment.Center).background(Color(0xFF4CAF50)))
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(seasoningProgress).background(Color.Black.copy(alpha = 0.6f)))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // IMAGEM CENTRAL INTERATIVA
                AnimatedContent(
                    targetState = foodImageRes.intValue,
                    transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
                    label = "FoodAnim"
                ) { targetRes ->
                    Image(
                        painter = painterResource(id = targetRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(300.dp)
                            .zIndex(1f)
                            .onGloballyPositioned { potBounds = it.boundsInRoot() }
                            .pointerInput(isMixed, isStirred) {
                                // DETETOR DE GESTO DE MEXER
                                if (isMixed && !isStirred) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        // Somamos o movimento (x e y) à distância total
                                        stirDistance += (Math.abs(dragAmount.x) + Math.abs(dragAmount.y))
                                        if (stirDistance >= requiredStirDistance) {
                                            isStirred = true
                                        }
                                    }
                                }
                            }
                            .clickable(enabled = isStirred && !isSeasoned) {
                                // DETETOR DE TOQUE NO TEMPERO
                                if (seasoningProgress in 0.38f..0.62f) {
                                    isSeasoned = true
                                    viewModel.feed() // RECOMPENSA DE XP
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // PIKACHU FINAL
            Image(
                painter = painterResource(id = R.drawable.pikachu_happy),
                contentDescription = "Pikachu",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .size(150.dp)
                    .clickable(enabled = isSeasoned) {
                        navController.popBackStack()
                    }
            )
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun DraggableBerryItem(
    berry: BerryType,
    isDropped: Boolean,
    potBounds: androidx.compose.ui.geometry.Rect,
    onDropSuccess: () -> Unit
) {
    if (isDropped) return
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .zIndex(if (isDragging) 2f else 0f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        val itemSizePx = with(density) { 48.dp.toPx() }
                        val dropPoint = offset + Offset(itemSizePx / 2, itemSizePx / 2)
                        if (potBounds.contains(dropPoint)) onDropSuccess() else offset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                )
            }
    ) {
        Image(painter = painterResource(id = berry.resId), contentDescription = null, modifier = Modifier.size(48.dp))
        Text(berry.label, fontSize = 11.sp)
    }
}

@Composable
fun ShakeDetector(onShakeDetected: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val g = sqrt(it.values[0]*it.values[0] + it.values[1]*it.values[1] + it.values[2]*it.values[2]) / SensorManager.GRAVITY_EARTH
                    if (g > 2.5f) onShakeDetected()
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
    }

    DisposableEffect(Unit) {
        accelerometer?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }
}