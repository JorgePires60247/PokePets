package com.example.pokepet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, petViewModel: PetViewModel) {

    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }

    // Validações da interface
    val isEmailValid = "@" in email.text
    val isPasswordValid = password.text.length >= 8 && password.text.any { !it.isLetterOrDigit() }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Text(text = "PokePet", fontSize = 36.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Welcome Back!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de Email com erro visual
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMsg = null },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                isError = !isEmailValid && email.text.isNotEmpty()
            )
            if (!isEmailValid && email.text.isNotEmpty()) {
                Text("Please enter a valid email.", color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Password com erro visual
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = !isPasswordValid && password.text.isNotEmpty()
            )
            if (!isPasswordValid && password.text.isNotEmpty()) {
                Text("Password must be at least 8 characters and include a symbol.", color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exibição de erros vindos do Firebase
            if (errorMsg != null) {
                Text(text = errorMsg!!, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMsg = null

                    auth.signInWithEmailAndPassword(email.text.trim(), password.text)
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                isLoading = false
                                errorMsg = task.exception?.message ?: "Login failed."
                                return@addOnCompleteListener
                            }

                            // ✅ Verifica se o utilizador já tem um Pokémon ativo
                            petViewModel.loadActivePokemon { hasPokemon, err ->
                                isLoading = false
                                if (err != null) {
                                    errorMsg = err
                                    return@loadActivePokemon
                                }

                                if (hasPokemon) {
                                    // Se já tiver Pokémon, vai para a Main Screen
                                    navController.navigate("main_screen/${petViewModel.activePokemonName}") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                } else {
                                    // Se não tiver, vai para o Hatching para ganhar um Pokémon aleatório
                                    navController.navigate("hatching_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                }
                            }
                        }
                },
                enabled = isEmailValid && isPasswordValid && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Login",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val annotatedText = buildAnnotatedString {
                append("Don't have an account? ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Sign Up") }
            }

            TextButton(onClick = { navController.navigate("signup_screen") }) {
                Text(
                    text = annotatedText,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}