package com.example.ortoplus.clinic.service

import com.example.ortoplus.clinic.models.Clinic
import com.example.ortoplus.clinic.models.ClinicDao
import com.example.ortoplus.clinic.models.ClinicWithDetails
import com.example.ortoplus.review.models.Review
import com.example.ortoplus.review.models.ReviewEntity
import com.example.ortoplus.utils.toAddressEntity
import com.example.ortoplus.utils.toClinic
import com.example.ortoplus.utils.toClinicEntity
import com.example.ortoplus.utils.toReview
import com.example.ortoplus.utils.toReviewEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClinicLocalService(
    private val clinicDao: ClinicDao
) {
    fun getAllClinics(): Flow<List<Clinic>> {
        return clinicDao.getAllClinics().map { clinicsWithDetails ->
            clinicsWithDetails.map { it.toClinic() }
        }
    }

    suspend fun getClinicById(id: String): Result<Clinic> {
        return try {
            val clinicWithDetails = clinicDao.getClinicById(id)
            if (clinicWithDetails != null) {
                Result.success(clinicWithDetails.toClinic())
            } else {
                Result.failure(Exception("Clinic not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReview(clinicId: String, comment: String, rating: Int, reviewId: String, userAccountId: String, date: String, synced: Boolean): Result<Review> {
        return try {
            val review = ReviewEntity(
                reviewId = reviewId,
                comment = comment,
                rating = rating,
                date = date,
                userAccountId = userAccountId,
                clinicId = clinicId,
                synced = synced
            )
            clinicDao.insertReview(review)
            Result.success(review.toReview())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveClinic(clinic: Clinic) {
        clinicDao.insertClinicWithDetails(
            clinic = clinic.toClinicEntity(),
            address = clinic.address.toAddressEntity(),
            reviews = clinic.reviews.map { it.toReviewEntity(clinic.clinicId) }
        )
    }

    suspend fun saveClinics(clinics: List<Clinic>) {
        val clinicsWithDetails = clinics.map { clinic ->
            ClinicWithDetails(
                clinic = clinic.toClinicEntity(),
                address = clinic.address.toAddressEntity(),
                reviews = clinic.reviews.map { it.toReviewEntity(clinic.clinicId) }
            )
        }
        clinicDao.insertClinicsWithDetails(clinicsWithDetails)
    }

    suspend fun clearAllClinics() {
        clinicDao.deleteAllClinics()
    }
}