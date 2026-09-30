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

/**
 * Large dual-handle range slider for start/end trim.
 * Touch targets are deliberately huge for phone use.
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

    var draggingStart by remember { mutableStateOf(false) }
    var draggingEnd by remember { mutableStateOf(false) }

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
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            val usable = (width - 2 * thumbPx).coerceAtLeast(1f)
                            val startX = thumbPx + (startMs.toFloat() / duration) * usable
                            val endX = thumbPx + (endMs.toFloat() / duration) * usable
                            val dStart = abs(offset.x - startX)
                            val dEnd = abs(offset.x - endX)
                            // Prefer nearer handle; large hit area
                            val hit = thumbPx * 2.2f
                            when {
                                dStart <= hit && dStart <= dEnd -> {
                                    draggingStart = true
                                    draggingEnd = false
                                }
                                dEnd <= hit -> {
                                    draggingEnd = true
                                    draggingStart = false
                                }
                                offset.x < (startX + endX) / 2f -> {
                                    draggingStart = true
                                    draggingEnd = false
                                }
                                else -> {
                                    draggingEnd = true
                                    draggingStart = false
                                }
                            }
                        },
                        onDragEnd = {
                            draggingStart = false
                            draggingEnd = false
                        },
                        onDragCancel = {
                            draggingStart = false
                            draggingEnd = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat()
                            val usable = (width - 2 * thumbPx).coerceAtLeast(1f)
                            val frac = ((change.position.x - thumbPx) / usable).coerceIn(0f, 1f)
                            val ms = (frac * duration).toLong()
                            val minGap = max(50L, duration / 200) // ~0.05s or 0.5%
                            if (draggingStart) {
                                val newStart = min(ms, endMs - minGap).coerceAtLeast(0L)
                                onRangeChange(newStart, endMs)
                                onScrubMs?.invoke(newStart)
                            } else if (draggingEnd) {
                                val newEnd = max(ms, startMs + minGap).coerceAtMost(duration)
                                onRangeChange(startMs, newEnd)
                                onScrubMs?.invoke(newEnd)
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
