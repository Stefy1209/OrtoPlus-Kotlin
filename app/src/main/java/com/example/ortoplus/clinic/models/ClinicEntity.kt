package com.example.ortoplus.clinic.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.ortoplus.address.models.AddressEntity
import com.example.ortoplus.review.models.ReviewEntity

@Entity(tableName = "clinics")
data class ClinicEntity(
    @PrimaryKey
    val clinicId: String,
    val name: String,
    val rating: Float,
    val latitude: Float,
    val longitude: Float,
    val addressId: String,
    val synced: Boolean
)

data class ClinicWithDetails(
    @Embedded val clinic: ClinicEntity,
    @Relation(
        parentColumn = "addressId",
        entityColumn = "addressId"
    )
    val address: AddressEntity,
    @Relation(
        parentColumn = "clinicId",
        entityColumn = "clinicId"
    )
    val reviews: List<ReviewEntity>
)