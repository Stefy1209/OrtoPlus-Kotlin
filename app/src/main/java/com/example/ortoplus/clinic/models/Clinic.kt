package com.example.ortoplus.clinic.models

import com.example.ortoplus.address.models.Address
import com.example.ortoplus.review.models.Review
import kotlinx.serialization.Serializable

@Serializable
data class Clinic(
    val clinicId: String,
    val name: String,
    val rating: Float,
    val latitude: Float,
    val longitude: Float,
    val address: Address,
    val reviews: MutableList<Review>
)