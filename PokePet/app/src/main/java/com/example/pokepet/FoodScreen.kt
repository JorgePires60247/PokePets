package com.example.pokepet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Definição dos tipos de bagas
sealed class BerryType(val id: Int, @DrawableRes val resId: Int, val label: String) {
    data object Chesto : BerryType(1, R.drawable.chesto_berry, "Chesto Berry")
    data object Sitrus : BerryType(2, R.drawable.cheri_berry, "Sitrus Berry")
    data object Oran : BerryType(3, R.drawable.oran_berry, "Oran Berry")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    navController: NavController,
    viewModel: PetViewModel // Integrado para atualizar XP e Fome
) {
    var currentInstruction by remember { mutableStateOf("Arrasta os ingredientes para a panela!") }
    val droppedBerries = remember { mutableStateSetOf<Int>() }
    var isMixed by remember { mutableStateOf(false) }
    var isCooled by remember { mutableStateOf(false) }
    val foodImageRes = remember { mutableStateOf(R.drawable.cooking_pot) }
    var potBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    val berries = listOf(BerryType.Chesto, BerryType.Sitrus, BerryType.Oran)
    val allBerriesDropped by remember { derivedStateOf { droppedBerries.size == berries.size } }

    // Gestão de estados e instruções
    LaunchedEffect(allBerriesDropped, isMixed, isCooled) {
        if (allBerriesDropped && !isMixed) {
            currentInstruction = "Abana o telemóvel para misturar!"
        } else if (allBerriesDropped && isMixed && !isCooled) {
            foodImageRes.value = R.drawable.hot_curry
            currentInstruction = "Está quente! Sopra para o microfone para arrefecer."
        } else if (allBerriesDropped && isMixed && isCooled) {
            foodImageRes.value = R.drawable.curry_bowl
            currentInstruction = "Pronto! Clica no Pikachu para o alimentar."
        }
    }

    // Detectores de Sensores
    if (allBerriesDropped && !isMixed) {
        ShakeDetector(onShakeDetected = { isMixed = true })
    }

    if (allBerriesDropped && isMixed && !isCooled) {
        BlowDetector(onBlowDetected = {
            isCooled = true
            viewModel.feed() // Atualiza progresso no ViewModel
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cooking Lounge") },
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
                Text(text = "Ingredients", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

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

                Spacer(modifier = Modifier.height(28.dp))
                Text(text = currentInstruction, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(76.dp))

                // Transformação animada do prato
                AnimatedContent(
                    targetState = foodImageRes.value,
                    transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
                    label = "FoodTransform"
                ) { targetImageRes ->
                    Image(
                        painter = painterResource(id = targetImageRes),
                        contentDescription = "Food",
                        modifier = Modifier
                            .size(320.dp)
                            .zIndex(1f)
                            .onGloballyPositioned { potBounds = it.boundsInRoot() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Image(
                painter = painterResource(id = R.drawable.pikachu_happy),
                contentDescription = "Pikachu",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(140.dp)
                    .clickable(enabled = isCooled) { navController.popBackStack() }
            )
        }
    }
}

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
        Image(painter = painterResource(id = berry.resId), contentDescription = berry.label, modifier = Modifier.size(48.dp))
        Text(text = berry.label, fontSize = 12.sp)
    }
}

@Composable
fun ShakeDetector(onShakeDetected: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val gForce = sqrt(it.values[0] * it.values[0] + it.values[1] * it.values[1] + it.values[2] * it.values[2]) / SensorManager.GRAVITY_EARTH
                    if (gForce > 2.7f) onShakeDetected()
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

@Composable
fun BlowDetector(onBlowDetected: () -> Unit) {
    val sampleRate = 8000
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    var audioRecord: AudioRecord? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize).apply {
            if (state == AudioRecord.STATE_INITIALIZED) startRecording()
        }
        onDispose { audioRecord?.stop(); audioRecord?.release() }
    }

    LaunchedEffect(Unit) {
        val buffer = ShortArray(bufferSize)
        withContext(Dispatchers.IO) {
            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    var maxAmp = 0.0
                    for (i in 0 until read) maxAmp = max(maxAmp, buffer[i].toDouble())
                    if (20 * log10(maxAmp / 0.1) > 15.0) withContext(Dispatchers.Main) { onBlowDetected() }
                }
                delay(100)
            }
        }
    }
}