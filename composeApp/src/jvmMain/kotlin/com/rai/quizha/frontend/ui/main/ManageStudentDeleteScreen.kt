package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rai.quizha.frontend.model.Student
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import kotlinx.coroutines.launch

@Composable
fun ManageStudentDeleteScreen(
    viewModel: StudentViewModel
) {
    val students by viewModel.students.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    // We use a dialog for the final confirmation, so we store the target student here
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Text("Delete Student", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
            Text("Search for a student to permanently remove their record.", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Name") },
                trailingIcon = { 
                    IconButton(onClick = { viewModel.searchStudents(searchQuery) }) {
                        Icon(Icons.Default.Search, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // List of Search Results
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students) { student ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${student.firstName} ${student.lastName}", style = MaterialTheme.typography.titleMedium)
                                Text("${student.course} - ${student.yearlevel}", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            // Delete Button
                            Button(
                                onClick = { studentToDelete = student },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Confirmation Dialog ---
    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Deletion") },
            text = { 
                Text("Are you absolutely sure you want to delete ${studentToDelete?.firstName} ${studentToDelete?.lastName}? This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete?.let {
                            viewModel.deleteStudent(it.id)
                            scope.launch { snackbarHostState.showSnackbar("Record deleted successfully.") }
                        }
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}