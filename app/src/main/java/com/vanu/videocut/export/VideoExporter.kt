package com.vanu.videocut.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.vanu.videocut.model.CropRect
import com.vanu.videocut.model.Segment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExportRequest(
    val segments: List<Segment>,
    val crop: CropRect,
)

@UnstableApi
class VideoExporter(private val context: Context) {

    @Volatile
    private var activeTransformer: Transformer? = null

    fun cancel() {
        try {
            activeTransformer?.cancel()
        } catch (_: Exception) {
        }
        activeTransformer = null
    }

    suspend fun export(
        request: ExportRequest,
        onProgress: (Float) -> Unit,
    ): Uri {
        require(request.segments.isNotEmpty()) { "No segments to export" }

        val cacheDir = File(context.cacheDir, "export").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(cacheDir, "VideoCut_$stamp.mp4")
        if (outFile.exists()) outFile.delete()

        val editedItems = request.segments.map { seg ->
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(seg.startMs.coerceAtLeast(0))
                .setEndPositionMs(seg.endMs.coerceAtLeast(seg.startMs + 1))
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(seg.sourceUri)
                .setClippingConfiguration(clipping)
                .build()

            val videoEffects = buildList {
                if (!request.crop.isFull) {
                    // Media3 Crop: left/right/bottom/top in NDC [-1,1], Y up.
                    val left = request.crop.left * 2f - 1f
                    val right = request.crop.right * 2f - 1f
                    val top = 1f - request.crop.top * 2f
                    val bottom = 1f - request.crop.bottom * 2f
                    add(Crop(left, right, bottom, top))
                }
                add(Presentation.createForHeight(1080))
            }

            EditedMediaItem.Builder(mediaItem)
                .setDurationUs(((seg.endMs - seg.startMs).coerceAtLeast(1)) * 1000)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()
        }

        val sequence = EditedMediaItemSequence.Builder(editedItems).build()
        val composition = Composition.Builder(sequence).build()

        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val handler = Handler(Looper.getMainLooper())
                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            activeTransformer = null
                            handler.removeCallbacksAndMessages(null)
                            if (cont.isActive) cont.resume(Unit)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            activeTransformer = null
                            handler.removeCallbacksAndMessages(null)
                            if (cont.isActive) cont.resumeWithException(exportException)
                        }
                    })
                    .build()

                activeTransformer = transformer
                cont.invokeOnCancellation {
                    cancel()
                    handler.removeCallbacksAndMessages(null)
                }

                val progressTick = object : Runnable {
                    override fun run() {
                        val t = activeTransformer ?: return
                        val holder = ProgressHolder()
                        val state = t.getProgress(holder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress((holder.progress / 100f).coerceIn(0f, 0.99f))
                        }
                        handler.postDelayed(this, 250)
                    }
                }
                handler.post(progressTick)
                transformer.start(composition, outFile.absolutePath)
            }
        }

        onProgress(1f)
        return publishToGallery(outFile, stamp)
    }

    private suspend fun publishToGallery(file: File, stamp: String): Uri = withContext(Dispatchers.IO) {
        val name = "VideoCut_$stamp.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/VideoCut"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore insert failed")

        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { input -> input.copyTo(out) }
        } ?: throw IllegalStateException("Could not open output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        try {
            val movies = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            if (movies != null) {
                movies.mkdirs()
                file.copyTo(File(movies, name), overwrite = true)
            }
        } catch (_: Exception) {
        }

        try {
            file.delete()
        } catch (_: Exception) {
        }
        uri
    }
}
