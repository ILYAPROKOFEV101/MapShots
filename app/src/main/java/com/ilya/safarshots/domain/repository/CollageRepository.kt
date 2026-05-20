package com.ilya.safarshots.domain.repository

import android.graphics.Bitmap
import com.ilya.safarshots.domain.model.CollageExport
import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.model.GeoPhoto

interface CollageRepository {
    suspend fun renderPreview(photos: List<GeoPhoto>, options: CollageOptions): Bitmap
    suspend fun exportCollage(photos: List<GeoPhoto>, options: CollageOptions): CollageExport
}
