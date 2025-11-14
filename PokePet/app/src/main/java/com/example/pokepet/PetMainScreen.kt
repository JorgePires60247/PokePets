package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
// Import Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// This is the main composable for the new screen
@Composable
fun PetMainScreen(petName: String) { // It can receive the pet's name
    // 1. Wrap the entire screen in a Scaffold
    Scaffold(
        // 2. Place the ActionButtonsRow in the bottomBar slot
        bottomBar = {
            // Add some padding to the bottom bar itself for better spacing
            ActionButtonsRow(modifier = Modifier.padding(bottom = 16.dp))
        }
    ) { innerPadding -> // Scaffold provides padding for the content area
        Column(
            // 3. Apply the Scaffold's inner padding to the main content
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Use padding from Scaffold
                .padding(horizontal = 16.dp) // Add your own horizontal padding
                .padding(top = 16.dp), // Add top padding)
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top text section
            Text(
                text = "Yay! $petName has hatched!", // Personalized with the pet's name
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "What would you like to do first?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main pet image placeholder
            AsyncImage(
                model = R.drawable.happy, // Placeholder - you can change this later
                contentDescription = "Main Pet Image",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // This is a placeholder for the small blue bar under the pet
            LinearProgressIndicator(progress = { 0.8f }, modifier = Modifier.width(180.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // Vital States section
            VitalStatesSection()

            // 4. The Spacer that pushed the buttons to the bottom is NO LONGER NEEDED
            // Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VitalStatesSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // "Vital States" Dropdown header (non-functional for now)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Vital States",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Individual stat bars
        VitalStat(iconRes = R.drawable.hp_icon, color = Color.Red, label = "Health", level = 0.9f)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(iconRes = R.drawable.energy_icon, color = Color.Yellow, label = "Energy", level = 0.8f)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(iconRes = R.drawable.happiness_icon, color = Color.Green, label = "Happiness", level = 0.7f)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(iconRes = R.drawable.clean_icon, color = Color.Blue, label = "Hygiene", level = 0.8f)
        Spacer(modifier = Modifier.height(8.dp))
        VitalStat(iconRes = R.drawable.hunger_icon, color = Color.Magenta, label = "Food", level = 0.6f)
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

data class ActionItem(@DrawableRes val iconRes: Int, val label: String)

@Composable
fun ActionButtonsRow(modifier: Modifier = Modifier) { // Accept a modifier
    val actionItems = listOf(
        ActionItem(R.drawable.energy_page_icon, "Bedroom"),
        ActionItem(R.drawable.hunger_icon, "Feed"),
        ActionItem(R.drawable.clean_page_icon, "Bathroom"),
        ActionItem(R.drawable.happiness_page_icon, "Playground"),
        ActionItem(R.drawable.camera_page_icon, "Camera")
    )

    // Using LazyRow for a conventional horizontal scroll
    LazyRow(
        modifier = modifier.fillMaxWidth(), // Apply modifier here
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(actionItems) { item ->
            ActionButton(iconRes = item.iconRes, label = item.label)
        }
    }
}

@Composable
fun ActionButton(@DrawableRes iconRes: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
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

@Preview(showBackground = true)
@Composable
fun PetMainScreenPreview() {
    PetMainScreen(petName = "Pikachu")
}
