package com.example.ortoplus.clinic.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Lost : NetworkStatus()
    object Idle : NetworkStatus()
}

class NetworkConnectivityObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var wasOffline = false

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Idle)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")

            if (wasOffline) {
                Log.d(TAG, "Transitioning from offline to online - triggering sync")
                _networkStatus.value = NetworkStatus.Available
                ClinicSyncWorker.enqueueSync(context)
                wasOffline = false
            }
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
            _networkStatus.value = NetworkStatus.Lost
            wasOffline = true
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            Log.d(TAG, "Network capabilities changed - Internet: $hasInternet, Validated: $isValidated")

            if (hasInternet && isValidated && wasOffline) {
                Log.d(TAG, "Network validated - triggering sync")
                _networkStatus.value = NetworkStatus.Available
                ClinicSyncWorker.enqueueSync(context)
                wasOffline = false
            }
        }
    }

    fun startObserving() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Check initial state
        val activeNetwork = connectivityManager.activeNetwork
        wasOffline = activeNetwork == null

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        Log.d(TAG, "Network observer started - wasOffline: $wasOffline")
    }

    fun stopObserving() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Network observer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping network observer", e)
        }
    }

    companion object {
        private const val TAG = "NetworkConnectivity"
    }
}