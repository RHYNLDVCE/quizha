// src/jvmMain/kotlin/com/rai/quizha/frontend/ui/main/DashboardScreen.kt
package com.rai.quizha.frontend.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rai.quizha.frontend.model.Activity
import com.rai.quizha.frontend.model.ActivityResult
import com.rai.quizha.frontend.viewmodel.ActivityViewModel
import com.rai.quizha.frontend.viewmodel.StudentViewModel

@Composable
fun DashboardScreen(
    studentViewModel: StudentViewModel,
    activityViewModel: ActivityViewModel,
    serverIp: String,   // <--- NEW PARAM
    serverPort: Int     // <--- NEW PARAM
) {
    // 1. Collect Data
    val students by studentViewModel.students.collectAsState()
    val activitiesResult by activityViewModel.activities.collectAsState()

    // 2. Fetch Data on Load
    LaunchedEffect(Unit) {
        studentViewModel.fetchStudents()
        activityViewModel.fetchAllActivities()
    }

    // 3. Compute Metrics
    val activities = (activitiesResult as? ActivityResult.Success)?.data ?: emptyList()

    val totalStudents = students.size
    val totalActivities = activities.size
    val ongoingActivities = activities.count { it.status == "ongoing" }
    val completedActivities = activities.count { it.status == "completed" }

    // Group Students by Department
    val studentsByDept = students.groupingBy { it.department }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

    // Recent Activities (Sorted by ID descending as proxy for recency, or parsing createdAt)
    val recentActivities = activities.sortedByDescending { it.id }.take(5)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- HEADER & SERVER INFO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard Overview",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Welcome back, Admin. Here is what's happening today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // SERVER IP DISPLAY CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Server Address",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$serverIp:$serverPort",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // --- METRIC CARDS ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricCard(
                    title = "Total Students",
                    value = totalStudents.toString(),
                    icon = Icons.Default.People,
                    color = Color(0xFF5E35B1), // Deep Purple
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Quizzes",
                    value = ongoingActivities.toString(),
                    icon = Icons.Default.Timer,
                    color = Color(0xFF00897B), // Teal
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Completed",
                    value = completedActivities.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF43A047), // Green
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Activities",
                    value = totalActivities.toString(),
                    icon = Icons.Default.LibraryBooks,
                    color = Color(0xFF1E88E5), // Blue
                    modifier = Modifier.weight(1f)
                )
            }

            // --- LOWER SECTION (Split View) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // LEFT COLUMN: Recent Activities
                Card(
                    modifier = Modifier.weight(2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Recent Activities",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        if (recentActivities.isEmpty()) {
                            Text("No activities found.", color = Color.Gray)
                        } else {
                            recentActivities.forEach { activity ->
                                RecentActivityRow(activity)
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha=0.3f))
                            }
                        }

                        TextButton(
                            onClick = { /* Could navigate to full list */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("View All Activity History")
                        }
                    }
                }

                // RIGHT COLUMN: Department Stats
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Students by Dept.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        if (studentsByDept.isEmpty()) {
                            Text("No student data available.", color = Color.Gray)
                        } else {
                            studentsByDept.forEach { (dept, count) ->
                                DepartmentStatRow(dept, count, totalStudents)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RecentActivityRow(activity: Activity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Dot
        val statusColor = when(activity.status) {
            "ongoing" -> Color(0xFF00C853)
            "completed" -> Color.Gray
            "paused" -> Color(0xFFFFAB00)
            else -> Color.Blue
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(statusColor)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(activity.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Created: ${activity.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Surface(
            color = statusColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = activity.status.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DepartmentStatRow(dept: String, count: Int, total: Int) {
    val percentage = if (total > 0) count.toFloat() / total.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dept, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = percentage,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}