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
import com.rai.quizha.frontend.model.Question
import com.rai.quizha.frontend.viewmodel.ActivityViewModel
import com.rai.quizha.frontend.viewmodel.QuestionsViewModel
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// --- Local UI Helper Model ---
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    var text: MutableState<String> = mutableStateOf(""),
    var optionA: MutableState<String> = mutableStateOf(""),
    var optionB: MutableState<String> = mutableStateOf(""),
    var optionC: MutableState<String> = mutableStateOf(""),
    var optionD: MutableState<String> = mutableStateOf(""),
    var correctOption: MutableState<String> = mutableStateOf("A")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageActivityCreateScreen(
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    studentViewModel: StudentViewModel
) {
    // --- Activity Form State ---
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("pending") }

    // Status Dropdown
    var isStatusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("pending", "ongoing", "paused", "completed")

    // --- Questions List State ---
    val questions = remember { mutableStateListOf<QuestionDraft>() }

    // --- Student Selection State ---
    val allStudents by studentViewModel.students.collectAsState()
    val selectedStudentIds = remember { mutableStateListOf<Long>() }
    var studentSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        studentViewModel.fetchStudents()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (title.isNotBlank() && duration.isNotBlank()) {
                        scope.launch {
                            saveActivityWithQuestionsAndStudents(
                                activityViewModel = activityViewModel,
                                questionsViewModel = questionsViewModel,
                                title = title,
                                duration = duration,
                                status = status,
                                questions = questions,
                                selectedStudentIds = selectedStudentIds,
                                onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("Activity, Questions & Students Saved!") }
                                    title = ""
                                    duration = ""
                                    status = "pending"
                                    questions.clear()
                                    selectedStudentIds.clear()
                                }
                            )
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Please fill in Activity Title and Duration.") }
                    }
                },
                icon = { Icon(Icons.Default.Save, "Save") },
                text = { Text("Save All") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // --- HEADER ---
            item {
                Spacer(Modifier.height(24.dp))
                Text("Create New Activity", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Set up quiz details, add questions, and assign students.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider(Modifier.padding(vertical = 16.dp))
            }

            // --- 1. ACTIVITY DETAILS ---
            item {
                Text("1. Activity Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = duration, onValueChange = { duration = it },
                        label = { Text("Duration (mins)") }, modifier = Modifier.weight(1f),
                        trailingIcon = { Icon(Icons.Default.Timer, null) }, singleLine = true
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = status, onValueChange = {}, readOnly = true,
                            label = { Text("Status") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(Modifier.matchParentSize().clickable { isStatusExpanded = true })
                        DropdownMenu(expanded = isStatusExpanded, onDismissRequest = { isStatusExpanded = false }) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { status = option; isStatusExpanded = false })
                            }
                        }
                    }
                }
            }

            // --- 2. QUESTIONS HEADER ---
            item {
                Divider(Modifier.padding(vertical = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2. Questions (${questions.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = { questions.add(QuestionDraft()) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Question")
                    }
                }
            }

            itemsIndexed(questions) { index, question ->
                QuestionEntryCard(
                    index = index + 1,
                    question = question,
                    onRemove = { questions.removeAt(index) }
                )
            }

            // --- 3. ASSIGN STUDENTS HEADER ---
            item {
                Divider(Modifier.padding(vertical = 16.dp))
                Text("3. Assign Students (${selectedStudentIds.size} selected)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentSearchQuery,
                    onValueChange = { studentSearchQuery = it },
                    label = { Text("Search Student Name") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = {
                        selectedStudentIds.clear()
                        selectedStudentIds.addAll(allStudents.map { it.id })
                    }) { Text("Select All") }

                    TextButton(onClick = { selectedStudentIds.clear() }) { Text("Clear Selection") }
                }
            }

            // --- STUDENT LIST ---
            item {
                val filteredStudents = allStudents.filter {
                    it.firstName.contains(studentSearchQuery, true) ||
                            it.lastName.contains(studentSearchQuery, true)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (filteredStudents.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No students found.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredStudents) { student ->
                                val isSelected = selectedStudentIds.contains(student.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedStudentIds.remove(student.id)
                                            else selectedStudentIds.add(student.id)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedStudentIds.add(student.id)
                                            else selectedStudentIds.remove(student.id)
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("${student.firstName} ${student.lastName}", style = MaterialTheme.typography.bodyLarge)
                                        Text("${student.course} - ${student.yearlevel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UPDATED SAVE LOGIC ---
fun saveActivityWithQuestionsAndStudents(
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    title: String,
    duration: String,
    status: String,
    questions: List<QuestionDraft>,
    selectedStudentIds: List<Long>,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val newActivity = Activity(
        id = 0, title = title, timeduration = duration, status = status,
        createdAt = timestamp, updatedAt = timestamp
    )

    // 1. Create Activity
    activityViewModel.createActivity(newActivity) { newActivityId ->
        if (newActivityId > 0) {
            // 2. Create Questions
            questions.forEach { draft ->
                val newQ = Question(
                    id = 0, activityId = newActivityId,
                    questionText = draft.text.value,
                    optionA = draft.optionA.value, optionB = draft.optionB.value,
                    optionC = draft.optionC.value, optionD = draft.optionD.value,
                    correctOption = draft.correctOption.value
                )
                questionsViewModel.createQuestion(newQ) { }
            }

            // 3. Assign Students to Activity (Save to DB)
            var pendingAssignments = selectedStudentIds.size
            if (pendingAssignments == 0) {
                onSuccess()
            } else {
                selectedStudentIds.forEach { studentId ->
                    activityViewModel.assignStudentToActivity(newActivityId, studentId) { success ->
                        // Simple completion tracking
                        pendingAssignments--
                        if (pendingAssignments == 0) {
                            onSuccess()
                        }
                    }
                }
            }
        } else {
            onError("Failed to create activity.")
        }
    }
}

@Composable
fun QuestionEntryCard(index: Int, question: QuestionDraft, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Question $index", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = question.text.value,
                onValueChange = { question.text.value = it },
                label = { Text("Question Text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 4
            )

            Spacer(Modifier.height(12.dp))
            Text("Options (Select the circle to mark correct answer)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))

            val options = listOf("A" to question.optionA, "B" to question.optionB, "C" to question.optionC, "D" to question.optionD)

            options.forEach { (label, state) ->
                val isSelected = question.correctOption.value == label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { question.correctOption.value = label }
                ) {
                    RadioButton(selected = isSelected, onClick = { question.correctOption.value = label })
                    OutlinedTextField(
                        value = state.value, onValueChange = { state.value = it },
                        label = { Text(if (isSelected) "Option $label (Correct)" else "Option $label") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if(isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}