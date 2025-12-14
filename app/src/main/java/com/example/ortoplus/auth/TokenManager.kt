package com.example.ortoplus.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter

private val Context.dataStore by preferencesDataStore("auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val KEY_JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_EXPIRATION = stringPreferencesKey("expiration_date")
    }

    suspend fun saveToken(token: String, expiresAt: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_JWT_TOKEN] = token
            preferences[KEY_EXPIRATION] = expiresAt
        }
    }

    suspend fun getToken(): String? {
        val preferences = context.dataStore.data.first()
        val token = preferences[KEY_JWT_TOKEN]
        val expiresAt = preferences[KEY_EXPIRATION]

        return if (token != null && isTokenValid(expiresAt)) {
            token
        } else {
            if(token != null) clearToken()
            null
        }
    }

     suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_JWT_TOKEN)
            preferences.remove(KEY_EXPIRATION)
        }
    }

    suspend fun hasToken(): Boolean {
        return getToken() != null
    }

    private fun isTokenValid(expiresAt: String?): Boolean {
        expiresAt ?: return true // If no expiration stored, assume valid (or change to false depending on logic)

        return try {
            val expirationDate = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(expiresAt))
            Instant.now().isBefore(expirationDate)
        } catch (e: Exception) {
            println("An error has spawned: $e")
            false // If parsing fails, treat as invalid
        }
    }
}