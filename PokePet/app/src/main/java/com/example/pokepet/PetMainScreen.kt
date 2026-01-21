package com.example.pokepet

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetMainScreen(
    petName: String,
    navController: NavController,
    viewModel: PetViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- ESTADOS DE CONTROLO ---
    var showHatchTutorial by remember { mutableStateOf(viewModel.currentLevel == 1 && viewModel.currentXP == 0f) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Internal state for the visual Level Up overlay
    var showLevelUpEffect by remember { mutableStateOf(false) }

    // Intercetar o botão "Back" do sistema para Logout
    BackHandler(enabled = true) {
        showLogoutDialog = true
    }

    // 1. EFEITO VISUAL: Deteta se há uma celebração pendente no ViewModel
    LaunchedEffect(viewModel.showLevelUpCelebration) {
        if (viewModel.showLevelUpCelebration) {
            showLevelUpEffect = true
            delay(4000) // Duração do efeito visual
            showLevelUpEffect = false
            viewModel.showLevelUpCelebration = false // Resetar o flag no ViewModel após mostrar
        }
    }

    // 2. SNACKBAR: Lógica de Desbloqueio do PokeCenter no Nível 2
    LaunchedEffect(viewModel.currentLevel) {
        if (viewModel.currentLevel >= 2 && !viewModel.hasShownPokeCenterUnlockWarning) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "New Location: PokeCenter is now open!",
                    actionLabel = "Go Now",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navController.navigate("energy_screen")
                }
            }
            viewModel.hasShownPokeCenterUnlockWarning = true
        }
    }

    // --- DIÁLOGO DE LOGOUT ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        navController.navigate("login_screen") {
                            popUpTo("main_screen/{petName}") { inclusive = true }
                        }
                    }
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("No") }
            }
        )
    }

    // Alerta de Tutorial inicial
    if (showHatchTutorial) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Welcome, Trainer! 🎉") },
            text = {
                Column {
                    Text("Congratulations on hatching $petName!")
                    Spacer(Modifier.height(8.dp))
                    Text("• Watch the Vital States to keep $petName healthy.")
                    Text("• Feed and clean to gain Experience (XP).")
                    Text("• Reach Level 2 to unlock the PokeCenter!")
                }
            },
            confirmButton = {
                Button(onClick = { showHatchTutorial = false }) { Text("Got it!") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título e Nível Atual
                Text(text = "Yay! $petName has hatched!", fontSize = 18.sp)
                Text(
                    text = "Level ${viewModel.currentLevel}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Imagem Dinâmica do Pokémon Ativo
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = PokemonCatalog.getPokemonImage(viewModel.activeSpeciesId),
                        contentDescription = "Pet",
                        modifier = Modifier.size(200.dp)
                    )

                    // Overlay de Level Up
                    if (showLevelUpEffect) {
                        LevelUpAnimation()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barra de Progresso de XP
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "XP: ${(viewModel.currentXP * 100).toInt()}%", fontSize = 12.sp)
                    LinearProgressIndicator(
                        progress = { viewModel.currentXP },
                        modifier = Modifier
                            .width(200.dp)
                            .height(10.dp)
                            .clip(CircleShape),
                        color = Color(0xFFFFD700),
                        trackColor = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                VitalStatesSection(viewModel)
                Spacer(modifier = Modifier.weight(1f))

                // Botões de Ação Inferiores
                ActionButtonsRow(
                    navController = navController,
                    viewModel = viewModel,
                    onLockedClick = {
                        scope.launch { snackbarHostState.showSnackbar("Reach level 2 to unlock PokeCenter!") }
                    }
                )
            }

            // BOTÃO DE AJUDA (Top Right)
            IconButton(
                onClick = { showHatchTutorial = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Help",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Camada de Confetes
            if (showLevelUpEffect) {
                ConfettiOverlay()
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun LevelUpAnimation() {
    val scale by animateFloatAsState(
        targetValue = 1.1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Text(
        text = "LEVEL UP!",
        color = Color(0xFFFFD700),
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        modifier = Modifier
            .scale(scale)
            .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ConfettiOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(15) { i ->
            val startX = remember { (0..1000).random().toFloat() }
            val animY = rememberInfiniteTransition().animateFloat(
                initialValue = -100f, targetValue = 2000f,
                animationSpec = infiniteRepeatable(tween(2000 + i * 100, easing = LinearEasing)), label = ""
            )
            Text(
                text = listOf("✨", "⭐", "🎉", "🎊").random(),
                modifier = Modifier
                    .offset(x = (startX / 3).dp, y = (animY.value / 4).dp)
                    .alpha(0.7f)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VitalStatesSection(viewModel: PetViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vital States", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        VitalStat(R.drawable.hp_icon, Color.Red, "Health", viewModel.health)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(R.drawable.ic_clean, Color.Blue, "Hygiene", viewModel.hygiene)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(R.drawable.hunger_icon, Color.Magenta, "Food", viewModel.food)
    }
}

@Composable
fun VitalStat(@DrawableRes iconRes: Int, color: Color, label: String, level: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = color,
        targetValue = if (level < 0.2f) Color.Red else color,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = ""
    )
    val finalColor = if (level < 0.2f) animatedColor else color

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), null, Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { level },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = finalColor, trackColor = Color.LightGray
            )
            if (level < 0.2f) Text("LOW $label!", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ActionButtonsRow(navController: NavController, viewModel: PetViewModel, onLockedClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        Box {
            ActionButton(
                iconRes = if (viewModel.isPokeCenterUnlocked) R.drawable.ic_pokecenter else R.drawable.pokecenter_off_icon,
                label = "PokeCenter",
                onClick = {
                    if (viewModel.isPokeCenterUnlocked) navController.navigate("energy_screen")
                    else onLockedClick()
                }
            )
            if (viewModel.isPokeCenterUnlocked && !viewModel.hasSeenPokeCenterTutorial) {
                Box(Modifier.size(12.dp).align(Alignment.TopEnd).background(Color.Red, CircleShape))
            }
        }
        ActionButton(R.drawable.pokedex_icon, "PokeDex", onClick = { navController.navigate("pokedex_screen") })
        ActionButton(R.drawable.hunger_icon, "Food", onClick = { navController.navigate("food_screen") })
        ActionButton(R.drawable.clean_page_icon, "Hygiene", onClick = { navController.navigate("bathroom_screen") })
    }
}

@Composable
fun ActionButton(@DrawableRes iconRes: Int, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Image(painterResource(iconRes), null, Modifier.padding(12.dp))
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}