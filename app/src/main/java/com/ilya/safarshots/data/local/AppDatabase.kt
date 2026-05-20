package com.ilya.safarshots.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ilya.safarshots.data.local.entity.CollageEntity
import com.ilya.safarshots.data.local.entity.GeoPhotoEntity

@Database(
    entities = [GeoPhotoEntity::class, CollageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun geoPhotoDao(): GeoPhotoDao
    abstract fun collageDao(): CollageDao
}
