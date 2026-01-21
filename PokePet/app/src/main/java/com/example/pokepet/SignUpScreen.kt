package com.example.pokepet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController, petViewModel: PetViewModel) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }

    // Validações de interface
    val isEmailValid = "@" in email.text && "." in email.text
    val isPasswordValid = password.text.length >= 8 && password.text.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.text == confirmPassword.text

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val dbRef = FirebaseDatabase.getInstance().reference

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Your Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de Email
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

            // Campo de Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMsg = null },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Password
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

            Spacer(modifier = Modifier.height(16.dp))

            // Confirmação de Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMsg = null },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = !passwordsMatch && confirmPassword.text.isNotEmpty()
            )
            if (!passwordsMatch && confirmPassword.text.isNotEmpty()) {
                Text("Passwords do not match.", color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mensagem de Erro
            if (errorMsg != null) {
                Text(errorMsg!!, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMsg = null

                    auth.createUserWithEmailAndPassword(email.text.trim(), password.text)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid
                                val profile = mapOf(
                                    "username" to username.text.trim(),
                                    "email" to email.text.trim()
                                )

                                // Grava o perfil no Realtime Database
                                if (uid != null) {
                                    dbRef.child("users").child(uid).child("profile").setValue(profile)
                                        .addOnCompleteListener { writeTask ->
                                            isLoading = false
                                            if (writeTask.isSuccessful) {
                                                petViewModel.clearLocalPokemon()
                                                // Navega para o Login após o registo bem-sucedido
                                                navController.navigate("login_screen") {
                                                    popUpTo("signup_screen") { inclusive = true }
                                                }
                                            } else {
                                                errorMsg = writeTask.exception?.message ?: "Failed to save profile."
                                            }
                                        }
                                }
                            } else {
                                isLoading = false
                                errorMsg = task.exception?.message ?: "Sign up failed."
                            }
                        }
                },
                enabled = isEmailValid && isPasswordValid && passwordsMatch && username.text.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Sign Up",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}