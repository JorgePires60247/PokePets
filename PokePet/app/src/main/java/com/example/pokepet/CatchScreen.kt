package com.example.pokepet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun CatchScreen(navController: NavController, viewModel: PetViewModel, pokemonResId: Int) {
    val scope = rememberCoroutineScope()
    val inventoryBalls = viewModel.inventory.filter {
        it.type == ItemType.POKEBALL || it.type == ItemType.ULTRABALL || it.type == ItemType.MASTERBALL
    }

    // --- ESTADOS ---
    var isLaunching by remember { mutableStateOf(false) }
    var selectedBall by remember { mutableStateOf<InventoryItem?>(null) }
    var catchResult by remember { mutableStateOf<String?>(null) }
    var activeMiniGame by remember { mutableStateOf<Int?>(null) }
    var showTutorial by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }

    // Animação da Pokébola
    val ballYOffset by animateIntOffsetAsState(
        targetValue = if (isLaunching) IntOffset(0, -1100) else IntOffset(0, 0),
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "ball"
    )

    // --- Dentro do CatchScreen.kt ---
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

// Estado para guardar a inclinação (Eixo X)
    var tiltX by remember { mutableStateOf(0f) }

// Listener para o sensor
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    // O valor X indica a inclinação lateral
                    tiltX = event.values[0]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

// Registar e remover o sensor conforme o ciclo de vida do ecrã
    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // --- LÓGICA DE JOGO ---
    fun onGameFinished(success: Boolean) {
        gameStarted = false
        activeMiniGame = null
        if (success) {
            scope.launch {
                isLaunching = true
                delay(1000)
                val chance = Random.nextInt(100)
                val caught = when (selectedBall?.type) {
                    ItemType.POKEBALL -> chance < 30
                    ItemType.ULTRABALL -> chance < 65
                    ItemType.MASTERBALL -> true
                    else -> false
                }
                catchResult = if (caught) "Success" else "Failed"
            }
        } else {
            catchResult = "Failed"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp)) {
                Image(painterResource(R.drawable.ic_backpack), null, Modifier.size(50.dp))
                Spacer(Modifier.width(12.dp))
                Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        inventoryBalls.distinctBy { it.type }.forEach { ball ->
                            val count = inventoryBalls.count { it.type == ball.type }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                if (!isLaunching && activeMiniGame == null) {
                                    selectedBall = ball
                                    viewModel.inventory.remove(ball)
                                    activeMiniGame = Random.nextInt(4)
                                    showTutorial = true
                                }
                            }) {
                                Image(painterResource(ball.icon), null, Modifier.size(40.dp))
                                Text("x$count", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            Text("Select a ball to start!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))

            // Pokémon Area
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth().height(400.dp)) {
                Image(painterResource(R.drawable.ic_grass_platform), null, Modifier.width(280.dp), contentScale = ContentScale.FillWidth)
                Image(
                    painter = painterResource(pokemonResId), null,
                    modifier = Modifier.size(180.dp).padding(bottom = 80.dp).alpha(if (isLaunching && ballYOffset.y < -700) 0f else 1f)
                )
                if (isLaunching && selectedBall != null) {
                    Image(painterResource(selectedBall!!.icon), null, Modifier.size(60.dp).offset { ballYOffset })
                }
            }
            Spacer(Modifier.weight(0.5f))
        }

        // --- OVERLAY DOS JOGOS E TUTORIAL ---
        activeMiniGame?.let { gameId ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)), contentAlignment = Alignment.Center) {
                if (showTutorial) {
                    val info = when(gameId) {
                        0 -> "RING CHALLENGE" to "Click when the circles match!"
                        1 -> "RAPID TAP" to "Tap 20 times before time runs out!"
                        2 -> "REFLEX CHALLENGE" to "Tap the barry as fast as you can!"
                        else -> "BALANCE TEST" to "Keep it as centered as possible!"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(info.first, color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(info.second, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                        Button(onClick = { showTutorial = false; gameStarted = true }) { Text("START") }
                    }
                } else if (gameStarted) {
                    val ballIcon = selectedBall?.icon ?: R.drawable.map_pball

                    when(gameId) {
                        // Passamos o ballIcon para os jogos que precisam dele
                        0 -> MiniGameShrinkingRing(selectedBallIcon = ballIcon) { onGameFinished(it) }
                        1 -> MiniGameRapidTap(selectedBallIcon = ballIcon) { onGameFinished(it) }
                        2 -> MiniGameReaction { onGameFinished(it) }
                        3 -> MiniGameGyroBalance(ballIcon, tiltX) { onGameFinished(it) }
                    }
                }
            }
        }
    }

    // Popups de Resultado
    if (catchResult != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (catchResult == "Success") "Gotcha!" else "Oh no!") },
            text = {
                Text(
                    if (catchResult == "Success")
                        "The Pokemon was caught! Returning to PokeCenter..."
                    else
                        "The Pokemon ran away!"
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (catchResult == "Success") {
                        viewModel.coins += 50

                        try {
                            navController.navigate("energy_screen") {
                                popUpTo("catch") { inclusive = true } // Remove o ecrã de captura
                            }
                        } catch (e: Exception) {
                            // Caso o nome esteja errado, volta apenas para trás para não crashar
                            navController.popBackStack()
                        }
                    } else {
                        navController.popBackStack()
                    }
                    catchResult = null
                }) { Text("OK") }
            }
        )
    }
}


@Composable
fun MiniGameShrinkingRing(selectedBallIcon: Int, onResult: (Boolean) -> Unit) {
    var scale by remember { mutableStateOf(2f) }

    LaunchedEffect(Unit) {
        animate(2f, 0.4f, animationSpec = tween(1500, easing = LinearEasing)) { value, _ ->
            scale = value
        }
        onResult(false)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().clickable {
            // Sucesso se clicar quando a bola está sobre o alvo (margem de 0.8 a 1.2)
            onResult(scale in 0.8f..1.2f)
        }
    ) {
        // Alvo (Círculo de captura)
        Box(
            Modifier
                .size(100.dp)
                .border(
                    width = 4.dp,
                    color = if (scale in 0.8f..1.2f) Color(0xFF4CAF50) else Color.White.copy(0.5f),
                    shape = CircleShape
                )
        )

        Image(
            painter = painterResource(id = selectedBallIcon),
            contentDescription = null,
            modifier = Modifier
                .size((100 * scale).dp)
                .alpha(0.8f)
        )

        Text(
            "CATCH ZONE",
            color = if (scale in 0.8f..1.2f) Color(0xFF4CAF50) else Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.offset(y = 140.dp)
        )
    }
}

@Composable
fun MiniGameRapidTap(selectedBallIcon: Int, onResult: (Boolean) -> Unit) {
    var taps by remember { mutableStateOf(0) }
    var time by remember { mutableStateOf(40) }
    val target = 20

    // Animação de vibração da bola
    val shakeOffset by animateDpAsState(
        targetValue = if (taps % 2 == 0) 0.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium), label = ""
    )

    LaunchedEffect(Unit) {
        while (time > 0) { delay(100); time-- }
        onResult(taps >= target)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("STABILIZE THE POKEBALL!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(15.dp))

        // Barra de Estabilidade (Amarelo Amigável)
        LinearProgressIndicator(
            progress = taps.toFloat() / target,
            modifier = Modifier.width(250.dp).height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = Color(0xFFFFD54F), // Honey Yellow
            trackColor = Color.White.copy(0.2f)
        )

        Spacer(Modifier.height(60.dp))

        // A Pokébola escolhida pelo user a abanar
        Image(
            painter = painterResource(id = selectedBallIcon),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp)
                .offset(x = shakeOffset)
                .clickable { taps++ }
        )

        Text(
            "PROGRESS: $taps / $target",
            color = Color(0xFFFFD54F),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
fun MiniGameReaction(onResult: (Boolean) -> Unit) {
    var isAppearing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(Random.nextLong(1500, 3500))
        isAppearing = true
        delay(650)
        if (isAppearing) onResult(false)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (isAppearing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.oran_berry),
                    contentDescription = null,
                    modifier = Modifier
                        .size(130.dp)
                        .clickable { onResult(true) }
                )
                Text(
                    "NOW!",
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )
            }
        } else {
            Text("STAY READY...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light)
        }
    }
}


@Composable
fun MiniGameGyroBalance(selectedBallIcon: Int, tiltX: Float, onResult: (Boolean) -> Unit) {
    var ballPosition by remember { mutableStateOf(0f) } // 0f é o centro
    var timeLeft by remember { mutableStateOf(50) } // 5 segundos (50 * 100ms)
    val limit = 150f // Limite lateral da barra em pixels

    // Atualizar posição da bola com base no sensor
    LaunchedEffect(tiltX) {
        // Invertemos o tiltX porque o sensor reporta a aceleração oposta à inclinação
        ballPosition -= tiltX * 2f

        // Verificar se caiu da barra
        if (ballPosition > limit || ballPosition < -limit) {
            onResult(false)
        }
    }

    // Timer de sobrevivência
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(100)
            timeLeft--
        }
        onResult(true) // Se aguentou os 5 segundos -> ganhou
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BALANCE THE BALL!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Tilt your phone to stay centered", color = Color(0xFFFFD54F), fontSize = 14.sp)

        Spacer(Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(300.dp).height(100.dp)) {
            // A Barra de Equilíbrio
            Box(Modifier.width(300.dp).height(8.dp).background(Color.White.copy(0.3f), RoundedCornerShape(4.dp)))

            // Zona Segura (Centro)
            Box(Modifier.width(60.dp).height(12.dp).background(Color(0xFF4CAF50).copy(0.5f), CircleShape))

            Image(
                painter = painterResource(id = selectedBallIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .offset { IntOffset(ballPosition.roundToInt(), -30) } // Sobe 30px para ficar em cima da barra
            )
        }

        Spacer(Modifier.height(20.dp))

        // Indicador de Tempo
        CircularProgressIndicator(
            progress = timeLeft / 50f,
            color = Color(0xFFFFD54F),
            strokeWidth = 6.dp,
            modifier = Modifier.size(60.dp)
        )
    }
}