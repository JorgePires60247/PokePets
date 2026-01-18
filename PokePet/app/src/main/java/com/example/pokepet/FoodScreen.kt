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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt

sealed class BerryType(val id: Int, @DrawableRes val resId: Int, val label: String) {
    object Chesto : BerryType(1, R.drawable.chesto_berry, "Chesto")
    object Sitrus : BerryType(2, R.drawable.cheri_berry, "Sitrus")
    object Oran : BerryType(3, R.drawable.oran_berry, "Oran")
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(navController: NavController, viewModel: PetViewModel) {
    var currentInstruction by remember { mutableStateOf("Arrasta as bagas para a panela!") }
    val droppedBerries = remember { mutableStateSetOf<Int>() }
    var isMixed by remember { mutableStateOf(false) }
    var isSeasoned by remember { mutableStateOf(false) }
    var isCleaned by remember { mutableStateOf(false) }

    var seasoningProgress by remember { mutableFloatStateOf(0f) }
    var cleaningProgress by remember { mutableFloatStateOf(0f) }
    var movingUp by remember { mutableStateOf(true) }
    var sparkleOffset by remember { mutableStateOf<Offset?>(null) }

    val foodImageRes = remember { mutableIntStateOf(R.drawable.cooking_pot) }
    // Estado para a imagem do Pikachu
    val pikachuImageRes = remember { mutableIntStateOf(R.drawable.pikachu_happy) }

    var potBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val berries = listOf(BerryType.Chesto, BerryType.Sitrus, BerryType.Oran)
    val allBerriesDropped by remember { derivedStateOf { droppedBerries.size == berries.size } }

    // 1. Lógica da Barra de Tempero
    LaunchedEffect(isMixed, isSeasoned) {
        if (isMixed && !isSeasoned) {
            while (true) {
                if (movingUp) {
                    seasoningProgress += 0.035f
                    if (seasoningProgress >= 1f) movingUp = false
                } else {
                    seasoningProgress -= 0.035f
                    if (seasoningProgress <= 0f) movingUp = true
                }
                delay(20)
            }
        }
    }

    LaunchedEffect(allBerriesDropped, isMixed, isSeasoned, isCleaned) {
        when {
            !allBerriesDropped -> {
                foodImageRes.intValue = R.drawable.cooking_pot
                pikachuImageRes.intValue = R.drawable.pikachu_happy
                currentInstruction = "Drag the berries down to the pot!"
            }
            allBerriesDropped && !isMixed -> {
                currentInstruction = "Shake the phone to mix everything!"
            }
            isMixed && !isSeasoned -> {
                // ETAPA: TEMPERO (Prato aparece limpo)
                foodImageRes.intValue = R.drawable.ic_curry
                pikachuImageRes.intValue = R.drawable.pikachu_happy
                currentInstruction = "Touch at the right time to spice things up!"
            }
            isSeasoned && !isCleaned -> {
                // ETAPA: LIMPEZA
                foodImageRes.intValue = R.drawable.ic_curry_dirty
                currentInstruction = "What a mess! Clean the edges of the plate with your finger!"
            }
            isCleaned -> {
                foodImageRes.intValue = R.drawable.hot_curry
                pikachuImageRes.intValue = R.drawable.pikachu_happy
                currentInstruction = "All done! Click on your PokePet to feed it!"
            }
        }
    }

    if (allBerriesDropped && !isMixed) {
        ShakeDetector(onShakeDetected = { isMixed = true })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kitchen") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Área das Bagas
                if (!isMixed) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        berries.forEach { berry ->
                            DraggableBerryItem(berry, berry.id in droppedBerries, potBounds) {
                                droppedBerries.add(berry.id)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(currentInstruction, fontSize = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                // Barras de Progresso
                if (isMixed && !isSeasoned) {
                    CookingProgressBar(progress = seasoningProgress, color = Color(0xFF4CAF50), hasTarget = true, label = "SPICE IT UP")
                } else if (isSeasoned && !isCleaned) {
                    CookingProgressBar(progress = cleaningProgress, color = Color(0xFF00BCD4), label = "CLEAN EDGES")
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ÁREA CENTRAL DO PRATO/PANELA
                Box(
                    modifier = Modifier.size(300.dp).onGloballyPositioned { potBounds = it.boundsInRoot() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = foodImageRes.intValue, label = "FoodAnim") { targetRes ->
                        Box(contentAlignment = Alignment.Center) {
                            if (targetRes == R.drawable.ic_curry_dirty) {
                                // Lógica de sobreposição para limpeza
                                Image(painterResource(R.drawable.ic_curry), null, Modifier.fillMaxSize())
                                Image(
                                    painter = painterResource(R.drawable.ic_curry_dirty),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(1f - cleaningProgress)
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragEnd = { sparkleOffset = null },
                                                onDragCancel = { sparkleOffset = null }
                                            ) { change, _ ->
                                                change.consume()
                                                sparkleOffset = change.position
                                                if (cleaningProgress < 1f) cleaningProgress += 0.008f else isCleaned = true
                                            }
                                        }
                                )
                            } else {
                                Image(painterResource(targetRes), null, Modifier.fillMaxSize())
                            }
                        }
                    }

                    // Efeito Sparkle (Brilho no dedo)
                    sparkleOffset?.let {
                        Box(Modifier.offset { IntOffset(it.x.roundToInt() - 20, it.y.roundToInt() - 20) }.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }

                    // Botão invisível para o Mini-jogo do Tempero
                    if (isMixed && !isSeasoned) {
                        Box(Modifier.fillMaxSize().clickable {
                            if (seasoningProgress in 0.4f..0.6f) isSeasoned = true
                        })
                    }
                }
            }

            // PIKACHU ANIMADO NO FUNDO
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
                AnimatedContent(targetState = pikachuImageRes.intValue, label = "PikachuAnim") { targetPika ->
                    Image(
                        painter = painterResource(targetPika),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .clickable(enabled = isCleaned) {
                                viewModel.feed()
                                navController.popBackStack()
                            }
                    )
                }
            }
        }
    }
}
@Composable
fun CookingProgressBar(progress: Float, color: Color, label: String, hasTarget: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(240.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEEEEEE))) {
            if (hasTarget) Box(Modifier.fillMaxHeight().width(48.dp).align(Alignment.Center).background(color.copy(alpha = 0.25f)))
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(color).clip(RoundedCornerShape(14.dp)))
        }
    }
}

@Composable
fun DraggableBerryItem(berry: BerryType, isDropped: Boolean, potBounds: androidx.compose.ui.geometry.Rect, onDropSuccess: () -> Unit) {
    if (isDropped) return
    var offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    Column(
        modifier = Modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = {
                    val itemSizePx = with(density) { 50.dp.toPx() }
                    if (potBounds.contains(offset + Offset(itemSizePx/2, itemSizePx/2))) onDropSuccess() else offset = Offset.Zero
                }, onDrag = { change, dragAmount -> change.consume(); offset += dragAmount })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(berry.resId), null, Modifier.size(50.dp))
        Text(berry.label, fontSize = 12.sp)
    }
}

@Composable
fun ShakeDetector(onShakeDetected: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val g = sqrt(it.values[0]*it.values[0] + it.values[1]*it.values[1] + it.values[2]*it.values[2]) / SensorManager.GRAVITY_EARTH
                if (g > 2.6f) onShakeDetected()
            }
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    }
    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }
}