package com.leicacamera.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import com.leicacamera.app.filter.AspectRatioMode
import com.leicacamera.app.filter.FilterEngine
import com.leicacamera.app.filter.PhotoFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStoreRepository(private val context: Context) {

    companion object {
        private const val SUBFOLDER = "Leica Inspired Camera"
        private const val RELATIVE_PATH = "Pictures/$SUBFOLDER"
    }

    suspend fun getLatestCapturedPhotoUri(): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("LCAM_%"),
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idCol)
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else null
            }
        }.getOrNull()
    }

    suspend fun processAndSavePhoto(
        imageProxy: ImageProxy,
        filter: PhotoFilter,
        aspectRatio: AspectRatioMode,
        isFrontCamera: Boolean,
        saveOriginalCopy: Boolean
    ): Result<Uri> = withContext(Dispatchers.Default) {
        runCatching {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalStateException("Impossibile decodificare il buffer dell'immagine.")

            val orientedBitmap = orientBitmap(rawBitmap, rotationDegrees, isFrontCamera)
            if (orientedBitmap !== rawBitmap) rawBitmap.recycle()

            val croppedBitmap = FilterEngine.cropToAspectRatio(orientedBitmap, aspectRatio)
            if (croppedBitmap !== orientedBitmap) orientedBitmap.recycle()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

            if (saveOriginalCopy && filter != PhotoFilter.NONE) {
                saveBitmapToMediaStore(
                    bitmap = croppedBitmap,
                    displayName = "LCAM_${timestamp}_ORIGINAL.jpg",
                    filterTag = "ORIGINAL"
                )
            }

            val processedBitmap = FilterEngine.applyFilter(croppedBitmap, filter)
            if (processedBitmap !== croppedBitmap) croppedBitmap.recycle()

            val savedUri = saveBitmapToMediaStore(
                bitmap = processedBitmap,
                displayName = "LCAM_${timestamp}_${filter.shortTag}.jpg",
                filterTag = filter.displayName
            )
            processedBitmap.recycle()
            savedUri
        }
    }

    private fun orientBitmap(source: Bitmap, rotationDegrees: Int, mirrorHorizontally: Boolean): Bitmap {
        if (rotationDegrees == 0 && !mirrorHorizontally) return source
        val matrix = Matrix().apply {
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
            if (mirrorHorizontally) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private suspend fun saveBitmapToMediaStore(
        bitmap: Bitmap,
        displayName: String,
        filterTag: String
    ): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Creazione voce MediaStore fallita.")

            try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 96, outStream)) {
                        throw IllegalStateException("Compressione JPEG fallita.")
                    }
                    outStream.flush()
                } ?: throw IllegalStateException("OutputStream nullo.")

                resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Leica Inspired Camera ($filterTag)")
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    exif.saveAttributes()
                }

                val publishValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, publishValues, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, SUBFOLDER).apply { if (!exists()) mkdirs() }
            val imageFile = File(targetDir, displayName)

            FileOutputStream(imageFile).use { outStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 96, outStream)) {
                    throw IllegalStateException("Compressione JPEG fallita.")
                }
                outStream.flush()
            }

            val exif = ExifInterface(imageFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Leica Inspired Camera ($filterTag)")
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.saveAttributes()

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: Uri.fromFile(imageFile)
        }
    }
}