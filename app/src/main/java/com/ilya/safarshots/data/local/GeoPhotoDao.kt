package com.ilya.safarshots.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilya.safarshots.data.local.entity.GeoPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoPhotoDao {
    @Query("SELECT * FROM geo_photos ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<GeoPhotoEntity>>

    @Query("SELECT * FROM geo_photos WHERE id = :id")
    fun observeById(id: String): Flow<GeoPhotoEntity?>

    @Query("SELECT * FROM geo_photos WHERE id = :id")
    suspend fun getById(id: String): GeoPhotoEntity?

    @Query("SELECT * FROM geo_photos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<GeoPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: GeoPhotoEntity)

    @Query("UPDATE geo_photos SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE geo_photos SET note = :note WHERE id = :id")
    suspend fun updateNote(id: String, note: String?)

    @Query("UPDATE geo_photos SET addressText = :addressText WHERE id = :id")
    suspend fun updateAddressText(id: String, addressText: String?)

    @Query("UPDATE geo_photos SET mapSnapshotUri = :uri WHERE id = :id")
    suspend fun updateMapSnapshotUri(id: String, uri: String?)

    @Query("UPDATE geo_photos SET thumbnailUri = :uri WHERE id = :id")
    suspend fun updateThumbnailUri(id: String, uri: String?)

    @Query("UPDATE geo_photos SET colorSeed = :colorSeed WHERE id = :id")
    suspend fun updateColorSeed(id: String, colorSeed: Int?)

    @Query("UPDATE geo_photos SET addedToCollageCount = addedToCollageCount + 1 WHERE id IN (:ids)")
    suspend fun incrementCollageCount(ids: List<String>)

    @Query("DELETE FROM geo_photos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM geo_photos")
    suspend fun deleteAll()
}
