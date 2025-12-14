package com.example.ortoplus.review.models

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val reviewId: String,
    val comment: String,
    val rating: Int,
    val date: String,
    val userAccountId: String
)
