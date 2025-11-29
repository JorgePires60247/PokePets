package com.example.pokepet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pokepet.R
import com.teuapp.ui.CircularIconButton

@Composable
fun PokeCenterScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PokeCenter",
            fontSize = 24.sp,
            color = Color(0xFF333333),
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
        )

        Image(
            painter = painterResource(id = R.drawable.happy),
            contentDescription = "PokePet",
            modifier = Modifier
                .size(180.dp)
                .padding(vertical = 16.dp)
        )

        // Barra de progresso amarela
        LinearProgressIndicator(
            progress = 0.7f,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = Color(0xFFFFD700),
            trackColor = Color(0xFFE0E0E0)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Vital States
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Vital States",
                fontSize = 18.sp,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            VitalBar(iconRes = R.drawable.ic_heart, color = Color.Red, progress = 0.8f)
            VitalBar(iconRes = R.drawable.clean_icon, color = Color.Blue, progress = 0.6f)
            VitalBar(iconRes = R.drawable.hunger_icon, color = Color.Green, progress = 0.5f)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botões inferiores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularIconButton(
                iconRes = R.drawable.ic_potion,
                label = "Potions",
                onClick = { /* ação futura */ }
            )
            CircularIconButton(
                iconRes = R.drawable.ic_pokeball,
                label = "Explore",
                onClick = { /* ação futura */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun VitalBar(iconRes: Int, color: Color, progress: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = color,
            trackColor = Color(0xFFE0E0E0)
        )
    }
}
