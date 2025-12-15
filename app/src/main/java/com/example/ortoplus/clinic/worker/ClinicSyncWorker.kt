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
            val repository = createClinicRepository()
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

    private fun createClinicRepository(): ClinicRepository {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }

        val tokenManager = TokenManager(applicationContext)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ortoplus-db"
        ).build()

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