package com.vanu.videocut.model

import android.net.Uri
import java.util.UUID

enum class AspectPreset(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_1_1("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    FREE("Free", null),
}

/**
 * Normalized crop rect in [0,1] video-frame space (top-left origin).
 * left/top/right/bottom.
 */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val isFull: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    fun width() = (right - left).coerceAtLeast(0.05f)
    fun height() = (bottom - top).coerceAtLeast(0.05f)
}

data class VideoSource(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
)

data class Segment(
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val sourceUri: Uri,
    val sourceName: String,
    val sourceDurationMs: Long,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0)
}

data class ProjectState(
    val sources: List<VideoSource> = emptyList(),
    val activeSourceId: String? = null,
    val segments: List<Segment> = emptyList(),
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val crop: CropRect = CropRect(),
    val aspectPreset: AspectPreset = AspectPreset.ORIGINAL,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportMessage: String? = null,
    val lastExportUri: Uri? = null,
    val error: String? = null,
) {
    val activeSource: VideoSource?
        get() = sources.firstOrNull { it.id == activeSourceId } ?: sources.firstOrNull()
}
