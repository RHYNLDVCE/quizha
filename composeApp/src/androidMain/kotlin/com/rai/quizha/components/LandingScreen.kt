// kotlin/com/rai/quizha/components/LandingScreen.kt
package com.rai.quizha.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rai.quizha.ServerConfig // Import the config from MainActivity

@Composable
fun LandingScreen(onStartScan: () -> Unit) {
    // Local state initialized from the global config
    var ip by remember { mutableStateOf(ServerConfig.ipAddress) }
    var port by remember { mutableStateOf(ServerConfig.port) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "QuizHa Student",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(32.dp))

        // --- Connection String Editor ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Server Connection Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))

                // IP Address Input
                OutlinedTextField(
                    value = ip,
                    onValueChange = {
                        ip = it
                        ServerConfig.ipAddress = it // Update Global Config
                    },
                    label = { Text("IP Address") },
                    placeholder = { Text("e.g. 192.168.1.5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Port Input
                OutlinedTextField(
                    value = port,
                    onValueChange = { input ->
                        // Only allow numeric input
                        if (input.all { it.isDigit() }) {
                            port = input
                            ServerConfig.port = input // Update Global Config
                        }
                    },
                    label = { Text("Port") },
                    placeholder = { Text("8080") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
        // --------------------------------

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Scan your assigned QR code to start the activity.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(8.dp))
            Text("SCAN QR CODE")
        }
    }
}