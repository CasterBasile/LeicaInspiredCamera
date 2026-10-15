package com.leicacamera.app.data

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leicacamera.app.filter.AspectRatioMode
import com.leicacamera.app.filter.PhotoFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "leica_camera_prefs")

data class CameraPreferences(
    val selectedFilter: PhotoFilter = PhotoFilter.LEICA_VIVID,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_4_3,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val saveOriginalCopy: Boolean = false,
    val showFramingGrid: Boolean = true,
    val highResolutionTier: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FILTER_ID = stringPreferencesKey("selected_filter_id")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val FLASH_MODE = intPreferencesKey("flash_mode")
        val SAVE_ORIGINAL = booleanPreferencesKey("save_original_copy")
        val SHOW_GRID = booleanPreferencesKey("show_framing_grid")
        val HIGH_RES = booleanPreferencesKey("high_resolution_tier")
    }

    val preferencesFlow: Flow<CameraPreferences> = context.dataStore.data.map { prefs ->
        val filterId = prefs[Keys.FILTER_ID] ?: PhotoFilter.LEICA_VIVID.id
        val ratioName = prefs[Keys.ASPECT_RATIO] ?: AspectRatioMode.RATIO_4_3.name
        val aspectRatio = runCatching { AspectRatioMode.valueOf(ratioName) }
            .getOrDefault(AspectRatioMode.RATIO_4_3)

        CameraPreferences(
            selectedFilter = PhotoFilter.fromId(filterId),
            aspectRatio = aspectRatio,
            flashMode = prefs[Keys.FLASH_MODE] ?: ImageCapture.FLASH_MODE_OFF,
            saveOriginalCopy = prefs[Keys.SAVE_ORIGINAL] ?: false,
            showFramingGrid = prefs[Keys.SHOW_GRID] ?: true,
            highResolutionTier = prefs[Keys.HIGH_RES] ?: true
        )
    }

    suspend fun setFilter(filter: PhotoFilter) {
        context.dataStore.edit { it[Keys.FILTER_ID] = filter.id }
    }

    suspend fun setAspectRatio(ratio: AspectRatioMode) {
        context.dataStore.edit { it[Keys.ASPECT_RATIO] = ratio.name }
    }

    suspend fun setFlashMode(flashMode: Int) {
        context.dataStore.edit { it[Keys.FLASH_MODE] = flashMode }
    }

    suspend fun setSaveOriginalCopy(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_ORIGINAL] = enabled }
    }

    suspend fun setShowFramingGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GRID] = enabled }
    }

    suspend fun setHighResolutionTier(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_RES] = enabled }
    }
}