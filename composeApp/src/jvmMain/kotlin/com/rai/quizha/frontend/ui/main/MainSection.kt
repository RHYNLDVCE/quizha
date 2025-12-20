// src/jvmMain/kotlin/com/rai/quizha/frontend/ui/main/MainSection.kt
package com.rai.quizha.frontend.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rai.quizha.frontend.viewmodel.ActivityViewModel
import com.rai.quizha.frontend.viewmodel.QuestionsViewModel
import com.rai.quizha.frontend.viewmodel.StudentViewModel

// =========================================================================
// 0. Theme Colors
// =========================================================================
private val BrandPink = Color(0xFFFF0266)
private val BrandLightPink = Color(0xFFFEEBF1)

// =========================================================================
// 1. Navigation Structure Definitions
// =========================================================================

sealed class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

sealed class TopLevelNavItem(title: String, icon: ImageVector, route: String) :
    NavItem(title, icon, route) {
    data object Dashboard : TopLevelNavItem(
        "Dashboard", Icons.Rounded.Dashboard, "DASHBOARD_ROUTE"
    )
    data object Students : TopLevelNavItem(
        "Manage Students", Icons.Default.Person, "STUDENTS_BASE_ROUTE"
    )
    data object Activities : TopLevelNavItem(
        "Manage Activities", Icons.Default.List, "ACTIVITIES_BASE_ROUTE"
    )
    // NEW: Settings Top Level Item
    data object Settings : TopLevelNavItem(
        "Settings", Icons.Default.Settings, "SETTINGS_BASE_ROUTE"
    )
}

sealed class SubNavItem(title: String, icon: ImageVector, route: String) :
    NavItem(title, icon, route) {

    // --- Student Sub Items ---
    data object StudentList : SubNavItem(
        "Student List", Icons.Default.PeopleAlt, "STUDENTS_LIST_ROUTE"
    )
    data object StudentCreate : SubNavItem(
        "Add New Student", Icons.Default.PersonAdd, "STUDENTS_CREATE_ROUTE"
    )
    data object StudentUpdate : SubNavItem(
        "Update Student", Icons.Default.Edit, "STUDENTS_UPDATE_ROUTE"
    )
    data object StudentDelete : SubNavItem(
        "Delete Student", Icons.Default.Delete, "STUDENTS_DELETE_ROUTE"
    )

    // --- Activity Sub Items ---
    data object ActivityList : SubNavItem(
        "Activity List", Icons.Default.ViewList, "ACTIVITIES_LIST_ROUTE"
    )
    data object ActivityCreate : SubNavItem(
        "Create Activity", Icons.Default.AddBox, "ACTIVITIES_CREATE_ROUTE"
    )
    data object ActivityUpdate : SubNavItem(
        "Update Activity", Icons.Default.Edit, "ACTIVITIES_UPDATE_ROUTE"
    )

    // --- Settings Sub Items ---
    // NEW: Logout Action (Hidden inside Settings)
    data object Logout : SubNavItem(
        "Logout", Icons.AutoMirrored.Filled.ExitToApp, "LOGOUT_ACTION"
    )
}

val allDestinations = listOf(
    TopLevelNavItem.Dashboard,
    SubNavItem.StudentList,
    SubNavItem.StudentCreate,
    SubNavItem.StudentUpdate,
    SubNavItem.StudentDelete,
    SubNavItem.ActivityList,
    SubNavItem.ActivityCreate,
    SubNavItem.ActivityUpdate,
    SubNavItem.Logout
)

// =========================================================================
// 2. Expandable Drawer Item with SMOOTH ANIMATIONS & MODERN STYLING
// =========================================================================

@Composable
fun ExpandableNavigationDrawerItem(
    parentItem: TopLevelNavItem,
    subItems: List<SubNavItem>,
    currentSelectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    // Check if any child is selected to highlight the parent group
    val isChildSelected = subItems.any { it.route == currentSelectedRoute }

    var isExpanded by remember { mutableStateOf(isChildSelected) }

    // Auto-expand if a child becomes active
    LaunchedEffect(currentSelectedRoute) {
        if (subItems.any { it.route == currentSelectedRoute }) {
            isExpanded = true
        }
    }

    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "ArrowRotation"
    )

    NavigationDrawerItem(
        label = {
            Text(
                parentItem.title,
                fontWeight = if(isChildSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        icon = {
            Icon(
                parentItem.icon,
                contentDescription = null,
                tint = if(isChildSelected) BrandPink else LocalContentColor.current
            )
        },
        selected = isChildSelected,
        onClick = { isExpanded = !isExpanded },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BrandPink.copy(alpha = 0.08f),
            selectedTextColor = BrandPink,
            unselectedContainerColor = Color.Transparent
        ),
        badge = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if(isChildSelected) BrandPink else Color.Gray,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationState)
            )
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
        ) {
            subItems.forEach { subItem ->
                val isSelected = currentSelectedRoute == subItem.route
                NavigationDrawerItem(
                    label = {
                        Text(
                            subItem.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            subItem.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        onItemSelected(subItem.route)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = BrandPink,
                        selectedTextColor = Color.White,
                        selectedIconColor = Color.White,
                        unselectedTextColor = Color.Gray,
                        unselectedIconColor = Color.Gray
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(48.dp)
                )
            }
        }
    }
}

// =========================================================================
// 3. Main Screen Composable (Permanent Drawer)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authToken: String,
    studentViewModel: StudentViewModel,
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    serverIp: String,   // <--- NEW PARAM
    serverPort: Int,    // <--- NEW PARAM
    onLogout: () -> Unit
) {
    var selectedRoute by remember { mutableStateOf(TopLevelNavItem.Dashboard.route) }
    val currentTitle = allDestinations.find { it.route == selectedRoute }?.title ?: "Main Application"

    val studentSubItems = listOf(
        SubNavItem.StudentList,
        SubNavItem.StudentCreate,
        SubNavItem.StudentUpdate,
        SubNavItem.StudentDelete
    )

    val activitySubItems = listOf(
        SubNavItem.ActivityList,
        SubNavItem.ActivityCreate,
        SubNavItem.ActivityUpdate
    )

    // Settings only contains Logout
    val settingsSubItems = listOf(
        SubNavItem.Logout
    )

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // --- Header ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(BrandPink, Color(0xFFFF5C8D))
                            )
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "QuizHa Admin",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Administrator Panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- 1. Dashboard ---
                NavigationDrawerItem(
                    icon = { Icon(TopLevelNavItem.Dashboard.icon, contentDescription = null) },
                    label = {
                        Text(
                            TopLevelNavItem.Dashboard.title,
                            fontWeight = if(selectedRoute == TopLevelNavItem.Dashboard.route) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = selectedRoute == TopLevelNavItem.Dashboard.route,
                    onClick = {
                        selectedRoute = TopLevelNavItem.Dashboard.route
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = BrandPink.copy(alpha = 0.1f),
                        selectedTextColor = BrandPink,
                        selectedIconColor = BrandPink
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp).background(Color.Gray.copy(alpha=0.2f)))

                Text(
                    "MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )

                // --- 2. Students (Expandable) ---
                ExpandableNavigationDrawerItem(
                    parentItem = TopLevelNavItem.Students,
                    subItems = studentSubItems,
                    currentSelectedRoute = selectedRoute,
                    onItemSelected = { route -> selectedRoute = route }
                )

                // --- 3. Activities (Expandable) ---
                ExpandableNavigationDrawerItem(
                    parentItem = TopLevelNavItem.Activities,
                    subItems = activitySubItems,
                    currentSelectedRoute = selectedRoute,
                    onItemSelected = { route -> selectedRoute = route }
                )

                // --- PUSH SETTINGS TO BOTTOM ---
                Spacer(Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp).background(Color.Gray.copy(alpha=0.2f)))

                Text(
                    "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )

                // --- 4. Settings (Contains Logout) ---
                ExpandableNavigationDrawerItem(
                    parentItem = TopLevelNavItem.Settings,
                    subItems = settingsSubItems,
                    currentSelectedRoute = selectedRoute,
                    onItemSelected = { route ->
                        // INTERCEPT LOGOUT
                        if (route == SubNavItem.Logout.route) {
                            onLogout()
                        } else {
                            selectedRoute = route
                        }
                    }
                )

                // Add a little padding at the very bottom
                Spacer(Modifier.height(16.dp))
            }
        },
        content = {
            Scaffold(
                containerColor = Color.White,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                currentTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color.Black
                        )
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, BrandLightPink)
                            )
                        )
                ) {
                    GetScreenForRoute(
                        route = selectedRoute,
                        studentViewModel = studentViewModel,
                        activityViewModel = activityViewModel,
                        questionsViewModel = questionsViewModel,
                        serverIp = serverIp,     // <--- Passed Here
                        serverPort = serverPort  // <--- Passed Here
                    )
                }
            }
        }
    )
}

// =========================================================================
// 4. Content Logic
// =========================================================================

@Composable
fun GetScreenForRoute(
    route: String,
    studentViewModel: StudentViewModel,
    activityViewModel: ActivityViewModel,
    questionsViewModel: QuestionsViewModel,
    serverIp: String,   // <--- NEW PARAM
    serverPort: Int     // <--- NEW PARAM
) {
    when (route) {
        // Pass IP/Port to Dashboard
        TopLevelNavItem.Dashboard.route -> DashboardScreen(
            studentViewModel = studentViewModel,
            activityViewModel = activityViewModel,
            serverIp = serverIp,
            serverPort = serverPort
        )

        // Student Routes
        SubNavItem.StudentList.route -> ManageStudentsListScreen(viewModel = studentViewModel)
        SubNavItem.StudentCreate.route -> ManageStudentCreateScreen(viewModel = studentViewModel)
        SubNavItem.StudentUpdate.route -> ManageStudentUpdateScreen(viewModel = studentViewModel)
        SubNavItem.StudentDelete.route -> ManageStudentDeleteScreen(viewModel = studentViewModel)

        // Activity Routes
        SubNavItem.ActivityList.route -> ManageActivitiesListScreen(
            activityViewModel = activityViewModel,
            questionsViewModel = questionsViewModel
        )
        SubNavItem.ActivityCreate.route -> ManageActivityCreateScreen(
            activityViewModel = activityViewModel,
            questionsViewModel = questionsViewModel,
            studentViewModel = studentViewModel
        )
        SubNavItem.ActivityUpdate.route -> ManageActivityUpdateScreen(
            activityViewModel = activityViewModel,
            questionsViewModel = questionsViewModel,
            studentViewModel = studentViewModel
        )

        // Settings / Logout Routes don't need a screen, as Logout is intercepted
        // But if we add a "General Settings" page later, it would go here.
        SubNavItem.Logout.route -> {
            // Placeholder in case of lag
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Destination not found for route: $route")
            }
        }
    }
}