package com.example.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageUploadHelper {
    fun createCameraTempUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val file = File(dir, "tfc_food_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun compressUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimension: Int = 460,
        quality: Int = 72
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Unable to read image dimensions"))
            }

            var sampleSize = 1
            val longest = max(origW, origH)
            while (longest / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalStateException("Could not decode image"))

            val scale = minOf(
                1f,
                maxDimension.toFloat() / decoded.width.toFloat(),
                maxDimension.toFloat() / decoded.height.toFloat()
            )
            val scaled = if (scale < 1f) {
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()

            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64"
            if (dataUrl.length > 240_000) {
                return@withContext Result.failure(IllegalStateException("Compressed image is still too large"))
            }
            Result.success(dataUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
