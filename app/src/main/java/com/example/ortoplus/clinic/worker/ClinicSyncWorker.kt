package com.example.ortoplus.clinic.worker

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ortoplus.auth.TokenManager
import com.example.ortoplus.clinic.service.ClinicLocalService
import com.example.ortoplus.clinic.service.ClinicRepository
import com.example.ortoplus.clinic.service.ClinicService
import com.example.ortoplus.localstorage.AppDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ClinicSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting clinic sync work")

        try {
            val db = createDatabase()
            val repository = createClinicRepository(db)
            val clinicService = createClinicService()

            // Step 1: Sync unsynced reviews to backend
            val reviewSyncResult = syncUnsyncedReviews(db, clinicService)
            if (!reviewSyncResult) {
                Log.w(TAG, "Review sync had issues, but continuing with clinic sync")
            }

            // Step 2: Refresh clinics from backend
            val result = repository.refreshClinics()

            return if (result.isSuccess) {
                val count = result.getOrNull()?.size ?: 0
                Log.d(TAG, "Clinic sync completed successfully - synced $count clinics")
                Result.success()
            } else {
                Log.e(TAG, "Clinic sync failed: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Clinic sync error", e)
            return Result.retry()
        }
    }

    private suspend fun syncUnsyncedReviews(
        db: AppDatabase,
        clinicService: ClinicService
    ): Boolean {
        return try {
            val unsyncedReviews = db.reviewDao().getUnsyncedReviews()
            Log.d(TAG, "Found ${unsyncedReviews.size} unsynced reviews")

            var successCount = 0
            var failureCount = 0

            for (review in unsyncedReviews) {
                val result = clinicService.addReview(
                    clinicId = review.clinicId,
                    comment = review.comment,
                    rating = review.rating
                )

                if (result.isSuccess) {
                    // Mark review as synced
                    db.reviewDao().markSynced(review.reviewId)
                    successCount++
                    Log.d(TAG, "Successfully synced review: ${review.reviewId}")
                } else {
                    failureCount++
                    Log.e(TAG, "Failed to sync review ${review.reviewId}: ${result.exceptionOrNull()?.message}")
                }
            }

            Log.d(TAG, "Review sync completed: $successCount succeeded, $failureCount failed")
            failureCount == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing reviews", e)
            false
        }
    }

    private fun createDatabase(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ortoplus-db"
        ).build()
    }

    private fun createClinicService(): ClinicService {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }
        val tokenManager = TokenManager(applicationContext)
        return ClinicService(httpClient, tokenManager)
    }

    private fun createClinicRepository(db: AppDatabase): ClinicRepository {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }

        val tokenManager = TokenManager(applicationContext)
        val clinicDao = db.clinicDao()
        val clinicService = ClinicService(httpClient, tokenManager)
        val clinicLocalService = ClinicLocalService(clinicDao)

        return ClinicRepository(
            context = applicationContext,
            remoteService = clinicService,
            localService = clinicLocalService
        )
    }

    companion object {
        private const val TAG = "ClinicSyncWorker"
        const val WORK_NAME = "clinic_sync_work"

        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<ClinicSyncWorker>()
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
            Log.d(TAG, "Clinic sync work enqueued")
        }
    }
}