package com.vanu.videocut.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.vanu.videocut.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoMeta {
    suspend fun load(context: Context, uri: Uri): Result<VideoSource> = withContext(Dispatchers.IO) {
        try {
            val name = queryDisplayName(context, uri) ?: "video"
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
                val (w, h) = if (rotation == 90 || rotation == 270) height to width else width to height
                if (duration <= 0L) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Could not read video duration. Try another file.")
                    )
                }
                Result.success(
                    VideoSource(
                        uri = uri,
                        displayName = name,
                        durationMs = duration,
                        width = w.coerceAtLeast(1),
                        height = h.coerceAtLeast(1),
                    )
                )
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
        } catch (_: Exception) {
            null
        } ?: uri.lastPathSegment
    }
}
