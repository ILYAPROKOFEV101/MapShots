package com.ilya.safarshots.domain.usecase

import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import javax.inject.Inject

class GetGeoPhotosUseCase @Inject constructor(
    private val repository: GeoPhotoRepository,
) {
    operator fun invoke() = repository.observePhotos()
}
