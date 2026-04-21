package com.example.reverseengineeringbuildphase.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object ImageSaver {
    suspend fun saveFromUrl(context: Context, imageUrl: String): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = URL(imageUrl).openStream().use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: error("Unable to decode image data.")

            saveBitmapToGallery(context, bitmap)
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri {
        val filename = "generated_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collection, contentValues)
            ?: error("Failed to create MediaStore record.")

        resolver.openOutputStream(itemUri).use { outStream ->
            requireNotNull(outStream) { "Failed to open output stream." }
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)) {
                "Failed to compress bitmap."
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(itemUri, publishValues, null, null)
        }

        return itemUri
    }
}
