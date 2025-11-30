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

@Composable
fun PotionsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.happy),
            contentDescription = "PokePet",
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = 0.7f,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = Color(0xFFFFD700),
            trackColor = Color(0xFFE0E0E0)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Potions",
            fontSize = 20.sp,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Ícones das poções
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                PotionIcon(R.drawable.ic_fullheal, "Full Heal")
                PotionIcon(R.drawable.ic_potion, "Potion")
                PotionIcon(R.drawable.ic_fullheart, "Full Heart")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                PotionIcon(R.drawable.ic_fullclean, "Full Clean")
                PotionIcon(R.drawable.ic_fullhunger, "Full Hunger")
            }
        }
    }
}

@Composable
fun PotionIcon(iconRes: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE0E0E0), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
