package com.example.ortoplus.notification

import android.util.Log
import com.example.ortoplus.auth.TokenManager
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import io.reactivex.rxjava3.core.Single

class SignalRService(
    private val baseUrl: String,
    private val tokenManager: TokenManager
) {
    private var hubConnection: HubConnection? = null

    fun startConnection(onMessageReceived: (String) -> Unit) {
        // 1. Build the connection
        hubConnection = HubConnectionBuilder.create("$baseUrl/notificationHub")
            .withAccessTokenProvider(Single.defer {
                val token = tokenManager.getToken() ?: ""
                Single.just(token)
            })
            .build()

        // 2. Listen for the method defined in your INotificationClient interface
        hubConnection?.on("ReceiveNotification", { message: String ->
            Log.d("SignalR", "Message received: $message")
            onMessageReceived(message)
        }, String::class.java)

        // 3. Start the connection
        try {
            hubConnection?.start()?.blockingAwait()
            Log.d("SignalR", "Connection started successfully")
        } catch (e: Exception) {
            Log.e("SignalR", "Error starting connection", e)
        }
    }

    fun stopConnection() {
        hubConnection?.stop()
    }
}