package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetMainScreen(
    petName: String,
    navController: NavController,
    viewModel: PetViewModel // ViewModel partilhado que controla os dados
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título e Nível
            Text(
                text = "Yay! $petName has hatched!",
                fontSize = 18.sp
            )
            Text(
                text = "Nível ${viewModel.currentLevel}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Imagem do Pet
            AsyncImage(
                model = R.drawable.happy,
                contentDescription = "Main Pet Image",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de XP Interativa (Dourada)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Experiência (XP): ${(viewModel.currentXP * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { viewModel.currentXP },
                    modifier = Modifier
                        .width(200.dp)
                        .height(10.dp),
                    color = Color(0xFFFFD700), // Cor de Ouro para o XP
                    trackColor = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Seção de Estados Vitais (HP, Higiene, Fome)
            VitalStatesSection(viewModel)

            Spacer(modifier = Modifier.weight(1f))

            // Botões de Ação Inferiores
            ActionButtonsRow(
                navController = navController,
                isUnlocked = viewModel.isPokeCenterUnlocked,
                onLockedClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Not yet! Take care of $petName to reach level 2 and unlock!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun VitalStatesSection(viewModel: PetViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Vital States",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Arrow"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barras conectadas ao ViewModel
        VitalStat(iconRes = R.drawable.hp_icon, color = Color.Red, label = "Health", level = viewModel.health)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(iconRes = R.drawable.ic_clean, color = Color.Blue, label = "Hygiene", level = viewModel.hygiene)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(iconRes = R.drawable.hunger_icon, color = Color.Magenta, label = "Food", level = viewModel.food)
    }
}

@Composable
fun VitalStat(@DrawableRes iconRes: Int, color: Color, label: String, level: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        LinearProgressIndicator(
            progress = { level },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = color,
            trackColor = Color.LightGray
        )
    }
}

@Composable
fun ActionButtonsRow(
    navController: NavController,
    isUnlocked: Boolean,
    onLockedClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // PokeCenter (Dinâmico: muda o ícone se estiver bloqueado)
        ActionButton(
            iconRes = if (isUnlocked) R.drawable.ic_pokecenter else R.drawable.pokecenter_off_icon,
            label = "PokeCenter",
            onClick = {
                if (isUnlocked) navController.navigate("energy_screen") else onLockedClick()
            }
        )

        ActionButton(
            iconRes = R.drawable.hunger_icon,
            label = "Food",
            onClick = { navController.navigate("food_screen") }
        )

        ActionButton(
            iconRes = R.drawable.clean_page_icon,
            label = "Hygiene",
            onClick = { navController.navigate("bathroom_screen") }
        )
    }
}

@Composable
fun ActionButton(@DrawableRes iconRes: Int, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp)
    }
}

