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
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Define the names for the berries for easier tracking
sealed class BerryType(val id: Int, @DrawableRes val resId: Int, val label: String) {
    data object Chesto : BerryType(1, R.drawable.chesto_berry, "Chesto Berry")
    data object Sitrus : BerryType(2, R.drawable.cheri_berry, "Sitrus Berry")
    data object Oran : BerryType(3, R.drawable.oran_berry, "Oran Berry")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(navController: NavController) {
    // State for UI text instruction
    var currentInstruction by remember { mutableStateOf("Drag the ingredients to the pot!") }

    // State for tracking dropped ingredients
    val droppedBerries = remember { mutableStateSetOf<Int>() }

    // State for tracking the mixing stage (Shake)
    var isMixed by remember { mutableStateOf(false) }

    // State for tracking the cooling stage (Blow)
    var isCooled by remember { mutableStateOf(false) }

    // State for the food image drawable ID (Pot, Hot Curry, or Cooled Curry Bowl)
    val foodImageRes = remember {
        mutableStateOf(R.drawable.cooking_pot)
    }

    // State for the pot's screen boundaries (used for drag-and-drop hit detection)
    var potBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    // Constants for the berry list
    val berries = listOf(
        BerryType.Chesto,
        BerryType.Sitrus,
        BerryType.Oran
    )

    // Derived state to check if all berries have been dropped
    val allBerriesDropped by remember {
        derivedStateOf { droppedBerries.size == berries.size }
    }

    // Effect to update instructions and trigger the transformations
    LaunchedEffect(allBerriesDropped, isMixed, isCooled) {
        if (allBerriesDropped && !isMixed) {
            currentInstruction = "Shake the phone to mix the ingredients"
        } else if (allBerriesDropped && isMixed && !isCooled) {
            // State 2: Mixing done, now it's hot and needs cooling
            foodImageRes.value = R.drawable.hot_curry
            currentInstruction = "The curry is hot! Blow into the mic to cool it down."
        } else if (allBerriesDropped && isMixed && isCooled) {
            // State 3: Cooling done, ready to eat
            foodImageRes.value = R.drawable.curry_bowl
            currentInstruction = "Food is ready! Tap Pikachu to feed."
        }
    }

    // Conditionally register the shake detector ONLY when mixing is required
    if (allBerriesDropped && !isMixed) {
        ShakeDetector(
            onShakeDetected = {
                isMixed = true
            }
        )
    }

    // Conditionally register the blow detector ONLY when cooling is required
    if (allBerriesDropped && isMixed && !isCooled) {
        BlowDetector(
            onBlowDetected = {
                isCooled = true
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cooking Lounge") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("PetMainScreen")
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Ingredients title
                Text(
                    text = "Ingredients",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, bottom = 12.dp)
                )

                // Ingredients Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    berries.forEach { berry ->
                        DraggableBerryItem(
                            berry = berry,
                            isDropped = berry.id in droppedBerries,
                            potBounds = potBounds,
                            onDropSuccess = {
                                droppedBerries.add(berry.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Instruction Text
                Text(
                    text = currentInstruction,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(76.dp))

                // ANIMATION WRAPPER: AnimatedContent handles the transition
                AnimatedContent(
                    targetState = foodImageRes.value,
                    transitionSpec = {
                        // Fade in and scale in the new content, while fading out and scaling out the old content
                        (fadeIn() + scaleIn(initialScale = 0.8f))
                            .togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
                    },
                    label = "Food Image Transition"
                ) { targetImageRes ->
                    // Pot / Curry Plate Image
                    Image(
                        painter = painterResource(id = targetImageRes),
                        contentDescription = when (targetImageRes) {
                            R.drawable.cooking_pot -> "Cooking Pot"
                            R.drawable.hot_curry -> "Hot Curry"
                            R.drawable.curry_bowl -> "Cooled Curry Bowl"
                            else -> "Food"
                        },
                        modifier = Modifier
                            .size(320.dp)
                            .zIndex(1f)
                            .onGloballyPositioned { coordinates ->
                                potBounds = coordinates.boundsInRoot()
                            }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // Bigger Pikachu
            Image(
                painter = painterResource(id = R.drawable.pikachu_happy),
                contentDescription = "Happy Pikachu",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(140.dp)
            )
        }
    }
}


/**
 * Composable for a draggable berry item. (STAGE 1)
 */
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
            .offset {
                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            }
            .zIndex(if (isDragging) 2f else 0f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false

                        val itemSizePx = with(density) { 48.dp.toPx() }
                        val dropPoint = offset + Offset(itemSizePx / 2, itemSizePx / 2)

                        if (potBounds.contains(dropPoint)) {
                            onDropSuccess()
                        } else {
                            offset = Offset.Zero
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        offset = Offset.Zero
                    },
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        change.consume()
                        offset = offset + dragAmount
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(id = berry.resId),
            contentDescription = berry.label,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = berry.label, fontSize = 12.sp)
    }
}


/**
 * Hook to listen for shake events using the device's accelerometer. (STAGE 2)
 */
@Composable
fun ShakeDetector(onShakeDetected: () -> Unit) {
    val context = LocalContext.current

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val SHAKE_THRESHOLD_GRAVITY = 2.7f
    val SHAKE_SLOP_TIME_MS = 500L
    var lastShakeTime = remember { 0L }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    val currentTime = System.currentTimeMillis()
                    if ((currentTime - lastShakeTime) > SHAKE_SLOP_TIME_MS) {
                        lastShakeTime = currentTime
                        onShakeDetected()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }
    }

    DisposableEffect(sensorListener, accelerometer) {
        if (accelerometer != null) {
            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
}

/**
 * Hook to listen for a sudden spike in microphone amplitude ("blow" event). (STAGE 3)
 */
@Composable
fun BlowDetector(onBlowDetected: () -> Unit) {
    // Configuration for AudioRecord
    val sampleRate = 8000
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // Threshold (adjust based on device and testing)
    // Measures the amplitude change needed to register a "blow"
    val BLOW_THRESHOLD_DB = 0.05
    val SILENCE_DB = 0.1

    var audioRecord: AudioRecord? = remember { null }
    val isListening = remember { mutableStateOf(false) }

    // DisposableEffect manages the AudioRecord lifecycle
    DisposableEffect(Unit) @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        ).apply {
            try {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    startRecording()
                    isListening.value = true
                }
            } catch (e: SecurityException) {
                // This will catch if RECORD_AUDIO permission is missing
                println("ERROR: RECORD_AUDIO permission is missing.")
            }
        }

        onDispose {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            isListening.value = false
        }
    }

    // LaunchedEffect runs the continuous monitoring loop
    LaunchedEffect(isListening.value) {
        if (isListening.value && audioRecord != null) {
            val buffer = ShortArray(bufferSize)
            var lastAmplitude = 0.0

            withContext(Dispatchers.IO) {
                while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord!!.read(buffer, 0, bufferSize)

                    if (read > 0) {
                        var maxAmplitude = 0.0
                        for (i in 0 until read) {
                            maxAmplitude = max(maxAmplitude, buffer[i].toDouble())
                        }

                        // Convert max amplitude to decibels (dB)
                        val currentDb = if (maxAmplitude > 0) {
                            20 * log10(maxAmplitude / SILENCE_DB)
                        } else {
                            0.0
                        }

                        // Check for a sudden spike (a "blow")
                        if (currentDb - lastAmplitude > BLOW_THRESHOLD_DB) {
                            withContext(Dispatchers.Main) {
                                onBlowDetected()
                            }
                        }

                        lastAmplitude = currentDb
                    }
                    delay(100)
                }
            }
        }
    }
}