package com.example.pokepet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect // Import novo
import android.os.Vibrator // Import novo
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.draw.rotate
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CatchScreen(
    navController: NavController,
    viewModel: PetViewModel,
    pokemonId: Int,
    xpReward: Float
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- VIBRATOR SETUP ---
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val inventoryBalls = viewModel.inventory.filter {
        it.type == ItemType.POKEBALL || it.type == ItemType.ULTRABALL || it.type == ItemType.MASTERBALL
    }

    val extractedName = remember(pokemonId) {
        try {
            val fullName = context.resources.getResourceEntryName(pokemonId)
            fullName.removePrefix("p_")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // --- ESTADOS DE ANIMAÇÃO E JOGO ---
    var isLaunching by remember { mutableStateOf(false) }
    var isWobbling by remember { mutableStateOf(false) }
    var wobbleCount by remember { mutableIntStateOf(0) }
    var pokemonVisible by remember { mutableStateOf(true) }

    var selectedBall by remember { mutableStateOf<InventoryItem?>(null) }
    var catchResult by remember { mutableStateOf<String?>(null) }
    var activeMiniGame by remember { mutableStateOf<Int?>(null) }
    var showTutorial by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }

    // Animações (Y e Rotação)
    val ballYOffset by animateIntOffsetAsState(
        targetValue = if (isLaunching) IntOffset(0, -800) else IntOffset(0, 0),
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "launchY"
    )

    val ballRotation by animateFloatAsState(
        targetValue = if (isLaunching) 1080f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "launchRotate"
    )

    val wobbleRotation by animateFloatAsState(
        targetValue = if (isWobbling) (if (wobbleCount % 2 == 0) -15f else 15f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wobbleRotate"
    )

    // --- FUNÇÃO: SEQUÊNCIA DE CAPTURA COM VIBRAÇÃO ---
    fun startCatchSequence() {
        scope.launch {
            // Fase 1: A bola voa
            isLaunching = true
            delay(800)

            // Fase 2: Impacto
            pokemonVisible = false
            isLaunching = false
            isWobbling = true

            // Fase 3: O suspense (1... 2... 3...) com Vibração
            repeat(3) {
                delay(1000)
                wobbleCount++

                // --- CÓDIGO DE VIBRAÇÃO ---
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibra por 150ms com amplitude padrão
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Método antigo para Androids mais velhos
                    vibrator.vibrate(150)
                }
                // -------------------------
            }

            // Fase 4: Resultado
            delay(500)
            isWobbling = false

            // Vibração final mais longa se capturar, ou dupla rápida se falhar (Opcional, mas fica fixe)
            val chance = (1..100).random()
            val caught = when (selectedBall?.type) {
                ItemType.POKEBALL -> chance < 35
                ItemType.ULTRABALL -> chance < 65
                ItemType.MASTERBALL -> true
                else -> false
            }

            // Pequena vibração final para confirmar o resultado
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(50)
            }

            catchResult = if (caught) "Success" else "Failed"
        }
    }

    // --- FUNÇÃO: FINALIZAÇÃO DO MINIJOGO ---
    fun onGameFinished(success: Boolean) {
        gameStarted = false
        activeMiniGame = null
        if (success) {
            startCatchSequence()
        } else {
            catchResult = "Failed"
        }
    }

    // --- SENSORES ---
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var tiltX by remember { mutableStateOf(0f) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) tiltX = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    // --- INTERFACE (O resto mantém-se igual) ---
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            // Barra superior
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp)) {
                Image(painterResource(R.drawable.ic_backpack), null, Modifier.size(50.dp))
                Spacer(Modifier.width(12.dp))
                Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        inventoryBalls.distinctBy { it.type }.forEach { ball ->
                            val count = inventoryBalls.count { it.type == ball.type }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                if (!isLaunching && !isWobbling && activeMiniGame == null) {
                                    selectedBall = ball
                                    viewModel.inventory.remove(ball)
                                    activeMiniGame = (0..3).random()
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

            Spacer(Modifier.weight(1f))

            // Campo de batalha
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth().height(450.dp)) {
                Image(painterResource(R.drawable.ic_grass_platform), null, Modifier.width(280.dp), contentScale = ContentScale.FillWidth)

                if (pokemonVisible) {
                    Image(
                        painter = painterResource(pokemonId), null,
                        modifier = Modifier.size(250.dp).padding(bottom = 80.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                if (selectedBall != null && (!pokemonVisible || isLaunching)) {
                    Image(
                        painter = painterResource(selectedBall!!.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (isWobbling) 80.dp else 60.dp)
                            .offset {
                                if (isLaunching) {
                                    ballYOffset
                                } else {
                                    IntOffset(30, -220)
                                }
                            }
                            .rotate(if (isLaunching) ballRotation else wobbleRotation)
                    )
                }
            }

            if (isWobbling && wobbleCount > 0) {
                Text(text = "$wobbleCount...", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            }

            Spacer(Modifier.weight(0.5f))
        }

        activeMiniGame?.let { gameId ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)), contentAlignment = Alignment.Center) {
                if (showTutorial) {
                    val info = when(gameId) {
                        0 -> "RING CHALLENGE" to "Tap when the circles align!"
                        1 -> "RAPID TAP" to "Stabilize the ball with 20 taps!"
                        2 -> "REFLEX TEST" to "Tap the berry as soon as it appears!"
                        else -> "BALANCE" to "Tilt your phone to keep the ball centered!"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(info.first, color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(info.second, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                        Button(onClick = { showTutorial = false; gameStarted = true }) { Text("START") }
                    }
                } else if (gameStarted) {
                    val icon = selectedBall?.icon ?: R.drawable.map_pball
                    when(gameId) {
                        0 -> MiniGameShrinkingRing(icon) { onGameFinished(it) }
                        1 -> MiniGameRapidTap(icon) { onGameFinished(it) }
                        2 -> MiniGameReaction { onGameFinished(it) }
                        3 -> MiniGameGyroBalance(icon, tiltX) { onGameFinished(it) }
                    }
                }
            }
        }
    }

    if (catchResult != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (catchResult == "Success") "Gotcha! 🎉" else "Oh no! 💨") },
            text = { Text(if (catchResult == "Success") "The Pokémon was caught! Returning to the PokeCenter to rest!" else "\" The Pokémon broke free and fled! Let's go back to the PokeCenter...") },
            confirmButton = {
                Button(onClick = {
                    if (catchResult == "Success") {
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        val rarity = if (xpReward > 0.3f) "Legendary" else if (xpReward > 0.1f) "Rare" else "Common"
                        viewModel.gainXP(xpReward)
                        viewModel.coins += 100
                        viewModel.addToPokedex(
                            CaughtPokemon(
                                pokemonId = pokemonId,
                                name = extractedName,
                                rarity = rarity,
                                xpReward = xpReward,
                                dateCaught = date,
                            )
                        )
                    }
                    navController.navigate("energy_screen") { popUpTo("energy_screen") { inclusive = true } }
                    catchResult = null
                }) { Text("OK") }
            }
        )
    }
}

// ... (Os Minijogos mantêm-se iguais) ...
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
    var gameFinished by remember { mutableStateOf(false) } // Estado para evitar múltiplos disparos

    val shakeOffset by animateDpAsState(
        targetValue = if (taps % 2 == 0) 0.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium), label = ""
    )
    LaunchedEffect(taps) {
        if (taps >= target && !gameFinished) {
            gameFinished = true
            onResult(true)
        }
    }

// 2. CRONÓMETRO: Termina se o tempo esgotar
    LaunchedEffect(Unit) {
        while (time > 0 && !gameFinished) {
            delay(100)
            time--
        }
        if (!gameFinished) {
            gameFinished = true
            onResult(taps >= target)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("STABILIZE THE POKEBALL!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
// Barra de Estabilidade
        LinearProgressIndicator(
            progress = (taps.toFloat() / target).coerceAtMost(1f), // Garante que a barra não ultrapassa 100%
            modifier = Modifier.width(250.dp).height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = Color(0xFFFFD54F), // Honey Yellow
            trackColor = Color.White.copy(0.2f)
        )
        Spacer(Modifier.height(60.dp))
// A Pokébola
        Image(
            painter = painterResource(id = selectedBallIcon),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp)
                .offset(x = shakeOffset)
// 3. BLOQUEIO: Só permite clicar se o jogo não tiver terminado
                .clickable(enabled = !gameFinished) {
                    taps++
                }
        )
        Text(
            "PROGRESS: ${taps.coerceAtMost(target)} / $target",
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
                    .offset { IntOffset(ballPosition.roundToInt(), -30) }
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