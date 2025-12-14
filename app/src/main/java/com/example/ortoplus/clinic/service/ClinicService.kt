package com.example.ortoplus.clinic.service

import android.util.Log
import com.example.ortoplus.auth.TokenManager
import com.example.ortoplus.clinic.models.Clinic
import com.example.ortoplus.review.models.Review
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.time.Instant

class ClinicService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    private val baseUrl = "http://10.0.2.2:5189/clinics"

    suspend fun getAllClinics(): Result<List<Clinic>> {
        return try {
            val token =
                tokenManager.getToken() ?: return Result.failure(Exception("Not authenticated"))

            Log.d("ClinicService", "Fetching clinics from: $baseUrl")

            val response = client.get(baseUrl) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            Log.d("ClinicService", "Response status: ${response.status}")

            if (response.status.isSuccess()) {
                val clinics = response.body<List<Clinic>>()
                Log.d("ClinicService", "Fetched ${clinics.size} clinics")
                Result.success(clinics)
            } else {
                Result.failure(Exception("Failed to fetch clinics: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("ClinicService", "Error fetching clinics", e)
            Result.failure(e)
        }
    }

    suspend fun getClinicById(id: String): Result<Clinic> {
        return try {
            val token =
                tokenManager.getToken() ?: return Result.failure(Exception("Not authenticated"))

            val response: Clinic = client.get("$baseUrl/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReview(clinicId: String, comment: String, rating: Int): Result<Review> {
        return try {
            val token = tokenManager.getToken() ?: return Result.failure(Exception("Not authenticated"))

            Log.d("ClinicService", "Adding review to clinic: $clinicId")

            val response = client.post("$baseUrl/$clinicId/reviews") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body = Review(
                    reviewId = "00000000-0000-0000-0000-000000000000",
                    comment = comment,
                    rating = rating,
                    date = Instant.now().toString(),
                    userAccountId = "00000000-0000-0000-0000-000000000000"
                ))
            }

            if (response.status.isSuccess()) {
                val createdReview = response.body<Review>()
                Result.success(createdReview)
            } else {
                Result.failure(Exception("Failed to add review: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("ClinicService", "Error adding review", e)
            Result.failure(e)
        }
    }
}