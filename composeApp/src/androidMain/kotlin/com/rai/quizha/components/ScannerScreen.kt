package com.rai.quizha.components


import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rai.quizha.model.QrPayload
import kotlinx.serialization.json.Json

@Composable
fun ScannerScreen(onQrScanned: (QrPayload) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        Box(Modifier.fillMaxSize()) {
            CameraPreview(onBarcodeDetected = { rawValue ->
                try {
                    val payload = Json.decodeFromString<QrPayload>(rawValue)
                    onQrScanned(payload)
                } catch (e: Exception) {
                    Log.e("Scanner", "Invalid QR: $rawValue")
                }
            })
            // Overlay
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(250.dp).background(Color.Transparent)) // Hole punch logic requires canvas, keeping simple for now
                Text("Align QR Code within frame", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp))
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required.")
        }
    }
}
