package com.example.ortoplus.address.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressDao {

    @Query("SELECT * FROM addresses WHERE addressId = :addressId")
    suspend fun getAddressById(addressId: String): AddressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)

    @Query("DELETE FROM addresses WHERE addressId = :addressId")
    suspend fun deleteAddress(addressId: String)
}