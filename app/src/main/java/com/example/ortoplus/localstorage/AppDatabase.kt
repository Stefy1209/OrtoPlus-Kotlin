package com.example.ortoplus.localstorage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ortoplus.address.models.AddressEntity
import com.example.ortoplus.clinic.models.ClinicDao
import com.example.ortoplus.clinic.models.ClinicEntity
import com.example.ortoplus.review.models.ReviewEntity

@Database(
    entities = [
        ClinicEntity::class,
        AddressEntity::class,
        ReviewEntity::class
               ],
    version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clinicDao(): ClinicDao
}