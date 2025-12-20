// src/jvmMain/kotlin/com/rai/quizha/main.kt
package com.rai.quizha

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rai.quizha.frontend.ui.AppScreen
import com.rai.quizha.frontend.ui.color.QuizhaTheme
import com.rai.quizha.frontend.ui.main.AdminLoginScreen
import com.rai.quizha.frontend.ui.main.MainScreen
import com.rai.quizha.frontend.viewmodel.ActivityViewModel
import com.rai.quizha.frontend.viewmodel.AdminViewModel
import com.rai.quizha.frontend.viewmodel.QuestionsViewModel
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import com.rai.quizha.server.model.JwtConfig
import com.rai.quizha.server.entry.startEmbeddedServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import java.net.NetworkInterface

fun main() = application {

    val appScope = CoroutineScope(Dispatchers.IO)
    val serverPort = 8080

    // 1. Start server (Backend)
    appScope.launch {
        val jwtConfig = JwtConfig(
            secret = "super-secret-key",
            issuer = "quizzy-server",
            audience = "quizzy-client"
        )
        startEmbeddedServer(port = serverPort, jwtConfig = jwtConfig)
    }

    // 2. Setup Ktor client (Frontend)
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    val baseUrl = "http://localhost:$serverPort"

    // --- REALTIME IP UPDATE LOGIC ---
    // Use mutableStateOf so the UI updates when this changes
    var serverIp by remember { mutableStateOf(getLocalIpAddress()) }

    // Poll for network changes every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val newIp = getLocalIpAddress()
            if (newIp != serverIp) {
                serverIp = newIp
            }
            delay(3000) // Check every 3 seconds
        }
    }
    // --------------------------------

    // Admin ViewModel (Login)
    val adminViewModel = remember { AdminViewModel(client, baseUrl) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "QuizHa - Admin Panel"
    ) {
        QuizhaTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Application State
                var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
                var authToken by remember { mutableStateOf<String?>(null) }

                // 3. Initialize ViewModels

                // Student ViewModel
                val studentViewModel = remember {
                    StudentViewModel(
                        client = client,
                        baseUrl = baseUrl,
                        getToken = { authToken }
                    )
                }

                // Activity ViewModel
                val activityViewModel = remember {
                    ActivityViewModel(
                        client = client,
                        baseUrl = baseUrl,
                        getToken = { authToken ?: "" }
                    )
                }

                // Questions ViewModel
                val questionsViewModel = remember {
                    QuestionsViewModel(
                        httpClient = client,
                        baseUrl = baseUrl,
                        getToken = { authToken ?: "" }
                    )
                }

                // Navigation Logic
                when (currentScreen) {
                    AppScreen.LOGIN -> {
                        AdminLoginScreen(
                            viewModel = adminViewModel,
                            onLoginSuccess = { token ->
                                authToken = token
                                currentScreen = AppScreen.MAIN
                            }
                        )
                    }

                    AppScreen.MAIN -> {
                        // Ensure authToken is not null before passing
                        if (authToken != null) {
                            MainScreen(
                                authToken = authToken!!,
                                studentViewModel = studentViewModel,
                                activityViewModel = activityViewModel,
                                questionsViewModel = questionsViewModel,
                                serverIp = serverIp,   // <--- Passed Realtime IP
                                serverPort = serverPort, // <--- Passed Port
                                onLogout = {
                                    // 1. Clear the token
                                    authToken = null

                                    // 2. Reset the AdminViewModel state so it doesn't auto-trigger login
                                    adminViewModel.logout()

                                    // 3. Navigate back
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        } else {
                            // Fallback if token is somehow null
                            currentScreen = AppScreen.LOGIN
                        }
                    }
                }
            }
        }
    }
}

// Logic to find the likely LAN IP (Filters out Docker and Loopback)
fun getLocalIpAddress(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()

        // Priority 1: Find standard LAN IP (192.168.x.x) that isn't Docker/Bridge
        val lanIp = interfaces.asSequence()
            .filter { !it.isLoopback && it.isUp && !it.name.contains("docker") && !it.name.contains("br-") }
            .flatMap { it.inetAddresses.asSequence() }
            .filter { it is Inet4Address && it.hostAddress.startsWith("192.168.") }
            .map { it.hostAddress }
            .firstOrNull()

        if (lanIp != null) return lanIp

        // Priority 2: Fallback to any non-Docker IPv4 if 192.168 isn't found
        interfaces.asSequence()
            .filter { !it.isLoopback && it.isUp && !it.name.contains("docker") && !it.name.contains("br-") }
            .flatMap { it.inetAddresses.asSequence() }
            .filter { it is Inet4Address }
            .map { it.hostAddress }
            .firstOrNull() ?: "127.0.0.1"

    } catch (e: Exception) {
        "127.0.0.1"
    }
}