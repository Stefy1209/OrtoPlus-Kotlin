package com.example.ortoplus.auth

import java.time.Instant
import java.time.format.DateTimeFormatter

class TokenManager {
    private var authToken: String? = null
    private var expirationDate: Instant? = null

    fun saveToken(token: String, expiresAt: String) {
        authToken = token
        expirationDate = try {
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse(expiresAt))
        } catch (e: Exception) {
            null
        }
    }

    fun getToken(): String? {
        return if (isTokenValid()) authToken else null
    }

    fun clearToken() {
        authToken = null
        expirationDate = null
    }

    fun hasToken(): Boolean {
        return isTokenValid()
    }

    private fun isTokenValid(): Boolean {
        authToken ?: return false
        val expiration = expirationDate ?: return true // If no expiration, assume valid
        return Instant.now().isBefore(expiration)
    }
}