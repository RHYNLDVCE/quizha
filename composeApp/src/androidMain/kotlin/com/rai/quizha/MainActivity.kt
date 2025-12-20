// kotlin/com/rai/quizha/MainActivity.kt
package com.rai.quizha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// --- CONFIG ---
// Changed from const val to a mutable object so the UI can update it
object ServerConfig {
    var ipAddress: String = "10.150.8.45" // Default
    var port: String = "8080"             // Default

    // Helper to get the full URL
    val baseUrl: String
        get() = "http://$ipAddress:$port"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizApp()
        }
    }
}