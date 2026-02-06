package de.kaiserdragon.iconrequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.kaiserdragon.iconrequest.data.AppRepository
import de.kaiserdragon.iconrequest.data.IconPackManager
import de.kaiserdragon.iconrequest.data.IconRequestExporter
import de.kaiserdragon.iconrequest.ui.HomeScreen
import de.kaiserdragon.iconrequest.ui.Screen
import de.kaiserdragon.iconrequest.ui.iconcomparison.IconComparisonScreen
import de.kaiserdragon.iconrequest.ui.iconcomparison.IconComparisonViewModel
import de.kaiserdragon.iconrequest.ui.iconcomparison.IconComparisonViewModelFactory
import de.kaiserdragon.iconrequest.ui.iconpackhealth.IconPackHealthScreen
import de.kaiserdragon.iconrequest.ui.iconpackhealth.IconPackHealthViewModel
import de.kaiserdragon.iconrequest.ui.iconpackhealth.IconPackHealthViewModelFactory
import de.kaiserdragon.iconrequest.ui.iconrequest.IconRequestScreen
import de.kaiserdragon.iconrequest.ui.iconrequest.IconRequestViewModel
import de.kaiserdragon.iconrequest.ui.iconrequest.IconRequestViewModelFactory
import de.kaiserdragon.iconrequest.ui.settings.SettingsScreen
import de.kaiserdragon.iconrequest.ui.settings.SettingsViewModel
import de.kaiserdragon.iconrequest.ui.settings.SettingsViewModelFactory
import de.kaiserdragon.iconrequest.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {

            // Initialize SettingsViewModel
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(applicationContext)
            )

            val themeSetting by settingsViewModel.themeSetting.collectAsState()
            val dynamicColors by settingsViewModel.useDynamicColors.collectAsState()

            // 1. Wrap EVERYTHING in your custom theme, passing the darkTheme state
            AppTheme(
                themeSetting = themeSetting,
                dynamicColor = dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val iconPackManager = remember { IconPackManager(applicationContext) }
                    val iconViewModel: IconRequestViewModel = viewModel(
                        factory = IconRequestViewModelFactory(
                            AppRepository(applicationContext),
                            iconPackManager,
                            IconRequestExporter(applicationContext),
                            settingsViewModel
                        )
                    )
                    val healthViewModel: IconPackHealthViewModel = viewModel(
                        factory = IconPackHealthViewModelFactory(iconPackManager)
                    )
                    val apps by iconViewModel.appList.collectAsState()

                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onLaunchRequest = {
                                    iconViewModel.clearSelections()
                                    navController.navigate(Screen.IconRequest.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onNavigateToHealth = { packageName ->
                                    // Use the createRoute helper to pass the argument
                                    navController.navigate(
                                        Screen.HealthCheck.createRoute(
                                            packageName
                                        )
                                    )
                                },
                                onNavigateToComparison = { packA, packB ->
                                    navController.navigate(
                                        Screen.Compare.createRoute(packA, packB)
                                    )
                                },
                                viewModel = iconViewModel
                            )
                        }
                        composable(Screen.IconRequest.route) {
                            IconRequestScreen(
                                viewModel = iconViewModel,
                                settingsViewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel, // Pass the correct VM
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.HealthCheck.route) { backStackEntry ->
                            val packageName =
                                backStackEntry.arguments?.getString("packageName") ?: ""
                            // Trigger the check once when this specific screen is launched
                            LaunchedEffect(packageName) {
                                healthViewModel.runHealthCheck(packageName)
                            }
                            IconPackHealthScreen(
                                viewModel = healthViewModel, // Pass the NEW dedicated ViewModel
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(
                            route = Screen.Compare.route,
                            arguments = listOf(
                                navArgument("packA") { type = NavType.StringType },
                                navArgument("packB") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val packA = backStackEntry.arguments?.getString("packA") ?: ""
                            val packB = backStackEntry.arguments?.getString("packB") ?: ""

                            val compareViewModel: IconComparisonViewModel = viewModel(
                                factory = IconComparisonViewModelFactory(
                                    iconPackManager,
                                    IconRequestExporter(applicationContext)
                                )
                            )

                            // Trigger the heavy lifting
                            LaunchedEffect(packA, packB) {
                                compareViewModel.runComparison(packA, packB, apps)
                            }

                            IconComparisonScreen(
                                viewModel = compareViewModel,
                                settingsViewModel = settingsViewModel,
                                packAPackage = packA,
                                packBPackage = packB,
                                allApps = apps,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}