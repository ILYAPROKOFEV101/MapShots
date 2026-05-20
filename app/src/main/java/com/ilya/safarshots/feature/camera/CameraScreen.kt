package com.ilya.safarshots.feature.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilya.safarshots.core.permissions.PermissionController
import com.ilya.safarshots.design.PermissionCard

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onPhotoSaved: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var hasCamera by remember { mutableStateOf(PermissionController.hasCamera(context)) }
    var hasLocation by remember { mutableStateOf(PermissionController.hasAnyLocation(context)) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var gridEnabled by remember { mutableStateOf(false) }
    val imageCapture = remember(lensFacing) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCamera = it
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocation = PermissionController.hasAnyLocation(context)
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }
    LaunchedEffect(Unit) {
        if (!hasLocation) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        viewModel.effects.collect { effect ->
            when (effect) {
                is CameraEffect.Message -> snackbarHostState.showSnackbar(effect.text)
                is CameraEffect.PhotoSaved -> onPhotoSaved(effect.photoId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        if (!hasCamera) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                PermissionCard(
                    title = "Нужен доступ к камере",
                    subtitle = "Чтобы делать фото прямо в приложении.",
                    buttonText = "Разрешить камеру",
                    onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                )
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreview(
                imageCapture = imageCapture,
                lensFacing = lensFacing,
                modifier = Modifier.fillMaxSize(),
            )
            if (gridEnabled) {
                GridOverlay(Modifier.fillMaxSize())
            }
            CameraTopBar(
                locationStatus = state.locationStatus,
                flashMode = flashMode,
                onClose = onClose,
                onFlashChange = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CameraControls(
                isSaving = state.isSaving,
                gridEnabled = gridEnabled,
                onCapture = {
                    val file = viewModel.createOutputFile()
                    val facing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) "front" else "back"
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                viewModel.onImageSaved(file, facing)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                file.delete()
                                viewModel.onCaptureError(exception.message ?: "Не удалось сделать фото")
                            }
                        },
                    )
                },
                onFlip = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                onToggleGrid = { gridEnabled = !gridEnabled },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun CameraTopBar(
    locationStatus: LocationStatus,
    flashMode: Int,
    onClose: () -> Unit,
    onFlashChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrim = Color.Black.copy(alpha = 0.35f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScrimIconButton(scrim = scrim, onClick = onClose, contentDescription = "Close") {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
        }
        LocationPill(locationStatus = locationStatus)
        ScrimIconButton(scrim = scrim, onClick = onFlashChange, contentDescription = "Flash") {
            val icon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                else -> Icons.Rounded.FlashOff
            }
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun ScrimIconButton(
    scrim: Color,
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = scrim,
        contentColor = Color.White,
        modifier = Modifier.padding(0.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun LocationPill(locationStatus: LocationStatus) {
    val available = locationStatus.label.lowercase().contains("ok") ||
        locationStatus.label.lowercase().contains("loc")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (available) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff,
                contentDescription = null,
                tint = Color.White,
            )
            Text(
                locationStatus.label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = Color.White.copy(alpha = 0.3f)
        val thirdWidth = size.width / 3f
        val thirdHeight = size.height / 3f
        drawLine(color, Offset(thirdWidth, 0f), Offset(thirdWidth, size.height), strokeWidth = 1.5f)
        drawLine(color, Offset(thirdWidth * 2f, 0f), Offset(thirdWidth * 2f, size.height), strokeWidth = 1.5f)
        drawLine(color, Offset(0f, thirdHeight), Offset(size.width, thirdHeight), strokeWidth = 1.5f)
        drawLine(color, Offset(0f, thirdHeight * 2f), Offset(size.width, thirdHeight * 2f), strokeWidth = 1.5f)
    }
}
