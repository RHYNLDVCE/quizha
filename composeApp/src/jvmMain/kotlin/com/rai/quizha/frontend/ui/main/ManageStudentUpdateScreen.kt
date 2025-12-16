package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rai.quizha.frontend.model.Student
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import kotlinx.coroutines.launch

@Composable
fun ManageStudentUpdateScreen(
    viewModel: StudentViewModel
) {
    // 1. Collect Data
    val students by viewModel.students.collectAsState()
    
    // 2. UI State
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    
    // Form State (initialized when a student is selected)
    var firstName by remember(selectedStudent) { mutableStateOf(selectedStudent?.firstName ?: "") }
    var lastName by remember(selectedStudent) { mutableStateOf(selectedStudent?.lastName ?: "") }
    var course by remember(selectedStudent) { mutableStateOf(selectedStudent?.course ?: "") }
    var yearLevel by remember(selectedStudent) { mutableStateOf(selectedStudent?.yearlevel ?: "") }
    var department by remember(selectedStudent) { mutableStateOf(selectedStudent?.department ?: "") }
    var birthdate by remember(selectedStudent) { mutableStateOf(selectedStudent?.birthdate ?: "") }

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
            Text("Update Student", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Search for a student, select them, and modify their details.", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            // --- STEP 1: SEARCH BAR ---
            if (selectedStudent == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Student by Name") },
                    trailingIcon = { 
                        IconButton(onClick = { viewModel.searchStudents(searchQuery) }) {
                            Icon(Icons.Default.Search, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Search Results List
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(students) { student ->
                        Card(
                            onClick = { selectedStudent = student }, // Select the student
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("${student.firstName} ${student.lastName}", style = MaterialTheme.typography.titleMedium)
                                Text("${student.course} - ${student.yearlevel}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                // --- STEP 2: EDIT FORM (Visible only when student is selected) ---
                
                // Header with "Cancel/Back" button
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Editing: ${selectedStudent?.firstName}", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { selectedStudent = null }) { Text("Change Selection") }
                }
                
                Divider(Modifier.padding(vertical = 8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName, onValueChange = { firstName = it },
                            label = { Text("First Name") }, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lastName, onValueChange = { lastName = it },
                            label = { Text("Last Name") }, modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Course") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = yearLevel, onValueChange = { yearLevel = it }, label = { Text("Year Level") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = birthdate, onValueChange = { birthdate = it }, label = { Text("Birthdate") }, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            selectedStudent?.let { current ->
                                val updated = current.copy(
                                    firstName = firstName, lastName = lastName,
                                    course = course, yearlevel = yearLevel,
                                    department = department, birthdate = birthdate
                                )
                                viewModel.updateStudent(updated) { success ->
                                    scope.launch {
                                        if (success) {
                                            snackbarHostState.showSnackbar("Student Updated Successfully")
                                            selectedStudent = null // Go back to search
                                        } else {
                                            snackbarHostState.showSnackbar("Update Failed")
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}