package com.example.pokepet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// --- DEFINIÇÃO DAS BAGAS ---
sealed class BerryType(val id: Int, @DrawableRes val resId: Int, val label: String) {
    object Chesto : BerryType(1, R.drawable.chesto_berry, "Chesto")
    object Sitrus : BerryType(2, R.drawable.cheri_berry, "Sitrus")
    object Oran : BerryType(3, R.drawable.oran_berry, "Oran")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(navController: NavController, viewModel: PetViewModel) {
    val context = LocalContext.current
    var currentInstruction by remember { mutableStateOf("Drag the berries to the pot!") }
    val droppedBerries = remember { mutableStateSetOf<Int>() }

    // Estados do Processo
    var isMixed by remember { mutableStateOf(false) }
    var isSeasoned by remember { mutableStateOf(false) }
    var isCleaned by remember { mutableStateOf(false) }
    var isCooled by remember { mutableStateOf(false) } // NOVO ESTADO

    // Progressos
    var seasoningProgress by remember { mutableFloatStateOf(0f) }
    var cleaningProgress by remember { mutableFloatStateOf(0f) }
    var coolingProgress by remember { mutableFloatStateOf(0f) } // NOVO PROGRESSO

    var movingUp by remember { mutableStateOf(true) }
    var sparkleOffset by remember { mutableStateOf<Offset?>(null) }
    val foodImageRes = remember { mutableIntStateOf(R.drawable.cooking_pot) }

    // Permissões de Áudio
    var hasMicPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasMicPermission = isGranted
    }

    // --- POKEMON DINÂMICO NA COZINHA ---
    val pokemonImageRes = PokemonCatalog.getPokemonImage(viewModel.activeSpeciesId, "HAPPY")

    var potBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val berries = listOf(BerryType.Chesto, BerryType.Sitrus, BerryType.Oran)
    val allBerriesDropped by remember { derivedStateOf { droppedBerries.size == berries.size } }

    // 1. Efeito: Pedir permissão quando chegar à fase de soprar
    LaunchedEffect(isCleaned) {
        if (isCleaned && !isCooled && !hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 2. Lógica do Microfone (Soprar)
    LaunchedEffect(isCleaned, isCooled, hasMicPermission) {
        if (isCleaned && !isCooled && hasMicPermission) {
            withContext(Dispatchers.IO) {
                // Configuração básica do AudioRecord
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize)
                    val buffer = ShortArray(minBufSize)

                    try {
                        audioRecord.startRecording()
                        while (isActive && coolingProgress < 1f) {
                            val read = audioRecord.read(buffer, 0, minBufSize)
                            if (read > 0) {
                                // Calcular a amplitude média (volume)
                                var sum = 0.0
                                for (i in 0 until read) {
                                    sum += abs(buffer[i].toInt())
                                }
                                val amplitude = sum / read

                                // Se o volume for alto (soprar é barulhento), aumenta o progresso
                                // O valor 500 é um threshold ajustável. Soprar direto no mic gera valores altos.
                                if (amplitude > 500) {
                                    coolingProgress += 0.02f // Velocidade de arrefecimento
                                }
                            }
                            delay(50) // Pequeno delay para não bloquear
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        audioRecord.stop()
                        audioRecord.release()
                    }

                    // Se saiu do loop porque encheu
                    if (coolingProgress >= 1f) {
                        isCooled = true
                    }
                }
            }
        }
    }

    // Lógica da Barra de Tempero (Animação automática)
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

    // Atualização de instruções e imagens
    LaunchedEffect(allBerriesDropped, isMixed, isSeasoned, isCleaned, isCooled) {
        when {
            !allBerriesDropped -> {
                foodImageRes.intValue = R.drawable.cooking_pot
                currentInstruction = "Drag the berries down to the pot!"
            }
            allBerriesDropped && !isMixed -> {
                currentInstruction = "Shake the phone to mix everything!"
            }
            isMixed && !isSeasoned -> {
                foodImageRes.intValue = R.drawable.ic_curry
                currentInstruction = "Touch at the right time to spice things up!"
            }
            isSeasoned && !isCleaned -> {
                foodImageRes.intValue = R.drawable.ic_curry_dirty
                currentInstruction = "What a mess! Clean the edges of the plate!"
            }
            // NOVO ESTADO: ARREFECER
            isCleaned && !isCooled -> {
                foodImageRes.intValue = R.drawable.hot_curry
                currentInstruction = "It's too hot! Blow into the mic to cool it!"
            }
            isCooled -> {
                // Pode manter o hot_curry ou mudar para outra imagem se tiveres
                foodImageRes.intValue = R.drawable.hot_curry
                currentInstruction = "Bon Appétit! Click on ${viewModel.activePokemonName} to feed!"
            }
        }
    }

    if (allBerriesDropped && !isMixed) {
        ShakeDetector(onShakeDetected = { isMixed = true })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitchen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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

                // Barras de Progresso Condicionais
                if (isMixed && !isSeasoned) {
                    CookingProgressBar(progress = seasoningProgress, color = Color(0xFF4CAF50), hasTarget = true, label = "SPICE IT UP")
                } else if (isSeasoned && !isCleaned) {
                    CookingProgressBar(progress = cleaningProgress, color = Color(0xFF00BCD4), label = "CLEAN EDGES")
                } else if (isCleaned && !isCooled) {
                    // BARRA DE ARREFECIMENTO
                    CookingProgressBar(progress = coolingProgress, color = Color(0xFF2196F3), label = "COOLING DOWN")
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Mic", tint = Color.Gray)
                }

                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier.size(300.dp).onGloballyPositioned { potBounds = it.boundsInRoot() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = foodImageRes.intValue, label = "FoodAnim") { targetRes ->
                        Box(contentAlignment = Alignment.Center) {
                            if (targetRes == R.drawable.ic_curry_dirty) {
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
                                // Imagem normal
                                Image(painterResource(targetRes), null, Modifier.fillMaxSize())

                                // Efeito visual de Fumo/Vapor quando está quente e a ser arrefecido
                                if (isCleaned && !isCooled) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = (1f - coolingProgress) * 0.4f)) // Fica menos branco conforme arrefece
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    sparkleOffset?.let {
                        Box(Modifier.offset { IntOffset(it.x.roundToInt() - 20, it.y.roundToInt() - 20) }.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }

                    if (isMixed && !isSeasoned) {
                        Box(Modifier.fillMaxSize().clickable {
                            if (seasoningProgress in 0.4f..0.6f) isSeasoned = true
                        })
                    }
                }
            }

            // POKEMON DINÂMICO
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
                Image(
                    painter = painterResource(pokemonImageRes),
                    contentDescription = "Active Pokemon",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable(enabled = isCooled) { // Só pode comer se estiver ARREFECIDO (isCooled)
                            viewModel.feed()
                            navController.popBackStack()
                        }
                )
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

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
    var startPosition by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                if (offset == Offset.Zero) {
                    startPosition = coordinates.positionInRoot()
                }
            }
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val itemSizePx = with(density) { 50.dp.toPx() }
                        val centerOffset = Offset(itemSizePx / 2, itemSizePx / 2)
                        val absolutePosition = startPosition + offset + centerOffset

                        if (potBounds.contains(absolutePosition)) {
                            onDropSuccess()
                        } else {
                            offset = Offset.Zero
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                )
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