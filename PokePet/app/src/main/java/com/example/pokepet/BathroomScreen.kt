package com.example.pokepet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private sealed class WashingStep {
    object Idle : WashingStep()
    object WaterOn : WashingStep()
    object ReadyToSoap : WashingStep()
    object Soaped : WashingStep()
    object Rinsing : WashingStep()
    object ReadyToDry : WashingStep()
    object Clean : WashingStep()
}

data class Bubble(val id: Int, val offset: Offset)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BathroomScreen(navController: NavController, viewModel: PetViewModel) {
    var step by remember { mutableStateOf<WashingStep>(WashingStep.Idle) }
    val scope = rememberCoroutineScope()

    // Estados do Sabão e Esfregar
    var soapOffset by remember { mutableStateOf(Offset.Zero) }
    var soapBounds by remember { mutableStateOf(Rect.Zero) }
    var soapVisible by remember { mutableStateOf(true) }
    val bubbles = remember { mutableStateListOf<Bubble>() }
    var scrubbingProgress by remember { mutableFloatStateOf(0f) }
    val targetScrubbing = 5000f // Valor total de movimento necessário

    // Estados do Pet
    var petBounds by remember { mutableStateOf(Rect.Zero) }

    val pikachuImage = when (step) {
        WashingStep.Idle -> R.drawable.ic_dirty_pikachu
        WashingStep.WaterOn, WashingStep.ReadyToSoap, WashingStep.Soaped, WashingStep.ReadyToDry -> R.drawable.pikachu_wet
        WashingStep.Rinsing -> R.drawable.pikachu_wet
        WashingStep.Clean -> R.drawable.pikachu_happy
    }

    // Sensor Shake (Secar)
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val shakeEventListener = remember {
        object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lx = 0f; private var ly = 0f; private var lz = 0f
            override fun onSensorChanged(event: SensorEvent) {
                val t = System.currentTimeMillis()
                if (t - lastUpdate > 100) {
                    val s = abs(event.values[0] + event.values[1] + event.values[2] - lx - ly - lz) / (t - lastUpdate) * 10000
                    if (s > 800 && step == WashingStep.ReadyToDry) {
                        scope.launch { if (step != WashingStep.Clean) { step = WashingStep.Clean; viewModel.clean() } }
                    }
                    lastUpdate = t; lx = event.values[0]; ly = event.values[1]; lz = event.values[2]
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
    }

    DisposableEffect(accelerometer) {
        sensorManager.registerListener(shakeEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(shakeEventListener) }
    }

    val instructionText = when (step) {
        WashingStep.Idle -> "Liga a torneira!"
        WashingStep.WaterOn -> "Desliga a água para ensaboar."
        WashingStep.ReadyToSoap -> "Esfregar o sabão no Pikachu!"
        WashingStep.Soaped -> "Liga a água para tirar o sabão!"
        WashingStep.Rinsing -> "Desliga a água antes de secar!"
        WashingStep.ReadyToDry -> "Abana o telemóvel para secar!"
        WashingStep.Clean -> "Limpinho!"
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Banho") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(instructionText, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Barra de progresso de "Esfregar"
            if (step == WashingStep.ReadyToSoap && scrubbingProgress > 0) {
                LinearProgressIndicator(
                    progress = scrubbingProgress / targetScrubbing,
                    modifier = Modifier.padding(top = 8.dp).width(200.dp).clip(RoundedCornerShape(10.dp)),
                    color = Color(0xFF03A9F4)
                )
            }

            if (step == WashingStep.Clean) {
                Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Chuveiro e Torneira
                val isWaterRunning = step == WashingStep.WaterOn || step == WashingStep.Rinsing
                Image(
                    painter = painterResource(if (isWaterRunning) R.drawable.chuveirocagua else R.drawable.chuveirosagua),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).align(Alignment.TopCenter).offset(x = (-50).dp)
                )

                Image(
                    painter = painterResource(R.drawable.tap),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(x = (-40).dp, y = 40.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                step = when (step) {
                                    WashingStep.Idle -> WashingStep.WaterOn
                                    WashingStep.WaterOn -> WashingStep.ReadyToSoap
                                    WashingStep.Soaped -> { bubbles.clear(); WashingStep.Rinsing }
                                    WashingStep.Rinsing -> WashingStep.ReadyToDry
                                    else -> step
                                }
                            }
                        }
                )

                // Pikachu
                Image(
                    painter = painterResource(pikachuImage),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp).align(Alignment.Center).offset(x = (-50).dp)
                        .onGloballyPositioned { petBounds = it.boundsInParent() }
                )

                // Bolhas (Clean Icons)
                bubbles.forEach { bubble ->
                    Image(
                        painter = painterResource(R.drawable.clean_icon),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).align(Alignment.Center)
                            .offset(x = bubble.offset.x.dp - 50.dp, y = bubble.offset.y.dp)
                    )
                }

                // Sabão com Detecção de Movimento (Esfregar)
                if (soapVisible && step == WashingStep.ReadyToSoap) {
                    Image(
                        painter = painterResource(R.drawable.clean_page_icon),
                        contentDescription = "Soap",
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .offset { IntOffset(soapOffset.x.roundToInt(), soapOffset.y.roundToInt()) }
                            .size(90.dp)
                            .onGloballyPositioned { soapBounds = it.boundsInParent() }
                            .offset(y = (-80).dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    soapOffset += dragAmount

                                    // 🚀 Lógica de Esfregar:
                                    // Se o sabão estiver sobre o pet E o utilizador o estiver a mover
                                    if (soapBounds.overlaps(petBounds)) {
                                        val movement = abs(dragAmount.x) + abs(dragAmount.y)
                                        scrubbingProgress += movement

                                        // Cria bolhas proporcionalmente ao movimento
                                        if (Random.nextInt(10) < 2) {
                                            bubbles.add(Bubble(Random.nextInt(), Offset(Random.nextInt(-70, 70).toFloat(), Random.nextInt(-70, 70).toFloat())))
                                        }

                                        if (scrubbingProgress >= targetScrubbing) {
                                            step = WashingStep.Soaped
                                            soapVisible = false
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}