package com.leicacamera.app.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

enum class PhotoFilter(
    val id: String,
    val displayName: String,
    val shortTag: String,
    val colorMatrixValues: FloatArray,
    val contrastStrength: Float = 0f,
    val blackPointLift: Int = 0,
    val highlightCompression: Int = 0,
    val vignetteStrength: Float = 0f,
    val tintOverlayArgb: Int = 0x00000000
) {
    NONE(
        id = "none",
        displayName = "Off",
        shortTag = "STD",
        colorMatrixValues = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0f,
        vignetteStrength = 0f
    ),

    NATURAL(
        id = "natural",
        displayName = "Natural",
        shortTag = "NAT",
        colorMatrixValues = floatArrayOf(
            1.05f, -0.02f, -0.01f, 0f, -2f,
            -0.01f, 1.04f, -0.01f, 0f, -2f,
            -0.02f, -0.02f, 1.06f, 0f, -1f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.12f,
        vignetteStrength = 0.12f
    ),

    LEICA_VIVID(
        id = "leica_vivid",
        displayName = "Leica Vivid",
        shortTag = "VIV",
        colorMatrixValues = floatArrayOf(
            1.22f, -0.08f, -0.04f, 0f, -8f,
            -0.05f, 1.16f, -0.04f, 0f, -8f,
            -0.06f, -0.08f, 1.24f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.32f,
        blackPointLift = -8,
        vignetteStrength = 0.25f
    ),

    CLASSIC_CHROME(
        id = "classic_chrome",
        displayName = "Classic Chrome",
        shortTag = "CHR",
        colorMatrixValues = floatArrayOf(
            0.88f, 0.08f, 0.02f, 0f, -4f,
            0.04f, 0.92f, 0.04f, 0f, -3f,
            0.02f, 0.08f, 0.96f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.26f,
        blackPointLift = -4,
        highlightCompression = 8,
        vignetteStrength = 0.22f,
        tintOverlayArgb = 0x101A2830
    ),

    MONOCHROME(
        id = "monochrome",
        displayName = "Monochrome",
        shortTag = "MONO",
        colorMatrixValues = floatArrayOf(
            0.34f, 0.56f, 0.10f, 0f, 0f,
            0.34f, 0.56f, 0.10f, 0f, 0f,
            0.34f, 0.56f, 0.10f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.20f,
        vignetteStrength = 0.20f
    ),

    MONOCHROME_HC(
        id = "monochrome_hc",
        displayName = "Monochrome HC",
        shortTag = "M-HC",
        colorMatrixValues = floatArrayOf(
            0.48f, 0.62f, 0.12f, 0f, -18f,
            0.48f, 0.62f, 0.12f, 0f, -18f,
            0.48f, 0.62f, 0.12f, 0f, -18f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.52f,
        blackPointLift = -14,
        vignetteStrength = 0.35f
    ),

    WARM_FILM(
        id = "warm_film",
        displayName = "Warm Film",
        shortTag = "WARM",
        colorMatrixValues = floatArrayOf(
            1.10f, 0.04f, -0.02f, 0f, 6f,
            0.02f, 1.03f, -0.02f, 0f, 3f,
            -0.04f, -0.02f, 0.90f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.14f,
        blackPointLift = 8,
        highlightCompression = 10,
        vignetteStrength = 0.20f,
        tintOverlayArgb = 0x12E8A850
    ),

    VINTAGE(
        id = "vintage",
        displayName = "Vintage",
        shortTag = "VNTG",
        colorMatrixValues = floatArrayOf(
            0.85f, 0.12f, 0.05f, 0f, 12f,
            0.06f, 0.86f, 0.06f, 0f, 10f,
            0.04f, 0.10f, 0.78f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = -0.08f,
        blackPointLift = 18,
        highlightCompression = 14,
        vignetteStrength = 0.30f,
        tintOverlayArgb = 0x14D4B483
    ),

    CINEMATIC(
        id = "cinematic",
        displayName = "Cinematic",
        shortTag = "CINE",
        colorMatrixValues = floatArrayOf(
            1.06f, -0.03f, -0.02f, 0f, 2f,
            -0.02f, 1.02f, 0.03f, 0f, 0f,
            -0.05f, 0.04f, 1.08f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.28f,
        blackPointLift = 4,
        highlightCompression = 6,
        vignetteStrength = 0.28f,
        tintOverlayArgb = 0x14103842
    ),

    SOFT(
        id = "soft",
        displayName = "Soft",
        shortTag = "SOFT",
        colorMatrixValues = floatArrayOf(
            0.96f, 0.04f, 0.01f, 0f, 8f,
            0.02f, 0.96f, 0.02f, 0f, 7f,
            0.01f, 0.03f, 0.94f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = -0.15f,
        blackPointLift = 12,
        highlightCompression = 16,
        vignetteStrength = 0.10f
    ),

    STREET(
        id = "street",
        displayName = "Street",
        shortTag = "STRT",
        colorMatrixValues = floatArrayOf(
            1.12f, -0.04f, -0.02f, 0f, -10f,
            -0.02f, 0.98f, -0.02f, 0f, -8f,
            -0.04f, -0.02f, 0.94f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        ),
        contrastStrength = 0.38f,
        blackPointLift = -10,
        vignetteStrength = 0.32f
    );

    companion object {
        fun fromId(id: String): PhotoFilter =
            entries.find { it.id == id } ?: NATURAL
    }
}

object FilterEngine {

    fun cropToAspectRatio(source: Bitmap, aspectRatio: AspectRatioMode): Bitmap {
        val srcW = source.width
        val srcH = source.height
        val isPortrait = srcH >= srcW

        val targetRatio = when (aspectRatio) {
            AspectRatioMode.RATIO_4_3 -> if (isPortrait) 3f / 4f else 4f / 3f
            AspectRatioMode.RATIO_16_9 -> if (isPortrait) 9f / 16f else 16f / 9f
            AspectRatioMode.RATIO_1_1 -> 1f
        }

        val currentRatio = srcW.toFloat() / srcH.toFloat()
        if (kotlin.math.abs(currentRatio - targetRatio) < 0.01f) {
            return source
        }

        val cropW: Int
        val cropH: Int
        if (currentRatio > targetRatio) {
            cropH = srcH
            cropW = (srcH * targetRatio).toInt()
        } else {
            cropW = srcW
            cropH = (srcW / targetRatio).toInt()
        }

        val xOffset = (srcW - cropW) / 2
        val yOffset = (srcH - cropH) / 2

        return Bitmap.createBitmap(source, xOffset, yOffset, cropW, cropH)
    }

    fun applyFilter(source: Bitmap, filter: PhotoFilter): Bitmap {
        if (filter == PhotoFilter.NONE) return source

        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val colorMatrix = ColorMatrix(filter.colorMatrixValues)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        if (filter.contrastStrength != 0f || filter.blackPointLift != 0 || filter.highlightCompression != 0) {
            applyToneCurveLutInPlace(output, filter)
        }

        if (filter.tintOverlayArgb != 0) {
            val tintPaint = Paint().apply {
                color = filter.tintOverlayArgb
                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            }
            canvas.drawRect(Rect(0, 0, width, height), tintPaint)
        }

        if (filter.vignetteStrength > 0f) {
            val radius = max(width, height) * 0.72f
            val centerX = width / 2f
            val centerY = height / 2f
            val edgeAlpha = (filter.vignetteStrength * 255).toInt().coerceIn(0, 180)
            val edgeColor = (edgeAlpha shl 24) or 0x000000

            val radialGradient = RadialGradient(
                centerX,
                centerY,
                radius,
                intArrayOf(0x00000000, 0x00000000, edgeColor),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = radialGradient
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        }

        return output
    }

    private fun applyToneCurveLutInPlace(bitmap: Bitmap, filter: PhotoFilter) {
        val lut = IntArray(256)
        val contrast = filter.contrastStrength
        val blackLift = filter.blackPointLift
        val highlightComp = filter.highlightCompression

        for (i in 0..255) {
            var normalized = i / 255.0
            if (contrast != 0f) {
                val curve = normalized - 0.25 * contrast * kotlin.math.sin(2.0 * Math.PI * normalized)
                normalized = curve.coerceIn(0.0, 1.0)
            }
            var mapped = (normalized * 255.0).toInt()

            val minOut = max(0, blackLift)
            val maxOut = 255 - highlightComp
            if (blackLift < 0) {
                mapped = ((mapped + blackLift).toFloat() / (255f + blackLift) * 255f).toInt()
            }
            mapped = minOut + (mapped * (maxOut - minOut) / 255)
            lut[i] = min(255, max(0, mapped))
        }

        val width = bitmap.width
        val height = bitmap.height
        val rowPixels = IntArray(width)

        for (y in 0 until height) {
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                val pixel = rowPixels[x]
                val a = pixel and -0x1000000
                val r = lut[(pixel shr 16) and 0xFF]
                val g = lut[(pixel shr 8) and 0xFF]
                val b = lut[pixel and 0xFF]
                rowPixels[x] = a or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(rowPixels, 0, width, 0, y, width, 1)
        }
    }
}

enum class AspectRatioMode(val label: String, val ratioValue: Float) {
    RATIO_4_3("4:3", 3f / 4f),
    RATIO_16_9("16:9", 9f / 16f),
    RATIO_1_1("1:1", 1f);

    fun next(): AspectRatioMode = when (this) {
        RATIO_4_3 -> RATIO_16_9
        RATIO_16_9 -> RATIO_1_1
        RATIO_1_1 -> RATIO_4_3
    }
}