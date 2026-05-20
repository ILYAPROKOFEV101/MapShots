package com.ilya.safarshots.domain.usecase

import com.ilya.safarshots.domain.model.CollageOptions
import com.ilya.safarshots.domain.repository.CollageRepository
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import javax.inject.Inject

class CreateCollageUseCase @Inject constructor(
    private val photoRepository: GeoPhotoRepository,
    private val collageRepository: CollageRepository,
) {
    suspend operator fun invoke(photoIds: List<String>, options: CollageOptions) =
        collageRepository.renderPreview(photoRepository.getPhotos(photoIds), options)
}
