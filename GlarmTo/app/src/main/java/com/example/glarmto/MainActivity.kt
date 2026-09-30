package com.example.glarmto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import com.example.glarmto.ui.calculator.CalculatorScreen
import com.example.glarmto.ui.dashboard.DashboardScreen
import com.example.glarmto.ui.login.WelcomeScreen
import com.example.glarmto.ui.login.RegisterScreen
import com.example.glarmto.ui.login.LoginScreen
import com.example.glarmto.ui.nutrition.NutritionScreen
import com.example.glarmto.ui.theme.GlarmToTheme
import com.example.glarmto.ui.workout.WorkoutScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding

import android.os.Build

/**
 * The main entry point of the GlarmTo application.
 * 
 * This activity acts as the host for Jetpack Compose UI. It configures the
 * window properties (like edge-to-edge display and 120Hz refresh rate) and
 * determines the initial destination based on user authentication state.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Force highest available refresh rate (e.g., 120Hz or 144Hz) for smooth Compose animations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window?.let { win ->
                val modes = win.windowManager.defaultDisplay.supportedModes
                val maxMode = modes.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val attrs = win.attributes
                    attrs.preferredDisplayModeId = maxMode.modeId
                    win.attributes = attrs
                }
            }
        }

        setContent {
            val application = applicationContext as GlarmToApplication
            val currentTheme by application.themeManager.currentTheme.collectAsState()

            GlarmToTheme(themeName = currentTheme) {
                val startDest = if (application.repository.getCurrentUser() != null) {
                    if (application.repository.isProfileSetup()) {
                        Screen.Dashboard.route
                    } else {
                        "onboarding"
                    }
                } else {
                    "welcome"
                }

                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    if (currentTheme == com.example.glarmto.data.preferences.ThemeManager.THEME_AURA) {
                        com.example.glarmto.ui.theme.AnimatedAuraBackground()
                    }
                    MainScreen(startDest)
                }
            }
        }
    }

    var onUserLeaveHintCallback: (() -> Unit)? = null

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onUserLeaveHintCallback?.invoke()
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Routines : Screen("routines", "Routines", Icons.Filled.ListAlt)
    object Workout : Screen("workout", "Workout", Icons.Filled.FitnessCenter)
    object Recovery : Screen("recovery", "Recovery", Icons.Filled.BatteryChargingFull)
    object Nutrition : Screen("nutrition", "Nutrition", Icons.Filled.Fastfood)
    object Calculator : Screen("calculator", "Profile", Icons.Filled.Person)
}

/**
 * The primary navigation container for the application.
 * 
 * Manages the [NavHost] and Bottom Navigation bar. It dynamically hides the
 * bottom bar on authentication and onboarding screens, providing a seamless
 * user experience.
 * 
 * @param startDestination The initial route string to launch.
 */
@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication

    val items = listOf(
        Screen.Dashboard,
        Screen.Routines,
        Screen.Workout,
        Screen.Nutrition,
        Screen.Calculator
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            // Hide bottom bar on auth and onboarding screens
            if (currentDestination?.route != "welcome" && currentDestination?.route != "register" && currentDestination?.route != "login" && currentDestination?.route != "onboarding") {
                NavigationBar {
                    items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = startDestination, 
            Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        val dest = if (application.repository.isProfileSetup()) Screen.Dashboard.route else "onboarding"
                        navController.navigate(dest) {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") { 
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        val dest = if (application.repository.isProfileSetup()) Screen.Dashboard.route else "onboarding"
                        navController.navigate(dest) {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                ) 
            }
            composable("onboarding") {
                com.example.glarmto.ui.onboarding.OnboardingScreen(onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo("welcome") { inclusive = true }
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
            composable(Screen.Dashboard.route) { 
                DashboardScreen(
                    onLogout = {
                        application.repository.logout()
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true } // Clear the entire backstack completely
                        }
                    },
                    onNavigateToHistory = { isMonthly ->
                        val mode = if (isMonthly) "monthly" else "daily"
                        navController.navigate("history?mode=$mode")
                    }
                ) 
            }
            composable(
                "history?mode={mode}",
                arguments = listOf(androidx.navigation.navArgument("mode") { defaultValue = "daily" })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "daily"
                val isMonthly = mode == "monthly"
                com.example.glarmto.ui.history.HistoryScreen(
                    isMonthly = isMonthly,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Routines.route) { com.example.glarmto.ui.routines.RoutinesScreen() }
            composable(Screen.Workout.route) { WorkoutScreen() }
            composable(Screen.Recovery.route) { com.example.glarmto.ui.workout.RecoveryScreen() }
            composable(Screen.Nutrition.route) { NutritionScreen() }
            composable(Screen.Calculator.route) { CalculatorScreen() }
        }
    }
}