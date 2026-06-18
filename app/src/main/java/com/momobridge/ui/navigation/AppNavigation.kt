package com.momobridge.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.navArgument
import com.momobridge.ui.apikeys.ApiKeysScreen
import com.momobridge.ui.dashboard.DashboardScreen
import com.momobridge.ui.help.HelpScreen
import com.momobridge.ui.settings.SenderConfigScreen
import com.momobridge.ui.settings.SettingsScreen
import com.momobridge.ui.setup.SetupScreen
import com.momobridge.ui.splash.SplashScreen
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.transactions.TransactionsScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Setup : Screen("setup")
    data object Main : Screen("main")
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object ApiKeys : Screen("api_keys")
    data object Settings : Screen("settings")
    data object SenderConfig : Screen("sender_config/{senderAddress}/{label}?body={body}")
    data object Help : Screen("help")
}

@Composable
fun AppNavigation(
    retrainSenderAddress: String? = null,
    retrainLabel: String? = null,
    retrainSmsBody: String? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val prefs = remember {
        context.getSharedPreferences("momobridge_prefs", Context.MODE_PRIVATE)
    }

    var permissionsGranted by remember { mutableStateOf(checkPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        permissionsGranted = granted.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            val perms = buildList {
                add(Manifest.permission.RECEIVE_SMS)
                add(Manifest.permission.READ_SMS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    // Handle retrain deep-link from notification
    LaunchedEffect(retrainSenderAddress, retrainLabel, retrainSmsBody) {
        if (retrainSenderAddress != null && retrainLabel != null) {
            // Navigate to sender config after splash/main is loaded
            navController.navigate(
                "sender_config/${retrainSenderAddress}/${retrainLabel}?body=${retrainSmsBody ?: ""}"
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier
            .fillMaxSize()
            .background(MomoColors.GroundDark)
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    val setupComplete = prefs.getBoolean("setup_complete", false)
                    val dest = if (setupComplete) Screen.Main.route else Screen.Setup.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                onComplete = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainTabScreen(
                navController = navController,
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToSenderConfig = { addr, label ->
                    navController.navigate("sender_config/$addr/$label")
                }
            )
        }

        composable(
            Screen.SenderConfig.route,
            arguments = listOf(
                navArgument("senderAddress") { type = NavType.StringType },
                navArgument("label") { type = NavType.StringType },
                navArgument("body") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val senderAddress = backStackEntry.arguments?.getString("senderAddress") ?: ""
            val label = backStackEntry.arguments?.getString("label") ?: senderAddress
            val prefilledBody = backStackEntry.arguments?.getString("body") ?: ""
            SenderConfigScreen(
                senderAddress = senderAddress,
                label = label,
                prefilledSmsBody = prefilledBody,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.Help.route) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainTabScreen(
    navController: androidx.navigation.NavController,
    onNavigateToHelp: () -> Unit,
    onNavigateToSenderConfig: (String, String) -> Unit,
    mainTabViewModel: MainTabViewModel = hiltViewModel()
) {
    val apiKeyCount by mainTabViewModel.apiKeyCount.collectAsStateWithLifecycle()
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onTabSelected = { route ->
                    tabNavController.navigate(route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                apiKeyCount = apiKeyCount
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTransactions = {
                        tabNavController.navigate(Screen.Transactions.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },

                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen()
            }
            composable(Screen.ApiKeys.route) {
                ApiKeysScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToHelp = onNavigateToHelp,
                    onNavigateToSenderConfig = onNavigateToSenderConfig,
                    onReconfigure = {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private fun checkPermissions(context: Context): Boolean {
    val perms = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return perms.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
