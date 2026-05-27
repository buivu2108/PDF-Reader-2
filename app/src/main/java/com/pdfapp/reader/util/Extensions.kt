package com.pdfapp.reader.util

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Convert byte count to human-readable file size string (e.g. "2.3 MB"). */
fun Long.toReadableSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$this B"
    }
}

/** Convert epoch millis to locale-formatted date string (e.g. "Jan 15, 2024"). */
fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

/** URL-encode a URI string for safe use as a navigation argument. */
fun String.toEncodedUri(): String = Uri.encode(this)

/** URL-decode a previously encoded URI string. */
fun String.toDecodedUri(): String = Uri.decode(this)
