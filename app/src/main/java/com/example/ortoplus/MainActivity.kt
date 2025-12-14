package com.example.ortoplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ortoplus.auth.TokenManager
import com.example.ortoplus.clinic.service.ClinicService
import com.example.ortoplus.login.service.AuthorizationService
import com.example.ortoplus.navigation.Screen
import com.example.ortoplus.notification.SignalRService
import com.example.ortoplus.ui.pages.DetailScreen
import com.example.ortoplus.ui.pages.LoginScreen
import com.example.ortoplus.ui.pages.MasterScreen
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private val tokenManager = TokenManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val authService = AuthorizationService(httpClient, tokenManager)
            val clinicService = ClinicService(httpClient, tokenManager)
            val signalRService = SignalRService("http://10.0.2.2:5189", tokenManager)

            NavHost(navController = navController, startDestination = Screen.Login.route) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        authService = authService,
                        onLoginSuccess = {
                            navController.navigate(Screen.Master.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Master.route) {
                    MasterScreen(
                        authService = authService,
                        clinicService = clinicService,
                        navController = navController
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("itemId") ?: return@composable

                    DetailScreen(
                        clinicId = id,
                        clinicService = clinicService,
                        onNavigateBack = { navController.popBackStack() },
                        signalRService = signalRService
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpClient.close()
    }
}