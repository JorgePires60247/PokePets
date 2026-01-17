package com.example.pokepet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(
    navController: NavController,
    viewModel: PetViewModel
) {
    val groupedInventory = viewModel.inventory.groupBy { it.type }

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

            // Cabeçalho e XP
            Text(text = "Welcome to the PokeCenter!", fontSize = 16.sp)
            Text(text = "Nível ${viewModel.currentLevel}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))

            Spacer(modifier = Modifier.height(12.dp))

            AsyncImage(model = R.drawable.happy, contentDescription = "Pet", modifier = Modifier.size(120.dp))

            Text(text = "Coins: ${viewModel.coins}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. INVENTÁRIO ---
            SectionTitle("Inventory (Tap to use)")
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
                                // 🚀 LÓGICA ESPECIAL PARA O MAPA
                                if (type == ItemType.MAP) {
                                    navController.navigate("map_screen")
                                } else {
                                    viewModel.useItem(items.last())
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. SECÇÃO ESPECIAL: MAPA (Limite 1) ---
            SectionTitle("Special Exploration")
            ShopItemCard(
                name = "Adventure Map",
                desc = "Unlock new areas to explore!",
                price = 150,
                icon = R.drawable.ic_map,
                currentCount = viewModel.inventory.count { it.type == ItemType.MAP },
                limit = 1, // 🚀 Limite único
                onBuy = { viewModel.buyItem(ItemType.MAP, 150, "Map", R.drawable.ic_map) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. POTIONS ---
            SectionTitle("Potions")
            ShopItemCard(name = "Full Heal", desc = "Restore all stats", price = 50, icon = R.drawable.ic_fullheal, currentCount = viewModel.inventory.count { it.type == ItemType.FULL_HEAL }, onBuy = { viewModel.buyItem(ItemType.FULL_HEAL, 50, "Full Heal", R.drawable.ic_fullheal) })
            ShopItemCard(name = "Standard Potion", desc = "Restore 25% health", price = 10, icon = R.drawable.ic_potion, currentCount = viewModel.inventory.count { it.type == ItemType.POTION }, onBuy = { viewModel.buyItem(ItemType.POTION, 10, "Potion", R.drawable.ic_potion) })

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. POKEBALLS ---
            SectionTitle("Pokeballs")
            ShopItemCard(name = "Pokeball", desc = "25% Catch Rate", price = 20, icon = R.drawable.ic_pokeball, currentCount = viewModel.inventory.count { it.type == ItemType.POKEBALL }, onBuy = { viewModel.buyItem(ItemType.POKEBALL, 20, "Pokeball", R.drawable.ic_pokeball) })
            ShopItemCard(name = "Ultra Ball", desc = "50% Catch Rate", price = 50, icon = R.drawable.ic_ultraball, currentCount = viewModel.inventory.count { it.type == ItemType.ULTRABALL }, onBuy = { viewModel.buyItem(ItemType.ULTRABALL, 50, "Ultra Ball", R.drawable.ic_ultraball) })
            ShopItemCard(name = "Master Ball", desc = "100% Catch Rate", price = 200, icon = R.drawable.ic_masterball, currentCount = viewModel.inventory.count { it.type == ItemType.MASTERBALL }, onBuy = { viewModel.buyItem(ItemType.MASTERBALL, 200, "Master Ball", R.drawable.ic_masterball) })

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. ITEMS ---
            SectionTitle("Tools & Items")
            ShopItemCard(name = "Identifier", desc = "Identify wild Pokemon", price = 30, icon = R.drawable.ic_identifier, currentCount = viewModel.inventory.count { it.type == ItemType.IDENTIFIER }, onBuy = { viewModel.buyItem(ItemType.IDENTIFIER, 30, "Identifier", R.drawable.ic_identifier) })
            ShopItemCard(name = "Fishing Rod", desc = "Fish in water areas", price = 60, icon = R.drawable.ic_fishing_rod, currentCount = viewModel.inventory.count { it.type == ItemType.FISHING_ROD }, onBuy = { viewModel.buyItem(ItemType.FISHING_ROD, 60, "Fishing Rod", R.drawable.ic_fishing_rod) })

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// (Componentes auxiliares SectionTitle, EmptyCard, ShopItemCard e InventorySlot permanecem iguais aos definidos anteriormente)

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