package com.pdfapp.reader.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

private const val TAG = "FileUtils"

object FileUtils {

    /** Get display name from a content URI. */
    fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
        }
        return name
    }

    /** Get file size from a content URI. */
    fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                size = cursor.getLong(0)
            }
        }
        return size
    }

    /** Create a share intent for a PDF file. */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Extract folder name from a file path. */
    fun getFolderName(path: String): String {
        val parent = File(path).parentFile
        return parent?.name ?: "Unknown"
    }

    /** Get file name without extension from a content URI. */
    fun getFileNameWithoutExtension(contentResolver: ContentResolver, uri: Uri): String? {
        val name = getFileName(contentResolver, uri) ?: return null
        return name.substringBeforeLast(".", name)
    }

    /** Copy a content URI to a temporary file for processing. */
    suspend fun copyToTempFile(context: Context, uri: Uri, prefix: String = "pdf"): File? {
        Log.d(TAG, "copyToTempFile: uri=$uri, scheme=${uri.scheme}, authority=${uri.authority}")
        return try {
            val tempFile = File.createTempFile(prefix, ".pdf", context.cacheDir)

            // Try content resolver first, then fallback to file path
            val inputStream = openStreamWithFallback(context, uri)
            if (inputStream == null) {
                Log.e(TAG, "copyToTempFile: all stream methods failed for uri=$uri")
                tempFile.delete()
                return null
            }

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val bytes = input.copyTo(output)
                    Log.d(TAG, "copyToTempFile: copied $bytes bytes to ${tempFile.absolutePath}")
                }
            }
            if (tempFile.length() == 0L) {
                Log.e(TAG, "copyToTempFile: temp file is EMPTY after copy")
                tempFile.delete()
                return null
            }
            Log.d(TAG, "copyToTempFile: SUCCESS, size=${tempFile.length()}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "copyToTempFile: EXCEPTION for uri=$uri", e)
            null
        }
    }

    /** Try content resolver, then fall back to direct file access. */
    private fun openStreamWithFallback(context: Context, uri: Uri): InputStream? {
        // Attempt 1: Content resolver (works for content:// and file:// URIs with permissions)
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) {
                Log.d(TAG, "openStreamWithFallback: contentResolver succeeded")
                return stream
            }
            Log.w(TAG, "openStreamWithFallback: openInputStream returned null")
        } catch (e: SecurityException) {
            Log.e(TAG, "openStreamWithFallback: SecurityException — ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "openStreamWithFallback: ${e.javaClass.simpleName} — ${e.message}")
        }

        // Attempt 2: Direct file path for file:// URIs
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    Log.d(TAG, "openStreamWithFallback: file:// path fallback succeeded")
                    return file.inputStream()
                }
            }
        }

        // Attempt 3: Query DATA column for the file path (content:// MediaStore URIs)
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri, arrayOf(MediaStore.Files.FileColumns.DATA), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val path = cursor.getString(0)
                        if (!path.isNullOrEmpty()) {
                            val file = File(path)
                            if (file.exists() && file.canRead()) {
                                Log.d(TAG, "openStreamWithFallback: DATA column fallback path=$path")
                                return file.inputStream()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "openStreamWithFallback: DATA column query failed — ${e.message}")
            }
        }

        return null
    }
}
