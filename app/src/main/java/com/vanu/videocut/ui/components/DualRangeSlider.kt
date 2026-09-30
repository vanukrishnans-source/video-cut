package com.vanu.videocut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vanu.videocut.ui.theme.Amber
import com.vanu.videocut.ui.theme.Purple
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class Thumb { Start, End }

/**
 * Large dual-handle range slider for start/end trim.
 *
 * Handles move ONLY when the user deliberately drags that handle (or Start/End
 * time fields are edited elsewhere). Tapping the track, preview, or elsewhere
 * never jumps or resets the selection. A tap far from both thumbs may scrub
 * the preview position only — it never changes start/end.
 */
@Composable
fun DualRangeSlider(
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    onRangeChange: (Long, Long) -> Unit,
    onScrubMs: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 10.dp,
    thumbRadius: Dp = 22.dp,
    activeColor: Color = Purple,
    inactiveColor: Color = Color(0xFF3A3A44),
    thumbColor: Color = Amber,
) {
    val duration = durationMs.coerceAtLeast(1L)
    val density = LocalDensity.current
    val thumbPx = with(density) { thumbRadius.toPx() }
    val trackH = with(density) { trackHeight.toPx() }

    // Keep latest values for the long-lived pointerInput coroutine so dragging
    // one thumb cannot reset the other to a stale composition-captured value.
    val startMsState = rememberUpdatedState(startMs)
    val endMsState = rememberUpdatedState(endMs)
    val onRangeChangeState = rememberUpdatedState(onRangeChange)
    val onScrubMsState = rememberUpdatedState(onScrubMs)

    var activeThumb by remember { mutableStateOf<Thumb?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbRadius * 2 + 16.dp)
            .padding(horizontal = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2 + 16.dp)
                .pointerInput(duration, thumbPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            val usable = (width - 2 * thumbPx).coerceAtLeast(1f)
                            val curStart = startMsState.value
                            val curEnd = endMsState.value
                            val startX = thumbPx + (curStart.toFloat() / duration) * usable
                            val endX = thumbPx + (curEnd.toFloat() / duration) * usable
                            val dStart = abs(offset.x - startX)
                            val dEnd = abs(offset.x - endX)
                            // Only claim a thumb when the press is clearly on it.
                            val hit = thumbPx * 2.2f
                            activeThumb = when {
                                dStart <= hit && dStart <= dEnd -> Thumb.Start
                                dEnd <= hit && dEnd < dStart -> Thumb.End
                                dStart <= hit -> Thumb.Start
                                dEnd <= hit -> Thumb.End
                                else -> {
                                    // Tap/drag on empty track: scrub preview only —
                                    // never move or reset either handle.
                                    val frac = ((offset.x - thumbPx) / usable).coerceIn(0f, 1f)
                                    val ms = (frac * duration).toLong()
                                    onScrubMsState.value?.invoke(ms)
                                    null
                                }
                            }
                        },
                        onDragEnd = { activeThumb = null },
                        onDragCancel = { activeThumb = null },
                        onDrag = { change, _ ->
                            val thumb = activeThumb ?: return@detectDragGestures
                            change.consume()
                            val width = size.width.toFloat()
                            val usable = (width - 2 * thumbPx).coerceAtLeast(1f)
                            val frac = ((change.position.x - thumbPx) / usable).coerceIn(0f, 1f)
                            val ms = (frac * duration).toLong()
                            val minGap = max(50L, duration / 200) // ~0.05s or 0.5%
                            val curStart = startMsState.value
                            val curEnd = endMsState.value
                            when (thumb) {
                                Thumb.Start -> {
                                    val newStart = min(ms, curEnd - minGap).coerceAtLeast(0L)
                                    onRangeChangeState.value(newStart, curEnd)
                                    onScrubMsState.value?.invoke(newStart)
                                }
                                Thumb.End -> {
                                    val newEnd = max(ms, curStart + minGap).coerceAtMost(duration)
                                    onRangeChangeState.value(curStart, newEnd)
                                    onScrubMsState.value?.invoke(newEnd)
                                }
                            }
                        },
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val usable = (w - 2 * thumbPx).coerceAtLeast(1f)
            val startX = thumbPx + (startMs.toFloat() / duration) * usable
            val endX = thumbPx + (endMs.toFloat() / duration) * usable

            // Inactive track
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(thumbPx, cy - trackH / 2f),
                size = Size(usable, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
            )
            // Active range
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(startX, cy - trackH / 2f),
                size = Size((endX - startX).coerceAtLeast(0f), trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
            )
            // Start thumb
            drawCircle(color = thumbColor, radius = thumbPx, center = Offset(startX, cy))
            drawCircle(color = Color.White, radius = thumbPx * 0.35f, center = Offset(startX, cy))
            // End thumb
            drawCircle(color = thumbColor, radius = thumbPx, center = Offset(endX, cy))
            drawCircle(color = Color.White, radius = thumbPx * 0.35f, center = Offset(endX, cy))
        }
    }
}
