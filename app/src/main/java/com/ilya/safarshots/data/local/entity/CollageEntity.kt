package com.ilya.safarshots.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collage_exports")
data class CollageEntity(
    @PrimaryKey val id: String,
    val template: String,
    val photoIdsCsv: String,
    val outputUri: String,
    val createdAtMillis: Long,
    val width: Int,
    val height: Int,
)
