package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

class ImageStorageManager(private val context: Context) {

    private val photoDirectory: File by lazy {
        File(context.filesDir, "aura_photos").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Safely compresses and saves an image from content URI into internal storage.
     * Prevents Out Of Memory (OOM) by calculating inSampleSize before decoding.
     */
    suspend fun saveAndCompressImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // First decode bounds only
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxTargetDimension = 1280
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            var sampleSize = 1
            val maxOriginal = max(originalWidth, originalHeight)
            if (maxOriginal > maxTargetDimension) {
                sampleSize = maxOriginal / maxTargetDimension
            }

            // Now decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.RGB_565 // Half memory footprint compared to ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return@withContext null

            val targetFile = File(photoDirectory, "photo_${System.currentTimeMillis()}_${(100..999).random()}.jpg")
            val outputStream = FileOutputStream(targetFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle() // Explicitly release native bitmap memory

            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely deletes an image file by path.
     */
    fun deleteImage(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists() && file.parentFile?.absolutePath == photoDirectory.absolutePath) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Calculates total storage used by diary photos and audio files.
     */
    fun calculateStorageUsageBytes(): Long {
        var total = 0L
        photoDirectory.listFiles()?.forEach { total += it.length() }
        File(context.filesDir, "aura_audio").listFiles()?.forEach { total += it.length() }
        return total
    }
}
