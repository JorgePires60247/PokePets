package com.example.pokepet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlin.math.roundToInt


// Define the steps of the washing process
private sealed class WashingStep {
    object Idle : WashingStep()           // Water is off, waiting to start
    object WaterOn : WashingStep()        // Water is on to get wet
    object ReadyToSoap : WashingStep()    // Water is off, ready for soap
    object Soaped : WashingStep()         // Pet is soapy, waiting for rinse
    object Rinsing : WashingStep()        // Pet is wet, waiting to be dried
    object Clean : WashingStep()          // All done!
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BathroomScreen(navController: NavController) {
    var step by remember { mutableStateOf<WashingStep>(WashingStep.Idle) }

    // State for soap
    var soapOffset by remember { mutableStateOf(Offset.Zero) }
    var soapVisible by remember { mutableStateOf(true) }
    var soapBounds by remember { mutableStateOf(Rect.Zero) }

    // State for pet
    var isSoapy by remember { mutableStateOf(false) }
    var isClean by remember { mutableStateOf(false) }
    var petBounds by remember { mutableStateOf(Rect.Zero) }

    // Shake detection logic
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    val shakeEventListener = remember {
        object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var last_x: Float = 0.0f
            private var last_y: Float = 0.0f
            private var last_z: Float = 0.0f
            private val SHAKE_THRESHOLD = 800

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val curTime = System.currentTimeMillis()
                    if (curTime - lastUpdate > 100) {
                        val diffTime = curTime - lastUpdate
                        lastUpdate = curTime

                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        val speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000

                        if (speed > SHAKE_THRESHOLD) {
                             if (step == WashingStep.Rinsing) {
                                step = WashingStep.Clean
                                isClean = true
                            }
                        }
                        last_x = x
                        last_y = y
                        last_z = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(accelerometer) {
        if (accelerometer == null) {
            onDispose {}
        } else {
            sensorManager.registerListener(shakeEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(shakeEventListener)
            }
        }
    }

    if (soapBounds.overlaps(petBounds)) {
        if (step == WashingStep.ReadyToSoap) {
            step = WashingStep.Soaped
            isSoapy = true
            soapVisible = false
        }
    }


    val instructionText = when (step) {
        WashingStep.Idle -> "Click the tap to turn on the water!"
        WashingStep.WaterOn -> "Click the tap again to turn it off."
        WashingStep.ReadyToSoap -> "Now drag the soap to your PokePet!"
        WashingStep.Soaped -> "Click the tap to rinse the soap!"
        WashingStep.Rinsing -> "Shake your phone to dry your PokePet!"
        WashingStep.Clean -> "All clean! Great job!"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bathroom") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = instructionText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val showerImage = when (step) {
                    WashingStep.WaterOn, WashingStep.Rinsing -> R.drawable.chuveirocagua
                    else -> R.drawable.chuveirosagua
                }

                Image(
                    painter = painterResource(id = showerImage),
                    contentDescription = "Shower",
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopCenter)
                        .offset(x = (-50).dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.tap),
                    contentDescription = "Water Tap",
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-40).dp, y = 40.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                step = when (step) {
                                    WashingStep.Idle -> WashingStep.WaterOn
                                    WashingStep.WaterOn -> WashingStep.ReadyToSoap
                                    WashingStep.Soaped -> {
                                        isSoapy = false // Rinse off soap
                                        WashingStep.Rinsing
                                    }
                                    else -> step
                                }
                            }
                        }
                )

                AsyncImage(
                    model = R.drawable.happy,
                    contentDescription = "PokePet",
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .offset(x = (-50).dp)
                        .onGloballyPositioned { petBounds = it.boundsInParent() }
                )

                if (isSoapy) {
                    Image(
                        painter = painterResource(id = R.drawable.clean_icon), // Bubbles icon
                        contentDescription = "Soap Bubbles",
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                            .offset(x = (-50).dp) // Centered on the pet
                    )
                }

                if (soapVisible) {
                    Image(
                        painter = painterResource(id = R.drawable.clean_page_icon), // Soap icon
                        contentDescription = "Soap",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset { IntOffset(soapOffset.x.roundToInt(), soapOffset.y.roundToInt()) }
                            .size(90.dp)
                            .onGloballyPositioned { soapBounds = it.boundsInParent() }
                            .offset(y = (-80).dp)
                            .pointerInput(step) {
                                if (step == WashingStep.ReadyToSoap) { // Logic updated to new step
                                    detectDragGestures {
                                        change, dragAmount ->
                                        change.consume()
                                        soapOffset += dragAmount
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BathroomScreenPreview() {
    BathroomScreen(navController = rememberNavController())
}
