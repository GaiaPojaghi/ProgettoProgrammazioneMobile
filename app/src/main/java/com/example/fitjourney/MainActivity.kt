package com.example.fitjourney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjourney.ui.screens.StudyHomeScreen
import androidx.navigation.compose.*
import com.example.fitjourney.ui.viewModel.AuthViewModel
import com.example.fitjourney.ui.viewModel.WeeklyDataViewModel
import com.example.fitjourney.ui.screens.*
import com.example.fitjourney.ui.theme.FitJourneyTheme
import com.example.fitjourney.ui.viewModel.StudyViewModel
import com.example.fitjourney.utils.GoogleAuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignInClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitJourneyTheme {
                val authViewModel = remember { AuthViewModel() }
                val studyViewModel = remember { StudyViewModel() }
                val weeklyDataViewModel = remember { WeeklyDataViewModel() }
                val googleSignInClient = GoogleAuthHelper.getGoogleSignInClient(this)

                FitJourneyApp(
                    authViewModel = authViewModel,
                    studyViewModel = studyViewModel,
                    weeklyDataViewModel = weeklyDataViewModel,
                    googleSignInClient = googleSignInClient
                )
            }
        }
    }
}

@Composable
fun FitJourneyApp(
    authViewModel: AuthViewModel,
    studyViewModel: StudyViewModel,
    weeklyDataViewModel: WeeklyDataViewModel,
    googleSignInClient: GoogleSignInClient
) {
    val navController = rememberNavController()
    val studyData by studyViewModel.studyData

    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, "home"),
        BottomNavItem("Settimana", Icons.Filled.BarChart, "weekly"),
        BottomNavItem("Medaglie", Icons.Filled.EmojiEvents, "rewards"),
        BottomNavItem("Profilo", Icons.Filled.Person, "profile")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Box {
                                Icon(item.icon, contentDescription = item.label)
                                if (item.route == "rewards" && studyData.newMedalUnlocked) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                StudyHomeScreen(
                    navController = navController,
                    viewModel = studyViewModel
                )
            }
            composable("studySession") {
                StudySessionScreen(
                    navController = navController,
                    viewModel = studyViewModel
                )
            }
            composable("weekly") {
                WeeklyDataScreen(
                    studyViewModel = studyViewModel,
                    weeklyDataViewModel = weeklyDataViewModel
                )
            }
            composable("rewards") {
                RewardsScreen(
                    viewModel = studyViewModel
                )
            }
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    viewModel = authViewModel
                )
            }
            composable("login") {
                LoginScreen(
                    navController = navController,
                    viewModel = authViewModel,
                    googleSignInClient = googleSignInClient
                )
            }
            composable("register") {
                RegisterForm(
                    navController = navController,
                    viewModel = authViewModel,
                    googleSignInClient = googleSignInClient
                )
            }
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)