package com.example.ortoplus.review.models

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReviewDao {

    @Query("SELECT * FROM reviews WHERE clinicId = :clinicId")
    suspend fun getReviewsByClinicId(clinicId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE synced = 0")
    suspend fun getUnsyncedReviews(): List<ReviewEntity>

    @Query("UPDATE reviews SET synced = 1 WHERE reviewId = :reviewId")
    suspend fun markSynced(reviewId: String)
}