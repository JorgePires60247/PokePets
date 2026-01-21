package com.example.pokepet

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.google.firebase.database.FirebaseDatabase

@RequiresApi(Build.VERSION_CODES.O)
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

    val auth = remember { FirebaseAuth.getInstance() }
    val dbRef = remember { FirebaseDatabase.getInstance().reference }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Added weight to push the footer button down
            Spacer(modifier = Modifier.weight(0.5f))

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

                                if (uid != null) {
                                    dbRef.child("users").child(uid).child("profile").setValue(profile)
                                        .addOnCompleteListener { writeTask ->
                                            isLoading = false
                                            if (writeTask.isSuccessful) {
                                                petViewModel.clearLocalPokemon()
                                                navController.navigate("login_screen") {
                                                    popUpTo("signup_screen") { inclusive = true }
                                                }
                                            } else {
                                                errorMsg = writeTask.exception?.message ?: "Failed to save profile."
                                            }
                                        }
                                } else {
                                    isLoading = false
                                    errorMsg = "Erro: Utilizador criado, mas ID não encontrado."
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

            // Pushes the footer to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // --- THE LOOP BACK TO LOGIN ---
            val annotatedText = buildAnnotatedString {
                append("Already have an account? ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Log In")
                }
            }

            TextButton(onClick = { navController.navigate("login_screen") }) {
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