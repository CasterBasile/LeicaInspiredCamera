package com.leicacamera.app.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.leicacamera.app.camera.CameraController
import com.leicacamera.app.filter.PhotoFilter
import com.leicacamera.app.ui.theme.LeicaAmber
import com.leicacamera.app.ui.theme.LeicaAnthracite
import com.leicacamera.app.ui.theme.LeicaBlack
import com.leicacamera.app.ui.theme.LeicaDarkGray
import com.leicacamera.app.ui.theme.LeicaRed
import com.leicacamera.app.ui.theme.LeicaSilver
import com.leicacamera.app.ui.theme.LeicaWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember { CameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(state.lensFacing, state.highResolutionTier) {
        cameraController.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = state.lensFacing,
            flashMode = state.flashMode,
            highResolutionTier = state.highResolutionTier,
            onCapabilitiesDetected = { viewModel.onCapabilitiesDetected(it) },
            onError = { viewModel.reportError(it) }
        )
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(2500)
            focusPoint = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LeicaBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (zoomChange != 1f) {
                            val updated = state.zoomRatio * zoomChange
                            viewModel.updateZoomRatio(updated)
                            cameraController.setZoomRatio(updated)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        focusPoint = offset
                        cameraController.triggerTapToFocus(previewView, offset.x, offset.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val composeColorMatrix = remember(state.selectedFilter) {
                ColorMatrix(state.selectedFilter.colorMatrixValues)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(state.aspectRatio.ratioValue)
                    .clip(RoundedCornerShape(2.dp))
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            colorFilter = ColorFilter.colorMatrix(composeColorMatrix)
                        }
                )

                if (state.selectedFilter.vignetteStrength > 0f || state.selectedFilter.tintOverlayArgb != 0) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (state.selectedFilter.tintOverlayArgb != 0) {
                            drawRect(color = Color(state.selectedFilter.tintOverlayArgb))
                        }
                        if (state.selectedFilter.vignetteStrength > 0f) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = state.selectedFilter.vignetteStrength * 0.85f)
                                    ),
                                    radius = size.maxDimension * 0.72f
                                )
                            )
                        }
                    }
                }

                if (state.showFramingGrid) {
                    FramingGridOverlay()
                }

                if (state.isShutterFlashing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LeicaBlack)
                    )
                }
            }

            focusPoint?.let { pt ->
                val density = LocalDensity.current
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pt.x - with(density) { 28.dp.toPx() }).roundToInt(),
                                    (pt.y - with(density) { 28.dp.toPx() }).roundToInt()
                                )
                            }
                            .size(56.dp)
                            .border(1.dp, LeicaAmber, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        TopBarControls(
            state = state,
            onCycleFlash = {
                viewModel.cycleFlashMode { newFlash ->
                    cameraController.updateFlashMode(newFlash)
                }
            },
            onSwitchCamera = { viewModel.toggleLensFacing() },
            onOpenSettings = { viewModel.setShowSettingsSheet(true) },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 128.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .background(LeicaBlack.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .border(0.5.dp, LeicaDarkGray, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.toggleFilterStrip() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(LeicaRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.selectedFilter.displayName.uppercase(Locale.US),
                        color = LeicaWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }

                Text(text = "|", color = LeicaDarkGray, fontSize = 11.sp)

                FocalStepButton("28", 1.0f, state.zoomRatio) {
                    viewModel.updateZoomRatio(1.0f)
                    cameraController.setZoomRatio(1.0f)
                }
                FocalStepButton("35", 1.25f, state.zoomRatio) {
                    viewModel.updateZoomRatio(1.25f)
                    cameraController.setZoomRatio(1.25f)
                }
                FocalStepButton("50", 1.8f, state.zoomRatio) {
                    viewModel.updateZoomRatio(1.8f)
                    cameraController.setZoomRatio(1.8f)
                }
            }

            if (state.capabilities.minExposureIndex < state.capabilities.maxExposureIndex) {
                ExposureControlBar(
                    currentIndex = state.exposureIndex,
                    minIndex = state.capabilities.minExposureIndex,
                    maxIndex = state.capabilities.maxExposureIndex,
                    step = state.capabilities.exposureStep,
                    onExposureChanged = { index ->
                        viewModel.updateExposureIndex(index)
                        cameraController.setExposureCompensation(index)
                    }
                )
            }

            AnimatedVisibility(
                visible = state.showFilterStrip,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilterSelectorStrip(
                    selectedFilter = state.selectedFilter,
                    onSelectFilter = { viewModel.selectFilter(it) }
                )
            }
        }

        BottomCameraDeck(
            state = state,
            onOpenLatestPhoto = {
                if (state.latestPhotoUri != null) {
                    viewModel.setShowFullScreenViewer(true)
                }
            },
            onShutterClick = {
                viewModel.triggerShutterVisualFeedback()
                cameraController.capturePhoto(
                    onCaptured = { proxy -> viewModel.onPhotoCaptured(proxy) },
                    onError = { msg -> viewModel.reportError(msg) }
                )
            },
            onToggleFilters = { viewModel.toggleFilterStrip() },
            onCycleAspectRatio = { viewModel.cycleAspectRatio() },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(LeicaBlack.copy(alpha = 0.88f))
                .padding(horizontal = 24.dp, vertical = 22.dp)
        )

        state.errorMessage?.let { errorMsg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 20.dp, end = 20.dp),
                containerColor = LeicaAnthracite,
                contentColor = LeicaWhite,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = LeicaRed, fontFamily = FontFamily.Monospace)
                    }
                }
            ) {
                Text(errorMsg, fontSize = 12.sp)
            }
        }
    }

    if (state.showSettingsSheet) {
        EssentialSettingsDialog(
            state = state,
            onDismiss = { viewModel.setShowSettingsSheet(false) },
            onToggleSaveOriginal = { viewModel.setSaveOriginalCopy(it) },
            onToggleGrid = { viewModel.setShowFramingGrid(it) },
            onToggleHighRes = { viewModel.setHighResolutionTier(it) }
        )
    }

    if (state.showFullScreenViewer && state.latestPhotoUri != null) {
        FullScreenPhotoDialog(
            photoUri = state.latestPhotoUri!!,
            onDismiss = { viewModel.setShowFullScreenViewer(false) }
        )
    }
}

@Composable
private fun TopBarControls(
    state: CameraUiState,
    onCycleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.capabilities.hasFlashUnit) {
            IconButton(
                onClick = onCycleFlash,
                modifier = Modifier
                    .size(40.dp)
                    .background(LeicaBlack.copy(alpha = 0.55f), CircleShape)
            ) {
                val (icon, tint) = when (state.flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Outlined.FlashOn to LeicaAmber
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Outlined.FlashAuto to LeicaWhite
                    else -> Icons.Outlined.FlashOff to LeicaSilver
                }
                Icon(icon, contentDescription = "Flash", tint = tint, modifier = Modifier.size(20.dp))
            }
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }

        Text(
            text = state.aspectRatio.label,
            color = LeicaSilver,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.capabilities.hasFrontCamera && state.capabilities.hasBackCamera) {
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(40.dp)
                        .background(LeicaBlack.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Cameraswitch,
                        contentDescription = "Cambia fotocamera",
                        tint = LeicaWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .background(LeicaBlack.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Impostazioni essenziali",
                    tint = LeicaWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FocalStepButton(
    mmLabel: String,
    targetZoom: Float,
    currentZoom: Float,
    onClick: () -> Unit
) {
    val isSelected = kotlin.math.abs(currentZoom - targetZoom) < 0.15f
    Text(
        text = "${mmLabel}mm",
        color = if (isSelected) LeicaAmber else LeicaSilver,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}

@Composable
private fun ExposureControlBar(
    currentIndex: Int,
    minIndex: Int,
    maxIndex: Int,
    step: Float,
    onExposureChanged: (Int) -> Unit
) {
    val evValue = currentIndex * step
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(220.dp)
            .padding(top = 6.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%+.1f EV", evValue),
            color = if (currentIndex != 0) LeicaAmber else LeicaSilver,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(56.dp)
        )
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { onExposureChanged(it.roundToInt()) },
            valueRange = minIndex.toFloat()..maxIndex.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = LeicaWhite,
                activeTrackColor = LeicaWhite,
                inactiveTrackColor = LeicaDarkGray
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun FilterSelectorStrip(
    selectedFilter: PhotoFilter,
    onSelectFilter: (PhotoFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LeicaBlack.copy(alpha = 0.82f))
    ) {
        items(PhotoFilter.entries) { filter ->
            val isSelected = filter == selectedFilter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelectFilter(filter) }
                    .padding(vertical = 4.dp)
            ) {
                FilterPreviewSwatch(
                    filter = filter,
                    isSelected = isSelected
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = filter.displayName,
                    color = if (isSelected) LeicaWhite else LeicaSilver,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun FilterPreviewSwatch(
    filter: PhotoFilter,
    isSelected: Boolean
) {
    val matrix = remember(filter) { ColorMatrix(filter.colorMatrixValues) }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LeicaRed else LeicaDarkGray,
                shape = CircleShape
            )
            .graphicsLayer {
                colorFilter = ColorFilter.colorMatrix(matrix)
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD85A4A),
                        Color(0xFFE6B87D),
                        Color(0xFF4B79A1),
                        Color(0xFF1F2421)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        }
    }
}

@Composable
private fun BottomCameraDeck(
    state: CameraUiState,
    onOpenLatestPhoto: () -> Unit,
    onShutterClick: () -> Unit,
    onToggleFilters: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LatestPhotoThumbnail(
                uri = state.latestPhotoUri,
                onClick = onOpenLatestPhoto
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LeicaAnthracite)
                    .border(1.dp, LeicaDarkGray, CircleShape)
                    .clickable { onCycleAspectRatio() }
            ) {
                Text(
                    text = state.aspectRatio.label,
                    color = LeicaWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(2.5.dp, LeicaWhite, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onShutterClick
                )
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(LeicaWhite)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (state.isSavingPhoto) LeicaRed else LeicaSilver.copy(alpha = 0.35f))
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (state.showFilterStrip) LeicaWhite else LeicaAnthracite)
                .border(
                    width = 1.dp,
                    color = if (state.showFilterStrip) LeicaWhite else LeicaDarkGray,
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable { onToggleFilters() }
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(LeicaRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = state.selectedFilter.shortTag,
                    color = if (state.showFilterStrip) LeicaBlack else LeicaWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun LatestPhotoThumbnail(
    uri: Uri?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbBitmap by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri == null) {
            thumbBitmap = null
            return@LaunchedEffect
        }
        thumbBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LeicaAnthracite)
            .border(1.5.dp, LeicaDarkGray, CircleShape)
            .clickable(enabled = uri != null, onClick = onClick)
    ) {
        if (thumbBitmap != null) {
            Image(
                bitmap = thumbBitmap!!,
                contentDescription = "Ultima fotografia scattata",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(LeicaDarkGray, CircleShape)
            )
        }
    }
}

@Composable
private fun FramingGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 0.75.dp.toPx()
        val lineColor = Color.White.copy(alpha = 0.22f)

        val thirdW = size.width / 3f
        val thirdH = size.height / 3f

        drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth)
        drawLine(lineColor, Offset(thirdW * 2, 0f), Offset(thirdW * 2, size.height), strokeWidth)
        drawLine(lineColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth)
        drawLine(lineColor, Offset(0f, thirdH * 2), Offset(size.width, thirdH * 2), strokeWidth)

        val cx = size.width / 2f
        val cy = size.height / 2f
        val arm = 8.dp.toPx()
        drawLine(lineColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth)
        drawLine(lineColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth)
        drawCircle(lineColor, radius = 2.dp.toPx(), center = Offset(cx, cy), style = Stroke(strokeWidth))
    }
}

@Composable
private fun EssentialSettingsDialog(
    state: CameraUiState,
    onDismiss: () -> Unit,
    onToggleSaveOriginal: (Boolean) -> Unit,
    onToggleGrid: (Boolean) -> Unit,
    onToggleHighRes: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LeicaAnthracite,
        titleContentColor = LeicaWhite,
        textContentColor = LeicaSilver,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(LeicaRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "IMPOSTAZIONI FOTOCAMERA",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingToggleRow(
                    title = "Massima Risoluzione Sensore",
                    subtitle = "Salva alla risoluzione nativa massima",
                    checked = state.highResolutionTier,
                    onCheckedChange = onToggleHighRes
                )
                SettingToggleRow(
                    title = "Conserva Originale Non Filtrato",
                    subtitle = "Salva un secondo JPEG neutro oltre al filtro",
                    checked = state.saveOriginalCopy,
                    onCheckedChange = onToggleSaveOriginal
                )
                SettingToggleRow(
                    title = "Griglia Telemetro",
                    subtitle = "Mostra linee guida dei terzi e mirino centrale",
                    checked = state.showFramingGrid,
                    onCheckedChange = onToggleGrid
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CHIUDI", color = LeicaWhite, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = LeicaWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = LeicaSilver, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LeicaWhite,
                checkedTrackColor = LeicaRed,
                uncheckedThumbColor = LeicaSilver,
                uncheckedTrackColor = LeicaDarkGray
            )
        )
    }
}

@Composable
private fun FullScreenPhotoDialog(
    photoUri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fullBitmap by remember(photoUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(photoUri) {
        fullBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(photoUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LeicaBlack)
        ) {
            fullBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = "Fotografia a schermo intero",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(LeicaAnthracite.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Chiudi", tint = LeicaWhite)
            }
        }
    }
}