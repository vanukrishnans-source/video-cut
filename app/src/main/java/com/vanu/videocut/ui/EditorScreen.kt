package com.vanu.videocut.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vanu.videocut.VideoCutViewModel
import com.vanu.videocut.model.AspectPreset
import com.vanu.videocut.model.Segment
import com.vanu.videocut.ui.components.CropOverlay
import com.vanu.videocut.ui.components.DualRangeSlider
import com.vanu.videocut.ui.theme.Amber
import com.vanu.videocut.ui.theme.CardDark
import com.vanu.videocut.ui.theme.CardElevated
import com.vanu.videocut.ui.theme.Purple
import com.vanu.videocut.ui.theme.TextPrimary
import com.vanu.videocut.ui.theme.TextSecondary
import com.vanu.videocut.util.TimeFormat

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    viewModel: VideoCutViewModel,
    onPickVideos: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val active = state.activeSource

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(active?.uri) {
        val src = active ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(src.uri))
        player.prepare()
        player.seekTo(state.trimStartMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Video Cut",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            ),
        )
        Text(
            "Trim · Crop · Merge segments",
            color = TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(12.dp))

        // Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, CardElevated, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (active != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
                CropOverlay(
                    crop = state.crop,
                    onCropChange = viewModel::setCrop,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = {
                        if (player.isPlaying) player.pause() else {
                            player.seekTo(state.trimStartMs)
                            player.play()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(56.dp)
                        .background(Purple.copy(alpha = 0.85f), RoundedCornerShape(28.dp)),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = TextPrimary)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Pick a video to start", color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onPickVideos,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add video(s)", fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.sources.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.sources.forEach { src ->
                    val selected = src.id == state.activeSourceId
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectSource(src.id) },
                        label = {
                            Text(
                                src.displayName.take(18) + if (src.displayName.length > 18) "…" else "",
                                maxLines = 1,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple,
                            selectedLabelColor = TextPrimary,
                        ),
                    )
                }
            }
        }

        if (active != null) {
            Spacer(Modifier.height(16.dp))
            Text("Trim range", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                "Drag a handle (or type times) — tapping elsewhere keeps your selection",
                color = TextSecondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))

            DualRangeSlider(
                startMs = state.trimStartMs,
                endMs = state.trimEndMs,
                durationMs = active.durationMs,
                onRangeChange = viewModel::setTrimRange,
                onScrubMs = { ms -> player.seekTo(ms) },
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeField(
                    label = "Start",
                    valueMs = state.trimStartMs,
                    onCommit = {
                        viewModel.setTrimStart(it)
                        player.seekTo(it)
                    },
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "End",
                    valueMs = state.trimEndMs,
                    onCommit = {
                        viewModel.setTrimEnd(it)
                        player.seekTo(it)
                    },
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Len", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        TimeFormat.formatShort(state.trimEndMs - state.trimStartMs),
                        color = Amber,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = viewModel::addSegment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = CardDark),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Segment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text("Crop", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AspectPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.aspectPreset == preset,
                        onClick = { viewModel.setAspectPreset(preset) },
                        label = { Text(preset.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple,
                            selectedLabelColor = TextPrimary,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Segments (${state.segments.size})",
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        if (state.segments.isEmpty()) {
            Text(
                "Add segments from one or more videos. They merge in list order on export.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Spacer(Modifier.height(8.dp))
            state.segments.forEachIndexed { index, seg ->
                SegmentRow(
                    index = index,
                    segment = seg,
                    onUp = { viewModel.moveSegment(seg.id, up = true) },
                    onDown = { viewModel.moveSegment(seg.id, up = false) },
                    onDelete = { viewModel.removeSegment(seg.id) },
                    onPreview = {
                        player.setMediaItem(MediaItem.fromUri(seg.sourceUri))
                        player.prepare()
                        player.seekTo(seg.startMs)
                        player.play()
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            val totalMs = state.segments.sumOf { it.durationMs }
            Text(
                "Total duration: ${TimeFormat.formatShort(totalMs)}",
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (state.isExporting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardElevated)
                    .padding(16.dp),
            ) {
                Text(state.exportMessage ?: "Exporting…", color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.exportProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Purple,
                )
                Text(
                    "${(state.exportProgress * 100).toInt()}%",
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::cancelExport) {
                    Text("Cancel")
                }
            }
        } else {
            Button(
                onClick = viewModel::export,
                enabled = state.segments.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
            ) {
                Text("Export merged MP4", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        state.exportMessage?.takeIf { !state.isExporting }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Amber)
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::clearError) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TimeField(
    label: String,
    valueMs: Long,
    onCommit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(valueMs) { mutableStateOf(TimeFormat.format(valueMs)) }
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Purple),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    TimeFormat.parse(text)?.let(onCommit)
                        ?: run { text = TimeFormat.format(valueMs) }
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardElevated)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    inner()
                }
            },
        )
    }
}

@Composable
private fun SegmentRow(
    index: Int,
    segment: Segment,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "#${index + 1}  ${segment.sourceName.take(22)}",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "${TimeFormat.formatShort(segment.startMs)} → ${TimeFormat.formatShort(segment.endMs)}  ·  ${TimeFormat.formatShort(segment.durationMs)}",
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
        IconButton(onClick = onPreview) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = Amber)
        }
        IconButton(onClick = onUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = TextSecondary)
        }
        IconButton(onClick = onDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = TextSecondary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
