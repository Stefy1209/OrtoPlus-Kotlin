package com.example.ortoplus.address.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey
    val addressId: String,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
) {
    fun toFullAddress(): String {
        return "$street, $city, $state $zipCode, $country"
    }

    override fun toString(): String {
        return toFullAddress()
    }
}