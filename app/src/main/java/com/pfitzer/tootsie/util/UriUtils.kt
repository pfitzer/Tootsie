package com.pfitzer.tootsie.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * Utility functions for working with URIs.
 */

/**
 * Converts a content URI to a temporary File.
 * Copies the content from the URI to a temporary file in the cache directory.
 *
 * @param context Android context
 * @return Temporary File, or null if conversion fails
 */
fun Uri.toFile(context: Context): File? {
    return try {
        val contentResolver = context.contentResolver

        // Get file name from URI
        val fileName = contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "temp_${System.currentTimeMillis()}"

        // Create temporary file in cache directory
        val tempFile = File(context.cacheDir, fileName)

        // Copy content from URI to file
        contentResolver.openInputStream(this)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Gets the MIME type of a content URI.
 *
 * @param context Android context
 * @return MIME type string, or null if not available
 */
fun Uri.getMimeType(context: Context): String? {
    return context.contentResolver.getType(this)
}

/**
 * Gets the file size of a content URI in bytes.
 *
 * @param context Android context
 * @return File size in bytes, or null if not available
 */
fun Uri.getFileSize(context: Context): Long? {
    return try {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        }
    } catch (e: Exception) {
        null
    }
}
