package com.example.ortoplus.clinic.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.ortoplus.clinic.models.Clinic
import com.example.ortoplus.review.models.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ClinicRepository(
    private val context: Context,
    private val remoteService: ClinicService,
    private val localService: ClinicLocalService
) {
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getAllClinics(): Flow<Result<List<Clinic>>> = flow {
        if (isOnline()) {
            Log.d("ClinicRepository", "Fetching clinics from remote service")
            try {
                val result = remoteService.getAllClinics()
                if (result.isSuccess) {
                    // Cache the data locally
                    result.getOrNull()?.let { clinics ->
                        localService.saveClinics(clinics)
                    }
                }
                emit(result)
            } catch (e: Exception) {
                Log.e("ClinicRepository", "Remote fetch failed, falling back to local", e)
                // If remote fails, fall back to local
                localService.getAllClinics().collect { clinics ->
                    emit(Result.success(clinics))
                }
            }
        } else {
            Log.d("ClinicRepository", "Offline - fetching clinics from local database")
            localService.getAllClinics().collect { clinics ->
                emit(Result.success(clinics))
            }
        }
    }

    suspend fun getClinicById(id: String): Result<Clinic> {
        return if (isOnline()) {
            Log.d("ClinicRepository", "Fetching clinic $id from remote service")
            try {
                val result = remoteService.getClinicById(id)
                if (result.isSuccess) {
                    // Cache the clinic locally
                    result.getOrNull()?.let { clinic ->
                        localService.saveClinic(clinic)
                    }
                }
                result
            } catch (e: Exception) {
                Log.e("ClinicRepository", "Remote fetch failed, falling back to local", e)
                localService.getClinicById(id)
            }
        } else {
            Log.d("ClinicRepository", "Offline - fetching clinic $id from local database")
            localService.getClinicById(id)
        }
    }

    suspend fun addReview(clinicId: String, comment: String, rating: Int): Result<Review> {
        return if (isOnline()) {
            Log.d("ClinicRepository", "Adding review to clinic $clinicId via remote service")
            try {
                val result = remoteService.addReview(clinicId, comment, rating)
                if (result.isSuccess) {
                    // Update local cache
                    result.getOrNull()?.let { review ->
                        localService.addReview(
                            clinicId = clinicId,
                            comment = comment,
                            rating = rating,
                            reviewId = review.reviewId,
                            userAccountId = review.userAccountId,
                            date = review.date
                        )
                    }
                }
                result
            } catch (e: Exception) {
                Log.e("ClinicRepository", "Failed to add review remotely", e)
                Result.failure(Exception("Cannot add review while offline"))
            }
        } else {
            Log.d("ClinicRepository", "Offline - cannot add review")
            Result.failure(Exception("Cannot add review while offline"))
        }
    }

    suspend fun refreshClinics(): Result<List<Clinic>> {
        return if (isOnline()) {
            Log.d("ClinicRepository", "Refreshing clinics from remote service")
            val result = remoteService.getAllClinics()
            if (result.isSuccess) {
                result.getOrNull()?.let { clinics ->
                    localService.clearAllClinics()
                    localService.saveClinics(clinics)
                }
            }
            result
        } else {
            Result.failure(Exception("Cannot refresh while offline"))
        }
    }
}