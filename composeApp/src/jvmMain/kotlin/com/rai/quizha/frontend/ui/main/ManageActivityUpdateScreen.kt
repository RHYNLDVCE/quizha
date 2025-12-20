package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rai.quizha.frontend.model.Activity
import com.rai.quizha.frontend.model.ActivityResult
import com.rai.quizha.frontend.model.Question
import com.rai.quizha.frontend.viewmodel.ActivityViewModel
import com.rai.quizha.frontend.viewmodel.QuestionsViewModel
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageActivityUpdateScreen(
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    studentViewModel: StudentViewModel
) {
    // --- Global State ---
    val activitiesState by activityViewModel.activities.collectAsState()
    val existingQuestions by questionsViewModel.questions.collectAsState()
    val allStudents by studentViewModel.students.collectAsState()
    val assignedStudents by activityViewModel.assignedStudents.collectAsState()

    // --- UI State ---
    var selectedActivity by remember { mutableStateOf<Activity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        activityViewModel.fetchAllActivities()
        studentViewModel.fetchStudents()
    }

    // --- Form State ---
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("pending") }
    val draftQuestions = remember { mutableStateListOf<QuestionDraft>() }

    // Student Selection State (For adding NEW students)
    val selectedStudentIds = remember { mutableStateListOf<Long>() }
    var studentSearchQuery by remember { mutableStateOf("") }

    // --- Populate Form when Activity Selected ---
    LaunchedEffect(selectedActivity) {
        if (selectedActivity != null) {
            title = selectedActivity!!.title
            duration = selectedActivity!!.timeduration
            status = selectedActivity!!.status
            questionsViewModel.loadQuestionsByActivityId(selectedActivity!!.id)
            activityViewModel.loadAssignedStudents(selectedActivity!!.id)
            selectedStudentIds.clear()
        }
    }

    // --- Populate Questions List ---
    LaunchedEffect(existingQuestions) {
        if (selectedActivity != null && existingQuestions.isNotEmpty()) {
            // FIX: Ensure we only populate if the draft list is empty to prevent duplicates
            if (draftQuestions.isEmpty()) {
                existingQuestions.forEach { q ->
                    draftQuestions.add(
                        QuestionDraft(
                            // FIX: Use the actual DB ID, converted to String
                            id = q.id.toString(),
                            text = mutableStateOf(q.questionText),
                            optionA = mutableStateOf(q.optionA),
                            optionB = mutableStateOf(q.optionB),
                            optionC = mutableStateOf(q.optionC),
                            optionD = mutableStateOf(q.optionD),
                            correctOption = mutableStateOf(q.correctOption)
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedActivity != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (title.isNotBlank() && duration.isNotBlank()) {
                            scope.launch {
                                updateActivityWithQuestionsAndStudents(
                                    activityViewModel,
                                    questionsViewModel,
                                    selectedActivity!!,
                                    title,
                                    duration,
                                    status,
                                    draftQuestions,
                                    selectedStudentIds,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Activity Updated & Students Assigned!")
                                            selectedActivity = null
                                            draftQuestions.clear()
                                            selectedStudentIds.clear()
                                        }
                                    },
                                    onError = { scope.launch { snackbarHostState.showSnackbar("Error: $it") } }
                                )
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Save, "Update") },
                    text = { Text("Save Changes") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (selectedActivity == null) {
                // ==========================
                // VIEW 1: SELECT ACTIVITY
                // ==========================
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Update Activity", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Select an activity to edit details, questions, or participants.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by Title") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    Spacer(Modifier.height(8.dp))

                    when (val result = activitiesState) {
                        is ActivityResult.Success -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val filtered = result.data.filter { it.title.contains(searchQuery, ignoreCase = true) }
                                items(filtered) { activity ->
                                    Card(
                                        onClick = {
                                            draftQuestions.clear()
                                            selectedActivity = activity
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(16.dp))
                                            Column {
                                                Text(activity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Status: ${activity.status} | Duration: ${activity.timeduration}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            } else {
                // ==========================
                // VIEW 2: EDIT FORM
                // ==========================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedActivity = null }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                            Text("Editing: ${selectedActivity?.title}", style = MaterialTheme.typography.headlineSmall)
                        }
                        Divider(Modifier.padding(vertical = 8.dp))
                    }

                    // --- 1. ACTIVITY DETAILS ---
                    item {
                        Text("1. Activity Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, modifier = Modifier.weight(1f))

                            var expanded by remember { mutableStateOf(false) }
                            val options = listOf("pending", "ongoing", "paused", "completed")
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = status, onValueChange = {}, readOnly = true, label = { Text("Status") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(Modifier.matchParentSize().clickable { expanded = true })
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    options.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { status = opt; expanded = false })
                                    }
                                }
                            }
                        }
                    }

                    // --- 2. QUESTIONS LIST ---
                    item {
                        Divider(Modifier.padding(vertical = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Questions (${draftQuestions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Button(onClick = { draftQuestions.add(QuestionDraft()) }) {
                                Icon(Icons.Default.Add, null)
                                Text("Add")
                            }
                        }
                    }

                    itemsIndexed(draftQuestions) { index, draft ->
                        QuestionEntryCard(index = index + 1, question = draft, onRemove = { draftQuestions.removeAt(index) })
                    }

                    // --- 3. ADD NEW STUDENTS ---
                    item {
                        Divider(Modifier.padding(vertical = 16.dp))
                        Text("3. Add New Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Select unassigned students to add them.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = studentSearchQuery,
                            onValueChange = { studentSearchQuery = it },
                            label = { Text("Search Student Name") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Scrollable selection box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            val filteredStudents = allStudents.filter {
                                it.firstName.contains(studentSearchQuery, true) ||
                                        it.lastName.contains(studentSearchQuery, true)
                            }

                            if (filteredStudents.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No students match search.")
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredStudents) { student ->
                                        // Check if already assigned
                                        val isAlreadyAssigned = assignedStudents.any { it.id == student.id }
                                        val isSelected = selectedStudentIds.contains(student.id)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isAlreadyAssigned) {
                                                    if (isSelected) selectedStudentIds.remove(student.id)
                                                    else selectedStudentIds.add(student.id)
                                                }
                                                .background(if (isAlreadyAssigned) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f) else Color.Transparent)
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected || isAlreadyAssigned,
                                                onCheckedChange = { checked ->
                                                    if (!isAlreadyAssigned) {
                                                        if (checked) selectedStudentIds.add(student.id)
                                                        else selectedStudentIds.remove(student.id)
                                                    }
                                                },
                                                enabled = !isAlreadyAssigned
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "${student.firstName} ${student.lastName}",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isAlreadyAssigned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isAlreadyAssigned) {
                                                    Text("(Already Assigned)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    // --- 4. LIST OF CURRENTLY ASSIGNED STUDENTS ---
                    item {
                        Divider(Modifier.padding(vertical = 16.dp))
                        Text("4. Assigned Participants (${assignedStudents.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (assignedStudents.isEmpty()) {
                        item {
                            Text("No students currently assigned to this activity.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        items(assignedStudents) { student ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("${student.firstName} ${student.lastName}", fontWeight = FontWeight.Bold)
                                        Text("${student.course} - ${student.yearlevel}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    // Option to remove could be added here
                                    IconButton(onClick = {
                                        // Optional: Call remove logic immediately or batch it
                                        activityViewModel.removeStudentFromActivity(selectedActivity!!.id, student.id)
                                    }) {
                                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UPDATED SAFE LOGIC (Fix for saving students & preventing answer deletion) ---
fun updateActivityWithQuestionsAndStudents(
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    originalActivity: Activity,
    newTitle: String,
    newDuration: String,
    newStatus: String,
    questions: List<QuestionDraft>,
    selectedStudentIds: List<Long>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // 1. Update Activity Object
    val updatedActivity = originalActivity.copy(
        title = newTitle,
        timeduration = newDuration,
        status = newStatus,
        updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    )

    activityViewModel.updateActivity(updatedActivity)

    // 2. Handle Questions Non-Destructively

    // A. Identify Original vs Current IDs
    val originalQuestions = questionsViewModel.questions.value
    val originalIds = originalQuestions.map { it.id }.toSet()
    val currentDraftIds = questions.mapNotNull { it.id.toLongOrNull() }.toSet()

    // B. Delete only questions that were explicitly REMOVED in the UI
    val idsToDelete = originalIds - currentDraftIds
    idsToDelete.forEach { id ->
        questionsViewModel.deleteQuestion(id) {}
    }

    // C. Update Existing or Create New
    questions.forEach { draft ->
        val draftId = draft.id.toLongOrNull() ?: 0L

        val questionData = Question(
            id = draftId,
            activityId = originalActivity.id,
            questionText = draft.text.value,
            optionA = draft.optionA.value,
            optionB = draft.optionB.value,
            optionC = draft.optionC.value,
            optionD = draft.optionD.value,
            correctOption = draft.correctOption.value
        )

        if (draftId > 0 && originalIds.contains(draftId)) {
            // ID exists in DB -> UPDATE
            questionsViewModel.updateQuestion(questionData) {}
        } else {
            // ID is 0 or new -> CREATE
            questionsViewModel.createQuestion(questionData) {}
        }
    }

    // 3. Assign New Students (Database)
    // (Only processes newly selected students, existing assignments remain untouched)
    if (selectedStudentIds.isEmpty()) {
        onSuccess()
    } else {
        var pendingRequests = selectedStudentIds.size
        // Simple counter to trigger onSuccess only when all API calls finish
        selectedStudentIds.forEach { studentId ->
            activityViewModel.assignStudentToActivity(originalActivity.id, studentId) { success ->
                pendingRequests--
                if (pendingRequests == 0) {
                    onSuccess()
                }
            }
        }
    }
}