package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.BusTrackerTheme
import com.example.myapplication.viewmodel.LocationViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Location : Screen("location")
    object CarGame : Screen("car_game")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LocationViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            BusTrackerTheme(darkTheme = uiState.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiveLocationApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun LiveLocationApp(viewModel: LocationViewModel) {
    val navController = rememberNavController()

    // Location permission state
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.shareMyLocation(true)
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(
            route = Screen.Splash.route,
            exitTransition = {
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
            }
        ) {
            SplashScreen(onSplashComplete = {
                navController.navigate(Screen.CarGame.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(
            route = Screen.CarGame.route,
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) }
        ) {
            CarGameScreen(onBack = { /* main screen – no back */ })
        }

        composable(
            route = Screen.Home.route,
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) }
        ) {
            HomeScreen(
                viewModel = viewModel,
                hasLocationPermission = hasLocationPermission,
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onLocationFound = {
                    navController.navigate(Screen.Location.route)
                }
            )
        }

        composable(
            route = Screen.Location.route,
            enterTransition = {
                androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = androidx.compose.animation.core.tween(350)
                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(350))
            },
            exitTransition = {
                androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = androidx.compose.animation.core.tween(300)
                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            }
        ) {
            LocationScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.clearSearch()
                    navController.popBackStack()
                }
            )
        }
    }
}
