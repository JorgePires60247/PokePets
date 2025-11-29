package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown

// This is the main composable for the new screen
@Composable
fun PetMainScreen(petName: String, navController: NavController) { // It can receive the pet's name
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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

        // Spacer to push the action buttons to the bottom
        Spacer(modifier = Modifier.weight(1f))

        // Bottom action buttons
        ActionButtonsRow(navController = navController)
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
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Arrow"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Individual stat bars - ** NOW USING YOUR DRAWABLES **
        // Replace R.drawable.hp_icon, etc., with your actual file names
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

// UPDATED to accept a Drawable Resource ID
@Composable
fun VitalStat(@DrawableRes iconRes: Int, color: Color, label: String, level: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // UPDATED to use Image instead of Icon
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp) // Set a size for your icon
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
fun ActionButtonsRow(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ** NOW USING YOUR DRAWABLES **
        // Replace R.drawable.action_energy, etc., with your actual button image names
        ActionButton(iconRes = R.drawable.energy_page_icon, label = "Energy", onClick = { navController.navigate("energy_screen") })
        ActionButton(iconRes = R.drawable.hunger_icon, label = "Food", onClick = { navController.navigate("food_screen") })
        ActionButton(iconRes = R.drawable.clean_page_icon, label = "Hygiene", onClick = { navController.navigate("bathroom_screen") })
        ActionButton(iconRes = R.drawable.happiness_icon, label = "Happiness", onClick = { navController.navigate("happiness_screen") })
    }
}

// UPDATED to accept a Drawable Resource ID
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
            // UPDATED to use Image instead of Icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.padding(12.dp) // Adjust padding as needed
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp)
    }
}

// Preview function to see the screen in Android Studio's design mode
@Preview(showBackground = true)
@Composable
fun PetMainScreenPreview() {
    // You'll need to add dummy drawable resources for the preview to work without errors
    // For example, create ic_arrow_drop_down.xml, hp_icon.xml etc. in the drawable folder.
    // You can use the built-in vector asset studio for this.
    PetMainScreen(petName = "Pikachu", navController = rememberNavController())
}
