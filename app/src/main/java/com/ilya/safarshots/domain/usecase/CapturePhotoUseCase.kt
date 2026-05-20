package com.ilya.safarshots.domain.usecase

import com.ilya.safarshots.domain.model.PhotoLocation
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import java.io.File
import javax.inject.Inject

class CapturePhotoUseCase @Inject constructor(
    private val repository: GeoPhotoRepository,
) {
    suspend operator fun invoke(
        originalFile: File,
        location: PhotoLocation?,
        cameraFacing: String?,
    ) = repository.saveCapturedPhoto(originalFile, location, cameraFacing)
}
