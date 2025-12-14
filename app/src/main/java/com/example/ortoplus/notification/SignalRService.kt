package com.example.ortoplus.notification

import android.util.Log
import com.example.ortoplus.auth.TokenManager
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SignalRService(
    private val baseUrl: String,
    private val tokenManager: TokenManager
) {
    private var hubConnection: HubConnection? = null

    // 1. Mark as suspend to run cleanly in Coroutines
    suspend fun startConnection(onMessageReceived: (String) -> Unit) {
        // 2. Switch to IO context to avoid blocking the Main UI thread
        withContext(Dispatchers.IO) {
            try {
                hubConnection = HubConnectionBuilder.create("$baseUrl/notificationHub")
                    .withAccessTokenProvider(Single.defer {
                        // 3. Use runBlocking to call the suspend getToken() function
                        // This bridges the Async/Await world of Kotlin with the RxJava world of SignalR
                        val token = runBlocking { tokenManager.getToken() } ?: ""
                        Single.just(token)
                    })
                    .build()

                hubConnection?.on("ReceiveNotification", { message: String ->
                    Log.d("SignalR", "Message received: $message")
                    onMessageReceived(message)
                }, String::class.java)

                // 4. This blocking call is now safe because we are wrapped in withContext(Dispatchers.IO)
                hubConnection?.start()?.blockingAwait()
                Log.d("SignalR", "Connection started successfully")

            } catch (e: Exception) {
                Log.e("SignalR", "Error starting connection", e)
            }
        }
    }

    fun stopConnection() {
        hubConnection?.stop()
    }
}