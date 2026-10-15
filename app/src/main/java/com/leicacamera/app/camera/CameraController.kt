package com.leicacamera.app.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class HardwareCapabilities(
    val hasFlashUnit: Boolean = false,
    val hasFrontCamera: Boolean = false,
    val hasBackCamera: Boolean = true,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val exposureStep: Float = 0.33f,
    val maxZoomRatio: Float = 4f
)

class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    suspend fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int,
        flashMode: Int,
        highResolutionTier: Boolean,
        onCapabilitiesDetected: (HardwareCapabilities) -> Unit,
        onError: (String) -> Unit
    ) {
        val provider = getCameraProvider()
        cameraProvider = provider

        val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
        val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)

        val actualLensFacing = when {
            lensFacing == CameraSelector.LENS_FACING_BACK && hasBack -> CameraSelector.LENS_FACING_BACK
            lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
            hasBack -> CameraSelector.LENS_FACING_BACK
            hasFront -> CameraSelector.LENS_FACING_FRONT
            else -> {
                onError("Nessun modulo fotocamera rilevato sul dispositivo.")
                return
            }
        }

        val selector = CameraSelector.Builder()
            .requireLensFacing(actualLensFacing)
            .build()

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                if (highResolutionTier) {
                    ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                } else {
                    ResolutionStrategy(
                        Size(2560, 1920),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                }
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setFlashMode(flashMode)
            .build()

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture
            )
            camera = boundCamera

            val info = boundCamera.cameraInfo
            val evState = info.exposureState
            val zoomState = info.zoomState.value

            val capabilities = HardwareCapabilities(
                hasFlashUnit = info.hasFlashUnit(),
                hasFrontCamera = hasFront,
                hasBackCamera = hasBack,
                minExposureIndex = if (evState.isExposureCompensationSupported) evState.exposureCompensationRange.lower else 0,
                maxExposureIndex = if (evState.isExposureCompensationSupported) evState.exposureCompensationRange.upper else 0,
                exposureStep = if (evState.isExposureCompensationSupported) evState.exposureCompensationStep.toFloat() else 0f,
                maxZoomRatio = zoomState?.maxZoomRatio ?: 4f
            )
            onCapabilitiesDetected(capabilities)
        } catch (e: Exception) {
            onError("Errore inizializzazione fotocamera: ${e.localizedMessage ?: "Sconosciuto"}")
        }
    }

    fun updateFlashMode(flashMode: Int) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        if (hasFlash) {
            imageCapture?.flashMode = flashMode
        }
    }

    fun triggerTapToFocus(previewView: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    fun setExposureCompensation(index: Int) {
        val cam = camera ?: return
        val state = cam.cameraInfo.exposureState
        if (state.isExposureCompensationSupported && index in state.exposureCompensationRange) {
            cam.cameraControl.setExposureCompensationIndex(index)
        }
    }

    fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 4f
        val minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
        cam.cameraControl.setZoomRatio(ratio.coerceIn(minZoom, maxZoom))
    }

    fun capturePhoto(
        onCaptured: (ImageProxy) -> Unit,
        onError: (String) -> Unit
    ) {
        val captureUseCase = imageCapture ?: run {
            onError("Fotocamera non pronta.")
            return
        }
        captureUseCase.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    onCaptured(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError("Scatto non riuscito: ${exception.localizedMessage ?: "Errore sensore"}")
                }
            }
        )
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
}