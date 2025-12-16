package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rai.quizha.frontend.viewmodel.AdminViewModel

// Define the custom Vibrant Pink Color
val VibrantPink = Color(0xFFFF0266)
val LightPinkBg = Color(0xFFFEEBF1)

@Composable
fun AdminLoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: (token: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val loginResult by viewModel.loginResult.collectAsState()
    val error by viewModel.error.collectAsState()

    // --- LOGIC PRESERVED START ---
    LaunchedEffect(loginResult) {
        loginResult?.let {
            isLoading = false
            onLoginSuccess(it)
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            isLoading = false
        }
    }
    // --- LOGIC PRESERVED END ---

    // Parent Box centers the content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, LightPinkBg)
                )
            )
            .padding(16.dp), // Safety padding for very small screens
        contentAlignment = Alignment.Center
    ) {
        // Fixed Size Card
        Card(
            modifier = Modifier
                .width(350.dp) // Fixed width
                .wrapContentHeight(), // Height adapts to content
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 40.dp, horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Header Section
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    text = "Admin Portal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibrantPink, // Accent color on subtitle
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                )

                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Username Icon",
                            tint = if (username.isNotEmpty()) VibrantPink else Color.Gray
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibrantPink,
                        focusedLabelColor = VibrantPink,
                        cursorColor = VibrantPink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Password Icon",
                            tint = if (password.isNotEmpty()) VibrantPink else Color.Gray
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibrantPink,
                        focusedLabelColor = VibrantPink,
                        cursorColor = VibrantPink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Modern Button
                Button(
                    onClick = {
                        isLoading = true
                        viewModel.login(
                            username = username.trim(),
                            password = password
                        )
                    },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantPink,
                        disabledContainerColor = VibrantPink.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "LOGIN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Error Message Display
                if (error != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}