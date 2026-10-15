package com.leicacamera.app.ui

import android.app.Application
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leicacamera.app.camera.HardwareCapabilities
import com.leicacamera.app.data.MediaStoreRepository
import com.leicacamera.app.data.UserPreferencesRepository
import com.leicacamera.app.filter.AspectRatioMode
import com.leicacamera.app.filter.PhotoFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraUiState(
    val selectedFilter: PhotoFilter = PhotoFilter.LEICA_VIVID,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_4_3,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val zoomRatio: Float = 1.0f,
    val exposureIndex: Int = 0,
    val capabilities: HardwareCapabilities = HardwareCapabilities(),
    val latestPhotoUri: Uri? = null,
    val isShutterFlashing: Boolean = false,
    val isSavingPhoto: Boolean = false,
    val showFilterStrip: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showFullScreenViewer: Boolean = false,
    val saveOriginalCopy: Boolean = false,
    val showFramingGrid: Boolean = true,
    val highResolutionTier: Boolean = true,
    val errorMessage: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application)
    private val mediaRepo = MediaStoreRepository(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        selectedFilter = prefs.selectedFilter,
                        aspectRatio = prefs.aspectRatio,
                        flashMode = prefs.flashMode,
                        saveOriginalCopy = prefs.saveOriginalCopy,
                        showFramingGrid = prefs.showFramingGrid,
                        highResolutionTier = prefs.highResolutionTier
                    )
                }
            }
        }
        viewModelScope.launch {
            val lastUri = mediaRepo.getLatestCapturedPhotoUri()
            _uiState.update { it.copy(latestPhotoUri = lastUri) }
        }
    }

    fun onCapabilitiesDetected(capabilities: HardwareCapabilities) {
        _uiState.update { state ->
            val validFlash = if (capabilities.hasFlashUnit) state.flashMode else ImageCapture.FLASH_MODE_OFF
            state.copy(
                capabilities = capabilities,
                flashMode = validFlash,
                exposureIndex = 0
            )
        }
    }

    fun selectFilter(filter: PhotoFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        viewModelScope.launch { prefsRepo.setFilter(filter) }
    }

    fun toggleFilterStrip() {
        _uiState.update { it.copy(showFilterStrip = !it.showFilterStrip) }
    }

    fun cycleAspectRatio() {
        val nextRatio = _uiState.value.aspectRatio.next()
        _uiState.update { it.copy(aspectRatio = nextRatio) }
        viewModelScope.launch { prefsRepo.setAspectRatio(nextRatio) }
    }

    fun cycleFlashMode(onFlashUpdated: (Int) -> Unit) {
        if (!_uiState.value.capabilities.hasFlashUnit) return
        val nextFlash = when (_uiState.value.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
        _uiState.update { it.copy(flashMode = nextFlash) }
        onFlashUpdated(nextFlash)
        viewModelScope.launch { prefsRepo.setFlashMode(nextFlash) }
    }

    fun toggleLensFacing() {
        val caps = _uiState.value.capabilities
        if (!caps.hasFrontCamera || !caps.hasBackCamera) return
        val nextLens = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.update {
            it.copy(
                lensFacing = nextLens,
                zoomRatio = 1.0f,
                exposureIndex = 0
            )
        }
    }

    fun updateZoomRatio(newRatio: Float) {
        val maxZoom = _uiState.value.capabilities.maxZoomRatio
        val clamped = newRatio.coerceIn(1.0f, maxZoom)
        _uiState.update { it.copy(zoomRatio = clamped) }
    }

    fun updateExposureIndex(newIndex: Int) {
        val caps = _uiState.value.capabilities
        val clamped = newIndex.coerceIn(caps.minExposureIndex, caps.maxExposureIndex)
        _uiState.update { it.copy(exposureIndex = clamped) }
    }

    fun triggerShutterVisualFeedback() {
        viewModelScope.launch {
            _uiState.update { it.copy(isShutterFlashing = true) }
            delay(85)
            _uiState.update { it.copy(isShutterFlashing = false) }
        }
    }

    fun onPhotoCaptured(imageProxy: ImageProxy) {
        val currentState = _uiState.value
        _uiState.update { it.copy(isSavingPhoto = true) }

        viewModelScope.launch {
            val result = mediaRepo.processAndSavePhoto(
                imageProxy = imageProxy,
                filter = currentState.selectedFilter,
                aspectRatio = currentState.aspectRatio,
                isFrontCamera = currentState.lensFacing == CameraSelector.LENS_FACING_FRONT,
                saveOriginalCopy = currentState.saveOriginalCopy
            )
            imageProxy.close()

            result.fold(
                onSuccess = { savedUri ->
                    _uiState.update {
                        it.copy(
                            latestPhotoUri = savedUri,
                            isSavingPhoto = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingPhoto = false,
                            errorMessage = error.localizedMessage ?: "Errore salvataggio immagine"
                        )
                    }
                }
            )
        }
    }

    fun setSaveOriginalCopy(enabled: Boolean) {
        _uiState.update { it.copy(saveOriginalCopy = enabled) }
        viewModelScope.launch { prefsRepo.setSaveOriginalCopy(enabled) }
    }

    fun setShowFramingGrid(enabled: Boolean) {
        _uiState.update { it.copy(showFramingGrid = enabled) }
        viewModelScope.launch { prefsRepo.setShowFramingGrid(enabled) }
    }

    fun setHighResolutionTier(enabled: Boolean) {
        _uiState.update { it.copy(highResolutionTier = enabled) }
        viewModelScope.launch { prefsRepo.setHighResolutionTier(enabled) }
    }

    fun setShowSettingsSheet(visible: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = visible) }
    }

    fun setShowFullScreenViewer(visible: Boolean) {
        _uiState.update { it.copy(showFullScreenViewer = visible) }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}