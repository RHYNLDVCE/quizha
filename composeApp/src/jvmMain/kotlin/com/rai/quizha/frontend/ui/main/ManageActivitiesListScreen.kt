package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.rai.quizha.frontend.model.Activity
import com.rai.quizha.frontend.model.ActivityResult
import com.rai.quizha.frontend.model.LeaderboardEntry
import com.rai.quizha.db.model.StudentAnswer
import com.rai.quizha.frontend.viewmodel.ActivityViewModel
import com.rai.quizha.frontend.viewmodel.QuestionsViewModel
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage

@Composable
fun ManageActivitiesListScreen(
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel
) {
    // 1. Collect Data
    val activityState by activityViewModel.activities.collectAsState()

    // Collect the Timer State
    val remainingTimes by activityViewModel.remainingTimes.collectAsState()

    // 2. Local State
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }

    // NAVIGATION STATE
    var selectedActivity by remember { mutableStateOf<Activity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 3. Fetch on load
    LaunchedEffect(Unit) {
        activityViewModel.fetchAllActivities()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (selectedActivity != null) {
                // ==========================================
                // VIEW 1: DETAIL SCREEN
                // ==========================================
                // Ensure we use the latest activity object from the state to capture Status updates (e.g. ongoing -> completed)
                val currentActivity = (activityState as? ActivityResult.Success)
                    ?.data?.find { it.id == selectedActivity!!.id }
                    ?: selectedActivity!!

                // Get timer for this specific activity
                val timeLeft = remainingTimes[currentActivity.id]

                ActivityDetailReadOnlyScreen(
                    activity = currentActivity,
                    remainingTime = timeLeft,
                    activityViewModel = activityViewModel,
                    questionsViewModel = questionsViewModel,
                    onBack = { selectedActivity = null }
                )
            } else {
                // ==========================================
                // VIEW 2: LIST SCREEN
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Activity List",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tap an item to view details.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { activityViewModel.fetchAllActivities() }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (val result = activityState) {
                        is ActivityResult.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is ActivityResult.Error -> {
                            Text("Error: ${result.message}", color = MaterialTheme.colorScheme.error)
                        }
                        is ActivityResult.Success -> {
                            val activities = result.data
                            if (activities.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No activities found.")
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(activities) { activity ->
                                        val timeLeft = remainingTimes[activity.id]

                                        ActivityItemCard(
                                            activity = activity,
                                            remainingTime = timeLeft,
                                            onClick = { selectedActivity = activity },
                                            onEdit = {
                                                if (activity.status == "pending") {
                                                    activityToEdit = activity
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Cannot edit started/completed activities.")
                                                    }
                                                }
                                            },
                                            onDelete = { activityToDelete = activity }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activityToDelete != null) {
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Delete Activity?") },
            text = { Text("Are you sure you want to delete '${activityToDelete?.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        activityToDelete?.let { activityViewModel.deleteActivity(it.id) }
                        activityToDelete = null
                        scope.launch { snackbarHostState.showSnackbar("Activity deleted.") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (activityToEdit != null) {
        EditActivityDialog(
            activity = activityToEdit!!,
            onDismiss = { activityToEdit = null },
            onConfirm = { updatedActivity ->
                activityViewModel.updateActivity(updatedActivity)
                activityToEdit = null
                scope.launch { snackbarHostState.showSnackbar("Update request sent.") }
            }
        )
    }
}

// ==========================================
// UPDATED DETAIL SCREEN (WITH LEADERBOARD)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailReadOnlyScreen(
    activity: Activity,
    remainingTime: Int?,
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    onBack: () -> Unit
) {
    val questions by questionsViewModel.questions.collectAsState()
    val assignedStudents by activityViewModel.assignedStudents.collectAsState()
    val leaderboard by activityViewModel.leaderboard.collectAsState()
    val leaderboardError by activityViewModel.leaderboardError.collectAsState() // Watch for errors

    // --- State for Student Detail Dialog ---
    var selectedEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var studentAnswers by remember { mutableStateOf<List<StudentAnswer>>(emptyList()) }
    var isFetchingAnswers by remember { mutableStateOf(false) }

    // --- QR Code State ---
    var qrDialogData by remember { mutableStateOf<QrData?>(null) }
    var isGeneratingQr by remember { mutableStateOf(false) }

    // Load Initial Data
    // CRITICAL FIX: Include activity.status in keys so this re-runs when you click "Stop" and status becomes "completed"
    LaunchedEffect(activity.id, activity.status) {
        questionsViewModel.loadQuestionsByActivityId(activity.id)
        if (activity.status == "completed") {
            activityViewModel.fetchLeaderboard(activity.id)
        } else {
            activityViewModel.loadAssignedStudents(activity.id)
        }
    }

    // Fetch answers when a student is selected from leaderboard
    LaunchedEffect(selectedEntry) {
        if (selectedEntry != null) {
            isFetchingAnswers = true
            activityViewModel.fetchStudentAnswers(selectedEntry!!.resultId) { answers ->
                studentAnswers = answers
                isFetchingAnswers = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("Activity Details", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.weight(1f))

            // Header Status/Timer Chip
            if (activity.status == "ongoing" && remainingTime != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatSecondsToTime(remainingTime),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Divider(Modifier.padding(vertical = 16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. INFO SECTION (Always visible)
            item {
                Text("Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                // Large Timer Display
                if (activity.status == "ongoing" && remainingTime != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TIME REMAINING", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha=0.8f))
                            Text(
                                text = formatSecondsToTime(remainingTime),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }

                ReadOnlyField("Title", activity.title)
                ReadOnlyField("Duration", "${activity.timeduration} mins")
                ReadOnlyField("Status", activity.status.uppercase())

                Spacer(Modifier.height(16.dp))

                // Control Buttons
                if (activity.status == "pending") {
                    Button(
                        onClick = {
                            val durationInt = activity.timeduration.toIntOrNull() ?: 0
                            if (durationInt > 0) {
                                activityViewModel.startActivity(activity.id, durationInt)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Activity Now")
                    }
                } else if (activity.status == "ongoing") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { activityViewModel.pauseActivity(activity.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)), // Amber
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pause")
                        }
                        Button(
                            onClick = { activityViewModel.completeActivity(activity.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                } else if (activity.status == "paused") {
                    Button(
                        onClick = { activityViewModel.resumeActivity(activity.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resume Activity")
                    }
                }
            }

            // 2. CONDITIONAL CONTENT
            if (activity.status == "completed") {
                // --- LEADERBOARD VIEW ---
                item {
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700))
                        Spacer(Modifier.width(8.dp))
                        Text("Leaderboard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        // Manual Refresh Button
                        IconButton(onClick = { activityViewModel.fetchLeaderboard(activity.id) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Leaderboard")
                        }
                    }
                    Text("Click a student to view their answers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                }

                if (leaderboardError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Error: $leaderboardError",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else if (leaderboard.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No results found yet.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    itemsIndexed(leaderboard) { index, entry ->
                        Card(
                            onClick = { selectedEntry = entry },
                            colors = CardDefaults.cardColors(containerColor = if(index == 0) Color(0xFFFFD700).copy(alpha=0.1f) else MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${index + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(40.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${entry.firstName} ${entry.lastName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${entry.score} pts",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

            } else {
                // --- STANDARD VIEW (Questions + Students) ---
                item {
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Text("Questions (${questions.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                items(questions) { q ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(q.questionText, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("A: ${q.optionA}", style = MaterialTheme.typography.bodySmall)
                            Text("B: ${q.optionB}", style = MaterialTheme.typography.bodySmall)
                            Text("C: ${q.optionC}", style = MaterialTheme.typography.bodySmall)
                            Text("D: ${q.optionD}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Answer: ${q.correctOption}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                item {
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Text("Assigned Students (${assignedStudents.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                if (assignedStudents.isEmpty()) {
                    item { Text("No students assigned.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                } else {
                    items(assignedStudents) { student ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${student.firstName} ${student.lastName}", fontWeight = FontWeight.SemiBold)
                                Text("${student.course} - ${student.yearlevel}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            // QR Generation
                            FilledTonalIconButton(
                                onClick = {
                                    isGeneratingQr = true
                                    activityViewModel.generateStudentToken(activity.id, student.id) { token ->
                                        isGeneratingQr = false
                                        if (token != null) {
                                            val payload = """{"activityId":${activity.id},"studentId":${student.id},"token":"$token"}"""
                                            val bitmap = generateQrCode(payload)
                                            if (bitmap != null) {
                                                qrDialogData = QrData("${student.firstName} ${student.lastName}", bitmap)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = "Generate QR")
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }

    // --- STUDENT ANSWER DETAILS DIALOG ---
    if (selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            modifier = Modifier.fillMaxSize().padding(16.dp),
            title = {
                Column {
                    Text("Result Details")
                    Text("${selectedEntry?.firstName} ${selectedEntry?.lastName} - Score: ${selectedEntry?.score}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                if (isFetchingAnswers) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(questions) { q ->
                            val answer = studentAnswers.find { it.questionId == q.id }
                            val userAnswer = answer?.selectedOption ?: "None"
                            val isCorrect = userAnswer == q.correctOption

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ),
                                border = BorderStroke(1.dp, if(isCorrect) Color(0xFF4CAF50) else Color(0xFFEF5350))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(q.questionText, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Row {
                                        Text("Correct: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(q.correctOption, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                    }
                                    Row {
                                        Text("Student Answer: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(userAnswer, style = MaterialTheme.typography.bodySmall,
                                            color = if(isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedEntry = null }) {
                    Text("Close")
                }
            }
        )
    }

    // --- QR DIALOG ---
    if (qrDialogData != null) {
        AlertDialog(
            onDismissRequest = { qrDialogData = null },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Student Access QR")
                    Text(qrDialogData!!.studentName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = qrDialogData!!.bitmap, contentDescription = "QR Code", modifier = Modifier.size(250.dp))
                    Text("Scan to enter activity.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { qrDialogData = null }) { Text("Close") } }
        )
    }

    if (isGeneratingQr) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

// --- HELPER CLASSES & FUNCTIONS ---

data class QrData(
    val studentName: String,
    val bitmap: ImageBitmap
)

@Composable
fun ReadOnlyField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ActivityItemCard(
    activity: Activity,
    remainingTime: Int?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if(activity.status == "ongoing") MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when(activity.status) {
                "ongoing" -> MaterialTheme.colorScheme.primary
                "completed" -> MaterialTheme.colorScheme.secondary
                "paused" -> Color(0xFFFFA000) // Amber
                else -> MaterialTheme.colorScheme.outline
            }

            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = statusColor.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = activity.status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = statusColor
                        )
                    }

                    if (activity.status == "ongoing" && remainingTime != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatSecondsToTime(remainingTime),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }
                }

                if (activity.status != "ongoing") {
                    Text("${activity.timeduration} mins", style = MaterialTheme.typography.bodySmall)
                }
            }

            if(activity.status == "pending") {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun formatSecondsToTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun EditActivityDialog(
    activity: Activity,
    onDismiss: () -> Unit,
    onConfirm: (Activity) -> Unit
) {
    var title by remember { mutableStateOf(activity.title) }
    var duration by remember { mutableStateOf(activity.timeduration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(activity.copy(title = title, timeduration = duration)) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Generates a QR Code ImageBitmap from a String using ZXing.
 * Requires dependencies: 'com.google.zxing:core' and 'com.google.zxing:javase'
 */
fun generateQrCode(data: String): ImageBitmap? {
    return try {
        val writer = QRCodeWriter()
        // Generate BitMatrix
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        // Convert to BufferedImage using JavaSE helper
        val bufferedImage: BufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)
        // Convert to Compose ImageBitmap
        bufferedImage.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}