package com.example.ortoplus.clinic.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.ortoplus.address.models.AddressEntity
import com.example.ortoplus.review.models.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicDao {

    @Transaction
    @Query("SELECT * FROM clinics")
    fun getAllClinics(): Flow<List<ClinicWithDetails>>

    @Transaction
    @Query("SELECT * FROM clinics WHERE clinicId = :clinicId")
    suspend fun getClinicById(clinicId: String): ClinicWithDetails?

    @Transaction
    @Query("SELECT * FROM clinics WHERE clinicId = :clinicId")
    fun getClinicByIdFlow(clinicId: String): Flow<ClinicWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinic(clinic: ClinicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinics(clinics: List<ClinicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Transaction
    suspend fun insertClinicWithDetails(
        clinic: ClinicEntity,
        address: AddressEntity,
        reviews: List<ReviewEntity>
    ) {
        insertAddress(address)
        insertClinic(clinic)
        insertReviews(reviews)
    }

    @Transaction
    suspend fun insertClinicsWithDetails(clinicsWithDetails: List<ClinicWithDetails>) {
        val addresses = clinicsWithDetails.map { it.address }
        val clinics = clinicsWithDetails.map { it.clinic }
        val allReviews = clinicsWithDetails.flatMap { it.reviews }

        insertAddresses(addresses)
        insertClinics(clinics)
        insertReviews(allReviews)
    }

    @Query("DELETE FROM clinics")
    suspend fun deleteAllClinics()

    @Query("DELETE FROM clinics WHERE clinicId = :clinicId")
    suspend fun deleteClinic(clinicId: String)
}