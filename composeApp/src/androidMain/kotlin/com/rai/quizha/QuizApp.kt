package com.rai.quizha


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rai.quizha.components.LandingScreen
import com.rai.quizha.components.ResultScreen
import com.rai.quizha.components.ScannerScreen
import com.rai.quizha.model.QrPayload
import com.rai.quizha.navunem.Screen
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Composable
fun QuizApp() {
    var currentScreen by remember { mutableStateOf(Screen.LANDING) }
    var qrData by remember { mutableStateOf<QrPayload?>(null) }
    var finalScore by remember { mutableStateOf(0) }

    // Ktor Client
    val client = remember {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    when (currentScreen) {
        Screen.LANDING -> LandingScreen(
            onStartScan = { currentScreen = Screen.SCANNER }
        )
        Screen.SCANNER -> ScannerScreen(
            onQrScanned = { payload ->
                qrData = payload
                currentScreen = Screen.QUIZ
            }
        )
        Screen.QUIZ -> {
            if (qrData != null) {
                QuizScreen(
                    client = client,
                    qrData = qrData!!,
                    onFinish = { score ->
                        finalScore = score
                        currentScreen = Screen.RESULT
                    }
                )
            } else {
                Text("Error: No QR Data")
            }
        }
        Screen.RESULT -> ResultScreen(
            score = finalScore,
            onHome = { currentScreen = Screen.LANDING }
        )
    }
}