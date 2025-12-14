package com.example.ortoplus.login.service

import android.util.Log
import com.example.ortoplus.auth.TokenManager
import com.example.ortoplus.login.models.LoginRequest
import com.example.ortoplus.login.models.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess


class AuthorizationService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    private val baseUrl = "http://10.0.2.2:5189/auth"

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            Log.d("AuthService", "Attempting login to: $baseUrl/login")
            Log.d("AuthService", "Username: $username")

            val response = client.post("$baseUrl/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }

            Log.d("AuthService", "Response status: ${response.status}")

            if (response.status.isSuccess()) {
                val data = response.body<LoginResponse>()
                tokenManager.saveToken(data.token, data.expiresAt)
                Log.d("AuthService", "Login successful, token saved")
                Result.success(data)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Login failed with status: ${response.status}, body: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("AuthService", "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearToken()
        Log.d("AuthService", "User logged out, token cleared")
    }
}