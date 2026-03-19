package com.bikeshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bikeshare.app.auth.AuthRepository
import com.bikeshare.app.home.HomeRepository
import com.bikeshare.app.home.HomeScreen
import com.bikeshare.app.home.HomeViewModel
import com.bikeshare.app.auth.LoginScreen
import com.bikeshare.app.auth.LoginViewModel
import com.bikeshare.app.auth.OtpScreen
import com.bikeshare.app.auth.OtpViewModel
import com.bikeshare.app.core.navigation.Routes
import com.bikeshare.app.core.network.ApiClient
import com.bikeshare.app.core.storage.TokenStorage
import com.bikeshare.app.ui.theme.BikeshareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStorage = TokenStorage(this)

        // If the user already has a saved token, go straight to home.
        // Otherwise start at the login screen.
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

    // Set up shared dependencies used across screens
    val apiService = ApiClient.create(tokenStorage)
    val authRepository = AuthRepository(apiService, tokenStorage)
    val homeRepository = HomeRepository(apiService)

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
                onVerified = {
                    navController.navigate(Routes.HOME) {
                        // Clear Login + OTP from the back stack so Back doesn't return there
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            val viewModel = remember { HomeViewModel(homeRepository) }
            HomeScreen(
                viewModel = viewModel,
                onLogout = {
                    tokenStorage.clearTokens()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.RIDE) {
            // TODO: replace with real RideScreen
        }
    }
}

