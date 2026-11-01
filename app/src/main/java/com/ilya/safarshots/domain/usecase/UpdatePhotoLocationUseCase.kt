package com.ilya.safarshots.domain.usecase

import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import javax.inject.Inject

class UpdatePhotoLocationUseCase @Inject constructor(
    private val repository: GeoPhotoRepository,
) {
    suspend operator fun invoke(photoId: String, location: PhotoLocation?) =
        repository.updatePhotoLocation(photoId, location)
}
