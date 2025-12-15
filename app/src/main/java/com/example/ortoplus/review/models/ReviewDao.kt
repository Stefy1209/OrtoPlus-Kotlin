package com.example.ortoplus.review.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Query("SELECT * FROM reviews WHERE clinicId = :clinicId")
    fun getReviewsByClinicId(clinicId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE reviewId = :reviewId")
    suspend fun getReviewById(reviewId: String): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Query("DELETE FROM reviews WHERE reviewId = :reviewId")
    suspend fun deleteReview(reviewId: String)

    @Query("DELETE FROM reviews WHERE clinicId = :clinicId")
    suspend fun deleteReviewsByClinicId(clinicId: String)
}