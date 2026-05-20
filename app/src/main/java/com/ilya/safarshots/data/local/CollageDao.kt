package com.ilya.safarshots.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.ilya.safarshots.data.local.entity.CollageEntity

@Dao
interface CollageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(export: CollageEntity)
}
