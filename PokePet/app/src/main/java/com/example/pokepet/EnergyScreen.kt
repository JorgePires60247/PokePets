package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(navController: NavController, viewModel: PetViewModel) {
    val groupedInventory = viewModel.inventory.groupBy { it.type }
    val scope = rememberCoroutineScope()

    // --- ESTADOS DE ANIMAÇÃO ---
    var showSparkleAnim by remember { mutableStateOf(false) }

    // Efeito para desligar as animações automaticamente
    LaunchedEffect(showSparkleAnim) { if (showSparkleAnim) { delay(1200); showSparkleAnim = false } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PokeCenter") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Welcome to the PokeCenter!", fontSize = 16.sp)
            Text(text = "Nível ${viewModel.currentLevel}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))

            Spacer(modifier = Modifier.height(12.dp))

            // --- 🚀 UI DA ANIMAÇÃO SOBRE O PET ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                AsyncImage(
                    model = R.drawable.happy,
                    contentDescription = "Pet",
                    modifier = Modifier.size(120.dp)
                )

                if (showSparkleAnim) AnimatedSparkleEffect()
            }

            Text(text = "Coins: ${viewModel.coins}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // --- INVENTÁRIO (SÓ POÇÕES E MAPA) ---
            SectionTitle("Inventory (Tap to use potions)")
            if (viewModel.inventory.isEmpty()) {
                EmptyCard("Your inventory is empty.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedInventory.forEach { (type, items) ->
                        val firstItem = items.first()
                        InventorySlot(
                            iconRes = firstItem.icon,
                            label = firstItem.name,
                            count = items.size,
                            onClick = {
                                when (type) {
                                    ItemType.MAP -> navController.navigate("map_screen")
                                    ItemType.POTION, ItemType.FULL_HEAL, ItemType.FULL_HEART,
                                    ItemType.FULL_CLEAN, ItemType.FULL_HUNGER -> {
                                        viewModel.useItem(items.last())
                                        // Disparar animação correta
                                        if (type == ItemType.FULL_HEART) showSparkleAnim = true
                                    }
                                    else -> {} // Pokeballs e Identifiers ignorados aqui
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECÇÃO DE LOJA ---
            SectionTitle("Special Exploration")
            ShopItemCard("Adventure Map", "Unlock areas!", 150, R.drawable.ic_map, viewModel.inventory.count { it.type == ItemType.MAP }, 1) {
                viewModel.buyItem(ItemType.MAP, 150, "Map", R.drawable.ic_map)
            }
            ShopItemCard("Identifier", "Identify Pokemon", 300, R.drawable.ic_identifier, viewModel.inventory.count { it.type == ItemType.IDENTIFIER }) {
                viewModel.buyItem(ItemType.IDENTIFIER, 300, "Identifier", R.drawable.ic_identifier)
            }

            SectionTitle("Potions & Care")
            ShopItemCard("Standard Potion", "Restore 25% HP", 10, R.drawable.ic_potion, viewModel.inventory.count { it.type == ItemType.POTION }) {
                viewModel.buyItem(ItemType.POTION, 10, "Potion", R.drawable.ic_potion)
            }
            ShopItemCard("Full Heal", "Full HP", 50, R.drawable.ic_fullheal, viewModel.inventory.count { it.type == ItemType.FULL_HEAL }) {
                viewModel.buyItem(ItemType.FULL_HEAL, 50, "Full Heal", R.drawable.ic_fullheal)
            }
            ShopItemCard("Full Heart", "Full Happiness", 40, R.drawable.ic_fullheart, viewModel.inventory.count { it.type == ItemType.FULL_HEART }) {
                viewModel.buyItem(ItemType.FULL_HEART, 40, "Full Heart", R.drawable.ic_fullheart)
            }
            ShopItemCard("Full Clean", "Full Cleanliness", 30, R.drawable.ic_fullclean, viewModel.inventory.count { it.type == ItemType.FULL_CLEAN }) {
                viewModel.buyItem(ItemType.FULL_CLEAN, 30, "Full Clean", R.drawable.ic_fullclean)
            }
            ShopItemCard("Full Food", "Full Hunger Bar", 35, R.drawable.ic_fullhunger, viewModel.inventory.count { it.type == ItemType.FULL_HUNGER }) {
                viewModel.buyItem(ItemType.FULL_HUNGER, 35, "Full Food", R.drawable.ic_fullhunger)
            }

            SectionTitle("Pokeballs")
            ShopItemCard("Pokeball", "25% Catch", 20, R.drawable.ic_pokeball, viewModel.inventory.count { it.type == ItemType.POKEBALL }) {
                viewModel.buyItem(ItemType.POKEBALL, 20, "Pokeball", R.drawable.ic_pokeball)
            }
            ShopItemCard("Ultra Ball", "50% Catch", 50, R.drawable.ic_ultraball, viewModel.inventory.count { it.type == ItemType.ULTRABALL }) {
                viewModel.buyItem(ItemType.ULTRABALL, 50, "Ultra Ball", R.drawable.ic_ultraball)
            }
            ShopItemCard("Master Ball", "100% Catch", 200, R.drawable.ic_masterball, viewModel.inventory.count { it.type == ItemType.MASTERBALL }) {
                viewModel.buyItem(ItemType.MASTERBALL, 200, "Master Ball", R.drawable.ic_masterball)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- 🚀 FUNÇÕES DAS ANIMAÇÕES ---


@Composable
fun AnimatedSparkleEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse), label = ""
    )

    Text("✨", fontSize = 45.sp, modifier = Modifier.scale(scale))
}



@Composable
fun SectionTitle(title: String) {
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth().height(70.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
fun ShopItemCard(name: String, desc: String, price: Int, @DrawableRes icon: Int, currentCount: Int, limit: Int = 5, onBuy: () -> Unit) {
    val isFull = currentCount >= limit
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFull) Color(0xFFF0F0F0) else Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), null, Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = if (isFull) "Limit reached ($limit/$limit)" else desc, fontSize = 11.sp, color = if (isFull) Color.Red else Color.Gray)
            }
            Button(
                onClick = onBuy, enabled = !isFull,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(if (isFull) "OWNED" else "${price}c", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InventorySlot(iconRes: Int, label: String, count: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        Card(modifier = Modifier.fillMaxSize(), onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(iconRes), null, Modifier.size(30.dp))
                Text(text = label, fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 9.sp)
            }
        }
        if (count > 1) {
            Surface(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), shape = CircleShape, color = Color(0xFFE91E63)) {
                Text(text = "x$count", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
            }
        }
    }
}