package com.example.ortoplus.clinic.worker

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun NetworkStatusHandler(
    networkObserver: NetworkConnectivityObserver,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val networkStatus by networkObserver.networkStatus.collectAsStateWithLifecycle()

    LaunchedEffect(networkStatus) {
        when (networkStatus) {
            is NetworkStatus.Available -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Connected to internet",
                        duration = SnackbarDuration.Short
                    )
                }
            }
            is NetworkStatus.Lost -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "No internet connection",
                        duration = SnackbarDuration.Indefinite,
                        actionLabel = "Dismiss"
                    )
                }
            }
            is NetworkStatus.Idle -> {
                // Do nothing on initial state
            }
        }
    }
}