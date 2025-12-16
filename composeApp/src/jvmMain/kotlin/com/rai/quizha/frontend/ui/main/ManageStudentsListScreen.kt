package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rai.quizha.frontend.model.Student
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import kotlinx.coroutines.launch

@Composable
fun ManageStudentsListScreen(
    viewModel: StudentViewModel
) {
    // 1. Collect Data from ViewModel
    val students by viewModel.students.collectAsState()
    val errorMessage by viewModel.error.collectAsState()

    // 2. Local UI State
    var searchQuery by remember { mutableStateOf("") }
    // REMOVED: studentToDelete and studentToEdit states

    // 3. Feedback State (Snackbar)
    val snackbarHostState = remember { SnackbarHostState() }

    // 4. Fetch students when screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchStudents()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            // --- HEADER ---
            Text(
                text = "Student List",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "View and search student records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- SEARCH BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        // Optional: Auto-search or reset on empty
                        if (it.isEmpty()) viewModel.fetchStudents()
                    },
                    label = { Text("Search by Name") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.searchStudents(searchQuery) },
                    enabled = searchQuery.isNotBlank()
                ) {
                    Text("Search")
                }
                FilledTonalIconButton(onClick = {
                    viewModel.fetchStudents()
                    searchQuery = ""
                }) {
                    Icon(Icons.Default.Refresh, "Refresh List")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- ERROR MESSAGE ---
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- STUDENT LIST ---
            if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("No students found.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(students) { student ->
                        StudentItemCard(
                            student = student
                            // REMOVED: onEdit and onDelete callbacks
                        )
                    }
                }
            }
        }
    }

    // REMOVED: Dialogs logic blocks
}

// --- SUB-COMPONENTS ---

@Composable
fun StudentItemCard(
    student: Student
    // REMOVED: onEdit and onDelete parameters
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials Icon
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${student.firstName.firstOrNull() ?: ""}${student.lastName.firstOrNull() ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.firstName} ${student.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${student.course} - ${student.yearlevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${student.department} | ${student.birthdate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // REMOVED: Action Buttons (Edit/Delete IconButtons)
        }
    }
}

// REMOVED: EditStudentDialog Composable