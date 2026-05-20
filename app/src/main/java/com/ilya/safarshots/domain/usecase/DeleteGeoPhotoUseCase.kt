package com.ilya.safarshots.domain.usecase

import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import javax.inject.Inject

class DeleteGeoPhotoUseCase @Inject constructor(
    private val repository: GeoPhotoRepository,
) {
    suspend operator fun invoke(id: String) = repository.deletePhoto(id)
}
