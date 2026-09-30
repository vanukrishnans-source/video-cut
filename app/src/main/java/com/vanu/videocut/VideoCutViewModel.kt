package com.vanu.videocut

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vanu.videocut.export.ExportRequest
import com.vanu.videocut.export.VideoExporter
import com.vanu.videocut.model.AspectPreset
import com.vanu.videocut.model.CropRect
import com.vanu.videocut.model.ProjectState
import com.vanu.videocut.model.Segment
import com.vanu.videocut.util.VideoMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoCutViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ProjectState())
    val state: StateFlow<ProjectState> = _state.asStateFlow()

    private val exporter = VideoExporter(app.applicationContext)

    fun clearError() {
        _state.update { it.copy(error = null, exportMessage = null) }
    }

    fun addVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val loaded = mutableListOf<com.vanu.videocut.model.VideoSource>()
            val errors = mutableListOf<String>()
            for (uri in uris) {
                VideoMeta.load(getApplication(), uri).fold(
                    onSuccess = { loaded += it },
                    onFailure = { errors += (it.message ?: "Failed to load video") },
                )
            }
            if (loaded.isNotEmpty()) {
                _state.update { s ->
                    val sources = s.sources + loaded
                    val active = s.activeSourceId ?: loaded.first().id
                    val activeSrc = sources.first { it.id == active }
                    s.copy(
                        sources = sources,
                        activeSourceId = active,
                        trimStartMs = 0L,
                        trimEndMs = activeSrc.durationMs,
                        error = errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                    )
                }
            } else if (errors.isNotEmpty()) {
                _state.update { it.copy(error = errors.joinToString("\n")) }
            }
        }
    }

    fun selectSource(id: String) {
        _state.update { s ->
            val src = s.sources.firstOrNull { it.id == id } ?: return@update s
            s.copy(
                activeSourceId = id,
                trimStartMs = 0L,
                trimEndMs = src.durationMs,
            )
        }
    }

    fun setTrimRange(startMs: Long, endMs: Long) {
        _state.update { s ->
            val max = s.activeSource?.durationMs ?: return@update s
            val start = startMs.coerceIn(0L, max - 1)
            val end = endMs.coerceIn(start + 1, max)
            s.copy(trimStartMs = start, trimEndMs = end)
        }
    }

    fun setTrimStart(startMs: Long) {
        val s = _state.value
        setTrimRange(startMs, s.trimEndMs)
    }

    fun setTrimEnd(endMs: Long) {
        val s = _state.value
        setTrimRange(s.trimStartMs, endMs)
    }

    fun addSegment() {
        _state.update { s ->
            val src = s.activeSource ?: return@update s.copy(error = "Pick a video first")
            if (s.trimEndMs <= s.trimStartMs) {
                return@update s.copy(error = "End must be after start")
            }
            val seg = Segment(
                sourceId = src.id,
                sourceUri = src.uri,
                sourceName = src.displayName,
                sourceDurationMs = src.durationMs,
                startMs = s.trimStartMs,
                endMs = s.trimEndMs,
            )
            s.copy(segments = s.segments + seg, error = null)
        }
    }

    fun removeSegment(id: String) {
        _state.update { it.copy(segments = it.segments.filterNot { seg -> seg.id == id }) }
    }

    fun moveSegment(id: String, up: Boolean) {
        _state.update { s ->
            val list = s.segments.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx < 0) return@update s
            val swapWith = if (up) idx - 1 else idx + 1
            if (swapWith !in list.indices) return@update s
            val tmp = list[idx]
            list[idx] = list[swapWith]
            list[swapWith] = tmp
            s.copy(segments = list)
        }
    }

    fun setAspectPreset(preset: AspectPreset) {
        _state.update { s ->
            val src = s.activeSource
            val crop = if (preset == AspectPreset.ORIGINAL || preset == AspectPreset.FREE || src == null) {
                if (preset == AspectPreset.ORIGINAL) CropRect() else s.crop
            } else {
                fitAspect(src.width, src.height, preset.ratio!!)
            }
            s.copy(aspectPreset = preset, crop = crop)
        }
    }

    fun setCrop(crop: CropRect) {
        _state.update { it.copy(crop = crop.sanitized(), aspectPreset = AspectPreset.FREE) }
    }

    fun export() {
        val s = _state.value
        if (s.segments.isEmpty()) {
            _state.update { it.copy(error = "Add at least one segment") }
            return
        }
        if (s.isExporting) return
        viewModelScope.launch {
            _state.update {
                it.copy(isExporting = true, exportProgress = 0f, exportMessage = "Exporting…", error = null)
            }
            try {
                val uri = exporter.export(
                    ExportRequest(segments = s.segments, crop = s.crop),
                    onProgress = { p ->
                        _state.update { st -> st.copy(exportProgress = p) }
                    },
                )
                _state.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 1f,
                        lastExportUri = uri,
                        exportMessage = "Saved to Movies/VideoCut",
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = null,
                        error = e.message ?: "Export failed",
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exporter.cancel()
        _state.update {
            it.copy(isExporting = false, exportProgress = 0f, exportMessage = "Export cancelled")
        }
    }

    private fun fitAspect(videoW: Int, videoH: Int, target: Float): CropRect {
        val vw = videoW.toFloat().coerceAtLeast(1f)
        val vh = videoH.toFloat().coerceAtLeast(1f)
        val videoAspect = vw / vh
        return if (videoAspect > target) {
            // too wide — crop sides
            val newW = vh * target
            val side = ((vw - newW) / 2f) / vw
            CropRect(left = side, top = 0f, right = 1f - side, bottom = 1f)
        } else {
            // too tall — crop top/bottom
            val newH = vw / target
            val side = ((vh - newH) / 2f) / vh
            CropRect(left = 0f, top = side, right = 1f, bottom = 1f - side)
        }
    }

    private fun CropRect.sanitized(): CropRect {
        val l = left.coerceIn(0f, 0.9f)
        val t = top.coerceIn(0f, 0.9f)
        val r = right.coerceIn(l + 0.05f, 1f)
        val b = bottom.coerceIn(t + 0.05f, 1f)
        return CropRect(l, t, r, b)
    }
}
