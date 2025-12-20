package com.example.ortoplus.utils

import com.example.ortoplus.address.models.Address
import com.example.ortoplus.address.models.AddressEntity
import com.example.ortoplus.clinic.models.Clinic
import com.example.ortoplus.clinic.models.ClinicEntity
import com.example.ortoplus.clinic.models.ClinicWithDetails
import com.example.ortoplus.review.models.Review
import com.example.ortoplus.review.models.ReviewEntity

// Mappers: Entity -> Model
fun ClinicWithDetails.toClinic(): Clinic {
    return Clinic(
        clinicId = clinic.clinicId,
        name = clinic.name,
        rating = clinic.rating,
        latitude = clinic.latitude,
        longitude = clinic.longitude,
        address = address.toAddress(),
        reviews = reviews.map { it.toReview() }.toMutableList()
    )
}

fun AddressEntity.toAddress(): Address {
    return Address(
        addressId = addressId,
        street = street,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country
    )
}

fun ReviewEntity.toReview(): Review {
    return Review(
        reviewId = reviewId,
        comment = comment,
        rating = rating,
        date = date,
        userAccountId = userAccountId
    )
}

// Mappers: Model -> Entity
fun Clinic.toClinicEntity(): ClinicEntity {
    return ClinicEntity(
        clinicId = clinicId,
        name = name,
        rating = rating,
        latitude = latitude,
        longitude = longitude,
        addressId = address.addressId,
        synced = true
    )
}

fun Address.toAddressEntity(): AddressEntity {
    return AddressEntity(
        addressId = addressId,
        street = street,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country
    )
}

fun Review.toReviewEntity(clinicId: String): ReviewEntity {
    return ReviewEntity(
        reviewId = reviewId,
        comment = comment,
        rating = rating,
        date = date,
        userAccountId = userAccountId,
        clinicId = clinicId
    )
}