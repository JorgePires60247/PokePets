package com.teuapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.pokepet.R


@Composable
fun FirstPageScreen(
    onPokeCenterClick: () -> Unit,
    onFoodClick: () -> Unit,
    onHygieneClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Congratulations!",
            fontSize = 28.sp,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your PokePet is now ready to explore!\nYou can now use the PokeCenter",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.happy),
            contentDescription = "PokePet",
            modifier = Modifier
                .size(180.dp)
                .padding(vertical = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularIconButton(
                iconRes = R.drawable.ic_pokecenter,
                label = "PokeCenter",
                onClick = onPokeCenterClick
            )
            CircularIconButton(
                iconRes = R.drawable.hunger_icon,
                label = "Food",
                onClick = onFoodClick
            )
            CircularIconButton(
                iconRes = R.drawable.clean_page_icon,
                label = "Hygiene",
                onClick = onHygieneClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CircularIconButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
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
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
