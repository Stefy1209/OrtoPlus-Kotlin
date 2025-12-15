package com.example.ortoplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.ortoplus.auth.TokenManager
import com.example.ortoplus.clinic.service.ClinicLocalService
import com.example.ortoplus.clinic.service.ClinicRepository
import com.example.ortoplus.clinic.service.ClinicService
import com.example.ortoplus.clinic.worker.NetworkConnectivityObserver
import com.example.ortoplus.clinic.worker.NetworkStatusHandler
import com.example.ortoplus.localstorage.AppDatabase
import com.example.ortoplus.login.service.AuthorizationService
import com.example.ortoplus.navigation.Screen
import com.example.ortoplus.notification.SignalRService
import com.example.ortoplus.ui.pages.DetailScreen
import com.example.ortoplus.ui.pages.LoginScreen
import com.example.ortoplus.ui.pages.MasterScreen
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
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

    private lateinit var tokenManager: TokenManager
    private lateinit var networkObserver: NetworkConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tokenManager = TokenManager(applicationContext)

        networkObserver = NetworkConnectivityObserver(applicationContext)
        networkObserver.startObserving()

        val startDestination = runBlocking {
            if (tokenManager.hasToken()) Screen.Master.route else Screen.Login.route
        }

        setContent {
            val navController = rememberNavController()
            val authService = AuthorizationService(httpClient, tokenManager)
            val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "ortoplus-db").build()
            val clinicDao = db.clinicDao()
            val clinicService = ClinicService(httpClient, tokenManager)
            val clinicLocalService = ClinicLocalService(clinicDao)
            val clinicRepository = ClinicRepository(
                context = applicationContext,
                remoteService = clinicService,
                localService = clinicLocalService
            )
            val signalRService = SignalRService("http://10.0.2.2:5189", tokenManager)

            // Create snackbar state at the top level
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            // Observe network status
            NetworkStatusHandler(
                networkObserver = networkObserver,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(navController = navController, startDestination = startDestination) {
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
                            clinicRepository = clinicRepository,
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
                            clinicRepository = clinicRepository,
                            onNavigateBack = { navController.popBackStack() },
                            signalRService = signalRService
                        )
                    }
                }

                // Snackbar host at the top level, visible across all screens
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkObserver.stopObserving()
        httpClient.close()
    }
}