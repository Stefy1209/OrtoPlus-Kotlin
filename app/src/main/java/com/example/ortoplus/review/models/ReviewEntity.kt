package com.example.ortoplus.review.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ortoplus.clinic.models.ClinicEntity

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = ClinicEntity::class,
            parentColumns = ["clinicId"],
            childColumns = ["clinicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clinicId"])]
)
data class ReviewEntity(
    @PrimaryKey
    val reviewId: String,
    val comment: String,
    val rating: Int,
    val date: String,
    val userAccountId: String,
    val clinicId: String,
    val synced: Boolean = false
)