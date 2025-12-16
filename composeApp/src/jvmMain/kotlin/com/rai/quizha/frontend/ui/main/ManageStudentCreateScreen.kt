package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rai.quizha.frontend.model.Student
import com.rai.quizha.frontend.viewmodel.StudentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStudentCreateScreen(
    viewModel: StudentViewModel
) {
    // --- Form State ---
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") } // Keeping as String YYYY-MM-DD for simplicity

    // Dropdown: Year Level
    var yearLevel by remember { mutableStateOf("") }
    var isYearExpanded by remember { mutableStateOf(false) }
    val yearLevels = listOf("1st", "2nd", "3rd", "4th")

    // Dropdown: Department
    var department by remember { mutableStateOf("") }
    var isDeptExpanded by remember { mutableStateOf(false) }
    val departments = listOf("IICT", "COED", "CAS", "COF", "Education", "Islamic Studies")

    // Dropdown: Course (You might want to filter this based on Department later)
    var course by remember { mutableStateOf("") }
    var isCourseExpanded by remember { mutableStateOf(false) }
    val courses = listOf("BSIT", "BSCS", "BSEd", "BSBio", "BSCivil", "AB Islamic")

    // --- UI Feedback ---
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val errorState by viewModel.error.collectAsState()

    // Listen for errors from ViewModel
    LaunchedEffect(errorState) {
        errorState?.let {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (firstName.isNotBlank() && lastName.isNotBlank() && yearLevel.isNotBlank() && 
                        department.isNotBlank() && course.isNotBlank() && birthdate.isNotBlank()) {
                        
                        // Create Student Object matching your Data Class
                        val newStudent = Student(
                            id = 0, // 0 tells backend to auto-generate ID
                            firstName = firstName,
                            lastName = lastName,
                            yearlevel = yearLevel,
                            department = department,
                            course = course,
                            birthdate = birthdate
                        )

                        viewModel.createStudent(newStudent)

                        scope.launch {
                            snackbarHostState.showSnackbar("Student added successfully!")
                            // Reset Form
                            firstName = ""
                            lastName = ""
                            birthdate = ""
                            yearLevel = ""
                            department = ""
                            course = ""
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Please fill in all fields.") }
                    }
                },
                icon = { Icon(Icons.Default.Save, contentDescription = "Save") },
                text = { Text("Save Student") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // Scrollable container
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Register New Student",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text("Personal Information", style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Birthdate Field (Simple Text Input for YYYY-MM-DD)
            OutlinedTextField(
                value = birthdate,
                onValueChange = { birthdate = it },
                label = { Text("Birthdate (YYYY-MM-DD)") },
                placeholder = { Text("2003-01-01") },
                trailingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Divider(Modifier.padding(vertical = 8.dp))
            Text("Academic Details", style = MaterialTheme.typography.titleMedium)

            // --- Department Dropdown ---
            DropdownField(
                label = "Department",
                options = departments,
                selectedOption = department,
                onOptionSelected = { department = it },
                isExpanded = isDeptExpanded,
                onExpandedChange = { isDeptExpanded = it }
            )

            // --- Course Dropdown ---
            DropdownField(
                label = "Course",
                options = courses,
                selectedOption = course,
                onOptionSelected = { course = it },
                isExpanded = isCourseExpanded,
                onExpandedChange = { isCourseExpanded = it }
            )

            // --- Year Level Dropdown ---
            DropdownField(
                label = "Year Level",
                options = yearLevels,
                selectedOption = yearLevel,
                onOptionSelected = { yearLevel = it },
                isExpanded = isYearExpanded,
                onExpandedChange = { isYearExpanded = it }
            )
            
            // Add extra space at bottom for FAB visibility
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- Reusable Dropdown Component Helper ---
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier
                .fillMaxWidth(),
            enabled = false, // We handle clicks manually via the Box overlay
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        // Overlay transparent clickable box
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onExpandedChange(true) }
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}