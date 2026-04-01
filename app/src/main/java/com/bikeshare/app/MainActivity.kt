package com.bikeshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bikeshare.app.auth.AuthRepository
import com.bikeshare.app.auth.LoginScreen
import com.bikeshare.app.auth.LoginViewModel
import com.bikeshare.app.auth.NameScreen
import com.bikeshare.app.auth.NameViewModel
import com.bikeshare.app.auth.OtpScreen
import com.bikeshare.app.auth.OtpViewModel
import com.bikeshare.app.core.navigation.Routes
import com.bikeshare.app.core.network.ApiClient
import com.bikeshare.app.core.network.AuthEventBus
import com.bikeshare.app.core.storage.TokenStorage
import com.bikeshare.app.history.HistoryRepository
import com.bikeshare.app.history.HistoryScreen
import com.bikeshare.app.history.HistoryViewModel
import com.bikeshare.app.home.HomeRepository
import com.bikeshare.app.home.HomeScreen
import com.bikeshare.app.home.HomeViewModel
import com.bikeshare.app.ride.RideRepository
import com.bikeshare.app.ride.RideScreen
import com.bikeshare.app.ride.RideViewModel
import com.bikeshare.app.scan.ScanRepository
import com.bikeshare.app.scan.ScanScreen
import com.bikeshare.app.scan.ScanViewModel
import com.bikeshare.app.ui.theme.BikeshareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStorage = TokenStorage(this)
        val startDestination = if (tokenStorage.isLoggedIn()) Routes.HOME else Routes.LOGIN

        setContent {
            BikeshareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(startDestination = startDestination, tokenStorage = tokenStorage)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(startDestination: String, tokenStorage: TokenStorage) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        AuthEventBus.logoutEvent.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val apiService = ApiClient.create(tokenStorage)
    val authRepository = AuthRepository(apiService, tokenStorage)
    val homeRepository = HomeRepository(apiService)
    val scanRepository = ScanRepository(apiService)
    val rideRepository = RideRepository(apiService)

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val viewModel = remember { LoginViewModel(authRepository) }
            LoginScreen(
                viewModel = viewModel,
                onOtpSent = { phone, debugOtp ->
                    navController.navigate("${Routes.OTP}/$phone?debugOtp=$debugOtp")
                },
            )
        }

        composable("${Routes.OTP}/{phone}?debugOtp={debugOtp}") { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val debugOtp = backStackEntry.arguments?.getString("debugOtp")
            val viewModel = remember { OtpViewModel(authRepository) }
            OtpScreen(
                phone = phone,
                debugOtp = debugOtp,
                viewModel = viewModel,
                onVerified = { nameRequired ->
                    if (nameRequired) {
                        navController.navigate(Routes.NAME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.NAME) {
            val viewModel = remember { NameViewModel(authRepository) }
            NameScreen(
                viewModel = viewModel,
                onNameSet = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.NAME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            val viewModel = remember { HomeViewModel(homeRepository) }
            HomeScreen(
                viewModel = viewModel,
                name = tokenStorage.getName() ?: "",
                onScanToUnlock = { navController.navigate(Routes.SCAN) },
                onActiveRide = {
                    navController.navigate(Routes.RIDE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onLogout = {
                    tokenStorage.clearTokens()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onHistory = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.SCAN) {
            val viewModel = remember { ScanViewModel(scanRepository) }
            ScanScreen(
                viewModel = viewModel,
                onRideStarted = {
                    navController.navigate(Routes.RIDE) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.RIDE) {
            val viewModel = remember { RideViewModel(rideRepository) }
            RideScreen(
                viewModel = viewModel,
                onRideEnded = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.RIDE) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HISTORY) {
            val historyRepository = HistoryRepository(apiService)
            val viewModel = remember { HistoryViewModel(historyRepository) }
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
