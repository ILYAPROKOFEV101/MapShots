package com.ilya.safarshots.domain.usecase

import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import javax.inject.Inject

class ImportPhotoUseCase @Inject constructor(
    private val repository: GeoPhotoRepository,
) {
    suspend operator fun invoke(
        sourceUri: String,
        location: PhotoLocation?,
    ) = repository.saveImportedPhoto(sourceUri, location)
}
