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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlin.math.roundToInt

private sealed class WashingStep {
    object Idle : WashingStep()
    object WaterOn : WashingStep()
    object ReadyToSoap : WashingStep()
    object Soaped : WashingStep()
    object Rinsing : WashingStep()
    object Clean : WashingStep()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BathroomScreen(
    navController: NavController,
    viewModel: PetViewModel // 🚀 Injeção do ViewModel
) {
    var step by remember { mutableStateOf<WashingStep>(WashingStep.Idle) }

    // Estado do sabão
    var soapOffset by remember { mutableStateOf(Offset.Zero) }
    var soapVisible by remember { mutableStateOf(true) }
    var soapBounds by remember { mutableStateOf(Rect.Zero) }

    // Estado do pet
    var isSoapy by remember { mutableStateOf(false) }
    var petBounds by remember { mutableStateOf(Rect.Zero) }

    // Lógica do Acelerómetro (Shake)
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    val shakeEventListener = remember {
        object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var last_x = 0.0f
            private var last_y = 0.0f
            private var last_z = 0.0f
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
                                // 🚀 AÇÃO FINALIZADA: Atualiza XP e Higiene
                                viewModel.clean()
                            }
                        }
                        last_x = x; last_y = y; last_z = z
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(accelerometer) {
        if (accelerometer != null) {
            sensorManager.registerListener(shakeEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(shakeEventListener) }
    }

    // Deteção de colisão (Sabão no Pet)
    if (soapBounds.overlaps(petBounds) && step == WashingStep.ReadyToSoap) {
        step = WashingStep.Soaped
        isSoapy = true
        soapVisible = false
    }

    val instructionText = when (step) {
        WashingStep.Idle -> "Click the tap to wash your PokePet!!"
        WashingStep.WaterOn -> "Click again to turn the water off."
        WashingStep.ReadyToSoap -> "Drag the soap to wash your PokePet!"
        WashingStep.Soaped -> "Turn on the tap to clean the soap off!"
        WashingStep.Rinsing -> "Shake your phone to dry your Pet!"
        WashingStep.Clean -> "All clean! Good Job!"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bathroom") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                textAlign = TextAlign.Center,
                color = if(step == WashingStep.Clean) Color(0xFF4CAF50) else Color.Unspecified
            )

            if (step == WashingStep.Clean) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Go back")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                val showerImage = if (step is WashingStep.WaterOn || step is WashingStep.Rinsing)
                    R.drawable.chuveirocagua else R.drawable.chuveirosagua

                Image(
                    painter = painterResource(id = showerImage),
                    contentDescription = "Shower",
                    modifier = Modifier.size(120.dp).align(Alignment.TopCenter).offset(x = (-50).dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.tap),
                    contentDescription = "Tap",
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-40).dp, y = 40.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                step = when (step) {
                                    WashingStep.Idle -> WashingStep.WaterOn
                                    WashingStep.WaterOn -> WashingStep.ReadyToSoap
                                    WashingStep.Soaped -> { isSoapy = false; WashingStep.Rinsing }
                                    else -> step
                                }
                            }
                        }
                )

                AsyncImage(
                    model = R.drawable.ic_dirty_pikachu,
                    contentDescription = "Pet",
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .offset(x = (-50).dp)
                        .onGloballyPositioned { petBounds = it.boundsInParent() }
                )

                if (isSoapy) {
                    Image(
                        painter = painterResource(id = R.drawable.clean_icon),
                        contentDescription = "Bubbles",
                        modifier = Modifier.size(200.dp).align(Alignment.Center).offset(x = (-50).dp)
                    )
                }

                if (soapVisible) {
                    Image(
                        painter = painterResource(id = R.drawable.clean_page_icon),
                        contentDescription = "Soap",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset { IntOffset(soapOffset.x.roundToInt(), soapOffset.y.roundToInt()) }
                            .size(90.dp)
                            .onGloballyPositioned { soapBounds = it.boundsInParent() }
                            .offset(y = (-80).dp)
                            .pointerInput(step) {
                                if (step == WashingStep.ReadyToSoap) {
                                    detectDragGestures { change, dragAmount ->
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