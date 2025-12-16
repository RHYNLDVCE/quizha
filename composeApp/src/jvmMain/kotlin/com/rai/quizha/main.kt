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
import com.rai.quizha.frontend.viewmodel.QuestionsViewModel // <--- Import this
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import com.rai.quizha.server.model.JwtConfig
import com.rai.quizha.server.entry.startEmbeddedServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun main() = application {

    val appScope = CoroutineScope(Dispatchers.IO)

    // 1. Start server (Backend)
    appScope.launch {
        val jwtConfig = JwtConfig(
            secret = "super-secret-key",
            issuer = "quizzy-server",
            audience = "quizzy-client"
        )
        startEmbeddedServer(port = 8080, jwtConfig = jwtConfig)
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

    val baseUrl = "http://localhost:8080"

    // Admin ViewModel (Login)
    val adminViewModel = remember { AdminViewModel(client, baseUrl) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Quizha - Admin Panel"
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

                // Questions ViewModel (NEW)
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
                                questionsViewModel = questionsViewModel // <--- Passed here
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